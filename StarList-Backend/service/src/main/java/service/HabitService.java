package service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import model.domain.Habit;
import model.enums.CompletionStatus;
import model.enums.DifficultyLevel;
import model.enums.ReferenceType;
import model.enums.TransactionType;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.api.HabitRepository;
import repository.entity.HabitEntity;
import repository.entity.UserEntity;
import repository.mapper.HabitMapper;
import service.dto.AddHabitRequest;
import service.dto.AddHabitResponse;
import service.dto.HabitResponse;
import service.dto.MarkHabitDoneResponse;
import service.dto.UpdateHabitRequest;
import service.exceptions.HabitAlreadyCompletedTodayException;
import service.exceptions.HabitNotFoundException;

@Slf4j
@Service
public class HabitService {

    private final UserService userService;
    private final HabitCompletionService habitCompletionService;
    private final CoinTransactionService coinTransactionService;
    private final AchievementService achievementService;
    private final HabitRepository habitRepository;
    private final HabitMapper habitMapper;
    private final CoinCalculator coinCalculator;

    public HabitService(UserService userService, HabitCompletionService habitCompletionService,
                        CoinTransactionService coinTransactionService, AchievementService achievementService,
                        HabitRepository habitRepository, HabitMapper habitMapper, CoinCalculator coinCalculator) {
        this.userService = userService;
        this.habitCompletionService = habitCompletionService;
        this.coinTransactionService = coinTransactionService;
        this.achievementService = achievementService;
        this.habitRepository = habitRepository;
        this.habitMapper = habitMapper;
        this.coinCalculator = coinCalculator;
    }

    @Transactional
    public AddHabitResponse addHabit(Long userId, @NonNull AddHabitRequest request) {
        log.info("About to add habit for user {}", userId);

        UserEntity userEntity = userService.findEntityById(userId);

        int[] coins = coinCalculator.computeBaseCoins(request.difficultyLevel());
        log.debug("Computed coins for {}: reward={}, penalty={}", request.difficultyLevel(), coins[0], coins[1]);

        Habit habit = Habit.builder()
                .userId(userId)
                .title(request.title())
                .description(request.description())
                .frequency(request.frequency())
                .difficultyLevel(request.difficultyLevel())
                .coinReward(coins[0])
                .coinPenalty(coins[1])
                .isActive(true)
                .build();

        AddHabitResponse response = AddHabitResponse.from(
                habitMapper.toDomain(
                        habitRepository.save(
                                habitMapper.fromDomain(habit, userEntity))));
        achievementService.onTaskOrHabitCreated(userId);
        return response;
    }

    @Transactional(readOnly = true)
    public HabitResponse getHabit(Long habitId, YearMonth yearMonth) {
        log.info("About to get habit {} for {}", habitId, yearMonth);

        Habit habit = habitMapper.toDomain(loadActiveHabit(habitId));
        Map<Long, Set<LocalDate>> completionsMap =
                habitCompletionService.getCompletedDatesForHabits(List.of(habitId), yearMonth);
        LocalDate habitCreatedDate = habit.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();

        return HabitResponse.from(habit,
                buildMonthCompletions(
                        completionsMap.getOrDefault(habitId, Collections.emptySet()),
                        yearMonth,
                        habitCreatedDate));
    }

    @Transactional(readOnly = true)
    public List<HabitResponse> getUserHabits(Long userId, YearMonth yearMonth) {
        log.info("About to list habits for user {} for {}", userId, yearMonth);

        List<HabitEntity> entities = habitRepository.findAllByUser_IdAndDeletedAtIsNull(userId);
        List<Long> habitIds = entities.stream().map(HabitEntity::getId).toList();
        Map<Long, Set<LocalDate>> completionsMap =
                habitCompletionService.getCompletedDatesForHabits(habitIds, yearMonth);

        return entities.stream()
                .map(habitMapper::toDomain)
                .map(habit -> HabitResponse
                        .from(habit,
                                buildMonthCompletions(
                                        completionsMap.getOrDefault(habit.getId(), Collections.emptySet()),
                                        yearMonth,
                                        habit.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate())))
                .toList();
    }

    @Transactional
    public HabitResponse updateHabit(Long habitId, @NonNull UpdateHabitRequest request) {
        log.info("About to update habit {}", habitId);

        HabitEntity entity = loadActiveHabit(habitId);

        boolean difficultyChanged = request.difficultyLevel() != null
                && !entity.getDifficultyLevel().equals(request.difficultyLevel());
        if (difficultyChanged) {
            log.debug("Difficulty changed for habit {}: {} -> {}", habitId, entity.getDifficultyLevel(),
                    request.difficultyLevel());
        }

        if (request.title() != null) entity.setTitle(request.title());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.frequency() != null) entity.setFrequency(request.frequency());
        if (request.difficultyLevel() != null) entity.setDifficultyLevel(request.difficultyLevel());

        if (difficultyChanged) {
            int[] coins = coinCalculator.computeBaseCoins(request.difficultyLevel());
            entity.setCoinReward(coins[0]);
            entity.setCoinPenalty(coins[1]);
            log.debug("Recalculated coins for habit {}: reward={}, penalty={}", habitId, coins[0], coins[1]);
        }

