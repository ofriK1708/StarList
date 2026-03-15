package service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import model.domain.Habit;
import model.enums.DifficultyLevel;
import model.enums.ReferenceType;
import model.enums.TransactionType;
import org.jspecify.annotations.NonNull;
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
    private final HabitRepository habitRepository;
    private final HabitMapper habitMapper;
    private final CoinCalculator coinCalculator;

    public HabitService(UserService userService, HabitCompletionService habitCompletionService,
                        CoinTransactionService coinTransactionService, HabitRepository habitRepository,
                        HabitMapper habitMapper, CoinCalculator coinCalculator) {
        this.userService = userService;
        this.habitCompletionService = habitCompletionService;
        this.coinTransactionService = coinTransactionService;
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

        return AddHabitResponse.from(
                habitMapper.toDomain(
                        habitRepository.save(
                                habitMapper.fromDomain(habit, userEntity))));
    }

    @Transactional(readOnly = true)
    public HabitResponse getHabit(Long habitId) {
        log.info("About to get habit {}", habitId);

        return HabitResponse.from(habitMapper.toDomain(loadActiveHabit(habitId)));
    }

    @Transactional(readOnly = true)
    public List<HabitResponse> getUserHabits(Long userId) {
        log.info("About to list habits for user {}", userId);

        return habitRepository.findAllByUser_IdAndDeletedAtIsNull(userId)
                .stream()
                .map(habitMapper::toDomain)
                .map(HabitResponse::from)
                .toList();
    }

    @Transactional
    public HabitResponse updateHabit(Long habitId, @NonNull UpdateHabitRequest request) {
        log.info("About to update habit {}", habitId);

        HabitEntity entity = loadActiveHabit(habitId);

        boolean difficultyChanged = !entity.getDifficultyLevel().equals(request.getDifficultyLevel());
        if (difficultyChanged) {
            log.debug("Difficulty changed for habit {}: {} -> {}", habitId, entity.getDifficultyLevel(), request.getDifficultyLevel());
        }

        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setFrequency(request.getFrequency());
        entity.setDifficultyLevel(request.getDifficultyLevel());

        if (difficultyChanged) {
            int[] coins = coinCalculator.computeBaseCoins(request.getDifficultyLevel());
            entity.setCoinReward(coins[0]);
            entity.setCoinPenalty(coins[1]);
            log.debug("Recalculated coins for habit {}: reward={}, penalty={}", habitId, coins[0], coins[1]);
        }

        return HabitResponse.from(
                habitMapper.toDomain(
                        habitRepository.save(entity)));
    }

    @Transactional
    public MarkHabitDoneResponse completeHabit(Long habitId) {
        log.info("About to complete habit {}", habitId);

        HabitEntity entity = loadActiveHabit(habitId);

        if (habitCompletionService.existsToday(habitId)) {
            throw new HabitAlreadyCompletedTodayException(habitId);
        }

        LocalDate today = LocalDate.now();

        int newStreak = today.minusDays(1).equals(entity.getLastCompletedDate()) ?
                entity.getCurrentStreak() + 1 : 1;
        int newBestStreak = Math.max(newStreak, entity.getBestStreak());

        entity.setCurrentStreak(newStreak);
        entity.setBestStreak(newBestStreak);
        entity.setTotalCompletions(entity.getTotalCompletions() + 1);
        entity.setLastCompletedDate(today);
        habitRepository.save(entity);

        int coinsEarned = coinCalculator.computeHabitCompletionReward(entity.getDifficultyLevel(), newStreak);
        log.debug("Habit {} '{}' completed: streak {} -> {}, best={}, coins={}",
                habitId, entity.getTitle(), entity.getCurrentStreak(), newStreak, newBestStreak, coinsEarned);
        UserEntity user = entity.getUser();

        habitCompletionService.record(entity, user, today, coinsEarned, newStreak);
        coinTransactionService.record(user, coinsEarned, TransactionType.HABIT_COMPLETION,
                ReferenceType.HABIT, habitId, "Completed habit: " + entity.getTitle());
        userService.addCoins(user, coinsEarned);

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
     * Loads a habit entity by ID, throwing {@link HabitNotFoundException} if absent or soft-deleted.
     */
    private HabitEntity loadActiveHabit(Long habitId) {
        return habitRepository.findById(habitId)
                .filter(e -> e.getDeletedAt() == null)
                .orElseThrow(() -> new HabitNotFoundException(habitId));
    }

}
