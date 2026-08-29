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
import model.enums.HabitFrequency;
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
import service.exceptions.HabitNotScheduledTodayException;

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
    private final HabitPeriodCalculator habitPeriodCalculator;

    public HabitService(UserService userService, HabitCompletionService habitCompletionService,
                        CoinTransactionService coinTransactionService, AchievementService achievementService,
                        HabitRepository habitRepository, HabitMapper habitMapper, CoinCalculator coinCalculator,
                        HabitPeriodCalculator habitPeriodCalculator) {
        this.userService = userService;
        this.habitCompletionService = habitCompletionService;
        this.coinTransactionService = coinTransactionService;
        this.achievementService = achievementService;
        this.habitRepository = habitRepository;
        this.habitMapper = habitMapper;
        this.coinCalculator = coinCalculator;
        this.habitPeriodCalculator = habitPeriodCalculator;
    }

    @Transactional
    public AddHabitResponse addHabit(Long userId, @NonNull AddHabitRequest request) {
        log.info("About to add habit for user {}", userId);

        UserEntity userEntity = userService.findEntityById(userId);

        validateFrequencyConfig(request.frequency(), request.scheduledDayOfWeek(), request.customIntervalDays());
        if (request.frequency() == HabitFrequency.MULTI_DAY) {
            validateMultiDayConfig(request.scheduledDaysOfWeek());
        }

        int[] coins = coinCalculator.computeBaseCoins(request.difficultyLevel());
        log.debug("Computed coins for {}: reward={}, penalty={}", request.difficultyLevel(), coins[0], coins[1]);

        Habit habit = Habit.builder()
                .userId(userId)
                .title(request.title())
                .description(request.description())
                .frequency(request.frequency())
                .scheduledDayOfWeek(request.scheduledDayOfWeek())
                .scheduledTimeType(request.scheduledTimeType())
                .scheduledHour(request.scheduledHour())
                .customIntervalDays(request.customIntervalDays())
                .scheduledDaysOfWeek(request.scheduledDaysOfWeek())
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

        HabitEntity entity = loadActiveHabit(habitId);
        Map<Long, Set<LocalDate>> completionsMap =
                habitCompletionService.getCompletedDatesForHabits(List.of(habitId), yearMonth);

        return HabitResponse.from(
                habitMapper.toDomain(entity),
                buildMonthCompletions(completionsMap.getOrDefault(habitId, Collections.emptySet()), yearMonth, entity));
    }

    @Transactional(readOnly = true)
    public List<HabitResponse> getUserHabits(Long userId, YearMonth yearMonth) {
        log.info("About to list habits for user {} for {}", userId, yearMonth);

        List<HabitEntity> entities = habitRepository.findAllByUser_IdAndDeletedAtIsNull(userId);
        List<Long> habitIds = entities.stream().map(HabitEntity::getId).toList();
        Map<Long, Set<LocalDate>> completionsMap =
                habitCompletionService.getCompletedDatesForHabits(habitIds, yearMonth);

        return entities.stream()
                .map(entity -> HabitResponse.from(
                        habitMapper.toDomain(entity),
                        buildMonthCompletions(
                                completionsMap.getOrDefault(entity.getId(), Collections.emptySet()),
                                yearMonth,
                                entity)))
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
        if (request.scheduledDayOfWeek() != null) entity.setScheduledDayOfWeek(request.scheduledDayOfWeek());
        if (request.scheduledTimeType() != null) entity.setScheduledTimeType(request.scheduledTimeType());
        if (request.scheduledHour() != null) entity.setScheduledHour(request.scheduledHour());
        if (request.customIntervalDays() != null) entity.setCustomIntervalDays(request.customIntervalDays());
        if (request.scheduledDaysOfWeek() != null) entity.setScheduledDaysOfWeek(request.scheduledDaysOfWeek());

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
        LocalDate today = LocalDate.now();
        LocalDate[] period = habitPeriodCalculator.currentPeriod(entity, today);

        // MULTI_DAY returns null when today is not a scheduled day
        if (period == null) {
            throw new HabitNotScheduledTodayException(habitId);
        }

        LocalDate periodStart = period[0];
        LocalDate periodEnd = period[1];

        boolean alreadyDone = entity.getFrequency() == HabitFrequency.DAILY
                ? habitCompletionService.existsToday(habitId)
                : habitCompletionService.existsForPeriod(habitId, periodStart, periodEnd);

        if (alreadyDone) {
            throw new HabitAlreadyCompletedTodayException(habitId);
        }

        boolean late = habitPeriodCalculator.isLateCompletion(entity, today);
        int oldBestStreak = entity.getBestStreak();
        int newStreak = isStreakContinued(entity, today, periodStart) ?
                entity.getCurrentStreak() + 1 : 1;
        int newBestStreak = Math.max(newStreak, oldBestStreak);

        int baseCoins = coinCalculator.computeHabitCompletionReward(entity.getDifficultyLevel(), newStreak);
        int penalty = late ? (entity.getCoinPenalty() != null ? entity.getCoinPenalty() : 0) : 0;
        int coinsEarned = Math.max(0, baseCoins - penalty);

        log.debug("Habit {} '{}' completed: streak {} -> {}, best={}, late={}, coins={}",
                habitId, entity.getTitle(), entity.getCurrentStreak(), newStreak, newBestStreak, late, coinsEarned);

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

    /**
     * Returns true if the user's last completion falls within the period immediately preceding the current one.
     *
     * <ul>
     *   <li>DAILY — last completed yesterday</li>
     *   <li>WEEKLY — last completed anywhere in the previous ISO week</li>
     *   <li>CUSTOM — last completed within the period ending the day before the current period starts</li>
     * </ul>
     */
    private boolean isStreakContinued(HabitEntity entity, LocalDate today, LocalDate currentPeriodStart) {
        LocalDate last = entity.getLastCompletedDate();
        if (last == null) return false;
        return switch (entity.getFrequency()) {
            case DAILY -> today.minusDays(1).equals(last);
            case WEEKLY -> {
                LocalDate prevWeekStart = currentPeriodStart.minusWeeks(1);
                LocalDate prevWeekEnd = currentPeriodStart.minusDays(1);
                yield !last.isBefore(prevWeekStart) && !last.isAfter(prevWeekEnd);
            }
            case CUSTOM -> {
                LocalDate prevPeriodEnd = currentPeriodStart.minusDays(1);
                LocalDate prevPeriodStart = prevPeriodEnd.minusDays(entity.getCustomIntervalDays() - 1);
                yield !last.isBefore(prevPeriodStart) && !last.isAfter(prevPeriodEnd);
            }
            // MULTI_DAY: the streak continues only if the user completed the slot
            // immediately before the current one (no skipped slots).
            // Walk backwards from yesterday — the first scheduled day we land on
            // is the "previous slot". If last equals that day, streak is alive.
            case MULTI_DAY -> {
                List<Integer> scheduledDays = entity.getScheduledDaysOfWeek();
                if (scheduledDays == null || scheduledDays.isEmpty()) yield false;
                LocalDate prev = today.minusDays(1);
                // Search up to 7 days back (the longest gap between two selected days)
                while (!prev.isBefore(today.minusDays(7))) {
                    if (scheduledDays.contains(prev.getDayOfWeek().getValue())) {
                        // Found the previous slot — streak continues only if last was exactly here
                        yield last.equals(prev);
                    }
                    prev = prev.minusDays(1);
                }
                yield false;
            }
        };
    }

    private void validateFrequencyConfig(HabitFrequency frequency, Integer scheduledDayOfWeek,
                                          Integer customIntervalDays) {
        if (frequency == HabitFrequency.WEEKLY || frequency == HabitFrequency.CUSTOM) {
            if (scheduledDayOfWeek == null || scheduledDayOfWeek < 1 || scheduledDayOfWeek > 7) {
                throw new IllegalArgumentException("scheduledDayOfWeek (1–7) is required for WEEKLY and CUSTOM habits");
            }
        }
        if (frequency == HabitFrequency.CUSTOM) {
            if (customIntervalDays == null || (customIntervalDays != 7 && customIntervalDays != 14 && customIntervalDays != 30)) {
                throw new IllegalArgumentException("customIntervalDays must be 7, 14, or 30");
            }
        }
    }

    private void validateMultiDayConfig(List<Integer> scheduledDaysOfWeek) {
        if (scheduledDaysOfWeek == null || scheduledDaysOfWeek.size() < 2 || scheduledDaysOfWeek.size() > 6) {
            throw new IllegalArgumentException("MULTI_DAY habits require 2–6 scheduled days of week");
        }
        boolean allValid = scheduledDaysOfWeek.stream().allMatch(d -> d >= 1 && d <= 7);
        if (!allValid) {
            throw new IllegalArgumentException("scheduledDaysOfWeek values must be 1 (Mon) through 7 (Sun)");
        }
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
     * Builds a per-period {@link CompletionStatus} list for the given month, driven by the habit's frequency.
     *
     * <p>Periods are determined by {@link HabitPeriodCalculator#periodsForMonth}: one slot per expected completion
     * (one per day for DAILY, one per scheduled-day-of-week for WEEKLY, one per interval window for CUSTOM).
     * The length of the returned list equals the number of radial segments shown on the frontend.
     *
     * <ul>
     *   <li>{@code DONE} — at least one completion date falls within {@code [periodStart, periodEnd]}</li>
     *   <li>{@code MISSED} — period has fully elapsed ({@code periodEnd < today}), habit existed before the
     *   period ended, and no completion recorded</li>
     *   <li>{@code NA} — period hasn't started yet, period is still ongoing, or the habit was created after
     *   the period already ended</li>
     * </ul>
     *
     * <p>Note: for WEEKLY habits the period window starts on Monday but the habit may be created mid-week.
     * We use {@code periodEnd} (not {@code periodStart}) for the creation-date guard so that mid-week creation
     * still allows the current period to be completed and counted.
     */
    private List<CompletionStatus> buildMonthCompletions(
            Set<LocalDate> completedDates, YearMonth yearMonth, HabitEntity entity) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate habitCreatedDate = entity.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
        List<LocalDate[]> periods = habitPeriodCalculator.periodsForMonth(entity, yearMonth);
        log.debug("Building month completions for {}: frequency={}, periods={}, completedDates={}",
                yearMonth, entity.getFrequency(), periods.size(), completedDates);
        List<CompletionStatus> result = new ArrayList<>(periods.size());
        for (LocalDate[] period : periods) {
            LocalDate periodStart = period[0];
            LocalDate periodEnd = period[1];
            // Skip if the period ended before the habit existed, or hasn't started yet (future)
            if (periodEnd.isBefore(habitCreatedDate) || periodStart.isAfter(today)) {
                result.add(CompletionStatus.NA);
            } else {
                boolean done = completedDates.stream()
                        .anyMatch(d -> !d.isBefore(periodStart) && !d.isAfter(periodEnd));
                if (done) {
                    result.add(CompletionStatus.DONE);
                } else if (periodEnd.isBefore(today)) {
                    result.add(CompletionStatus.MISSED);
                } else {
                    result.add(CompletionStatus.NA);
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