        return HabitResponse.from(
                habitMapper.toDomain(
                        habitRepository.save(entity)));
    }

    /**
     * Marks a habit as completed for today, updating streak and awarding coins.
     *
     * <p>The habit completion record is inserted first — before any habit field mutations —
     * so that a concurrent duplicate request fails fast on the DB unique constraint
     * ({@code habit_id + completed_date}) and the whole transaction rolls back cleanly.
     * The {@code existsToday} pre-check handles the normal (non-racing) duplicate case with a clear error message;
     * the catch on {@link org.springframework.dao.DataIntegrityViolationException} is the safety net for the race.
     *
     * @throws HabitAlreadyCompletedTodayException if the habit was already completed today
     * @throws HabitNotFoundException              if the habit does not exist or is deleted
     */
    @Transactional
    public MarkHabitDoneResponse completeHabit(Long habitId) {
        log.info("About to complete habit {}", habitId);

        HabitEntity entity = loadActiveHabit(habitId);

        if (habitCompletionService.existsToday(habitId)) {
            throw new HabitAlreadyCompletedTodayException(habitId);
        }

        LocalDate today = LocalDate.now();

        int oldBestStreak = entity.getBestStreak();
        int newStreak = today.minusDays(1).equals(entity.getLastCompletedDate()) ?
                entity.getCurrentStreak() + 1 : 1;
        int newBestStreak = Math.max(newStreak, oldBestStreak);

        int coinsEarned = coinCalculator.computeHabitCompletionReward(entity.getDifficultyLevel(), newStreak);
        log.debug("Habit {} '{}' completed: streak {} -> {}, best={}, coins={}",
                habitId, entity.getTitle(), entity.getCurrentStreak(), newStreak, newBestStreak, coinsEarned);
        UserEntity user = entity.getUser();

        try {
            habitCompletionService.record(entity, user, today, coinsEarned, newStreak);
        } catch (DataIntegrityViolationException e) {
            throw new HabitAlreadyCompletedTodayException(habitId);
        }

        entity.setCurrentStreak(newStreak);
        entity.setBestStreak(newBestStreak);
        entity.setTotalCompletions(entity.getTotalCompletions() + 1);
        entity.setLastCompletedDate(today);
        habitRepository.save(entity);
        coinTransactionService.record(user, coinsEarned, TransactionType.HABIT_COMPLETION,
                ReferenceType.HABIT, habitId, "Completed habit: " + entity.getTitle());
        userService.addCoins(user, coinsEarned);
        achievementService.onHabitCompleted(user.getId(), newStreak, oldBestStreak, Instant.now());

        return MarkHabitDoneResponse.builder()
                .habitId(habitId)
                .coinsEarned(coinsEarned)
                .newTotalCoins(user.getTotalCoins())
                .currentStreak(newStreak)
                .bestStreak(newBestStreak)
                .build();
    }

    @Transactional
    public void deleteHabit(Long habitId) {
        log.info("About to delete habit {}", habitId);

        HabitEntity entity = loadActiveHabit(habitId);
        log.debug("Soft-deleting habit '{}' (id={})", entity.getTitle(), habitId);
        entity.setDeletedAt(Instant.now());
        habitRepository.save(entity);
    }

    /**
     * Builds a per-day {@link CompletionStatus} array for the given month.
     *
     * <ul>
     *   <li>{@code DONE} — day is in the past or today, on/after {@code habitCreatedDate}, and appears in {@code
     *   completedDates}</li>
     *   <li>{@code MISSED} — day is in the past, on/after {@code habitCreatedDate}, and not in {@code completedDates
     *   }</li>
     *   <li>{@code NA} — day is today and not marked yet, OR in the future, OR before the habit was created</li>
     * </ul>
     */
    private List<CompletionStatus> buildMonthCompletions(
            Set<LocalDate> completedDates, YearMonth yearMonth, LocalDate habitCreatedDate) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        log.debug("Building month completions for {}: habitCreatedDate={}, today={}, completedDates={}",
                yearMonth, habitCreatedDate, today, completedDates);
        List<CompletionStatus> result = new ArrayList<>(yearMonth.lengthOfMonth());
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            if (date.isAfter(today) || date.isBefore(habitCreatedDate)) {
                result.add(CompletionStatus.NA);
                log.debug("Date {} is {}, marking as NA", date,
                        date.isAfter(today) ? "in the future" : "before habit creation");
            } else if (completedDates.contains(date)) {
                result.add(CompletionStatus.DONE);
                log.debug("Date {} is in completedDates, marking as DONE", date);
            } else {
                if (date.isEqual(today)) {
                    result.add(CompletionStatus.NA);
                    log.debug("Date {} is today, not in completedDates yet, marking as NA", date);
                } else {
                    result.add(CompletionStatus.MISSED);
                    log.debug("Date {} is not in completedDates, marking as MISSED", date);
                }
            }
        }
        return result;
    }

    /**
     * Loads a habit entity by ID, throwing {@link HabitNotFoundException} if absent or soft-deleted.
     */
    private HabitEntity loadActiveHabit(Long habitId) {
        return habitRepository.findById(habitId)
                .filter(e -> e.getDeletedAt() == null)
                .orElseThrow(() -> new HabitNotFoundException(habitId));
    }

}
