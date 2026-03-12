package service;

import java.time.Instant;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import model.domain.Habit;
import model.enums.DifficultyLevel;
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
import service.dto.UpdateHabitRequest;
import service.exceptions.HabitNotFoundException;

@Slf4j
@Service
public class HabitService {

    private final UserService userService;
    private final HabitRepository habitRepository;
    private final HabitMapper habitMapper;
    private final CoinCalculator coinCalculator;

    public HabitService(UserService userService, HabitRepository habitRepository, HabitMapper habitMapper, CoinCalculator coinCalculator) {
        this.userService = userService;
        this.habitRepository = habitRepository;
        this.habitMapper = habitMapper;
        this.coinCalculator = coinCalculator;
    }

    @Transactional
    public AddHabitResponse addHabit(Long userId, @NonNull AddHabitRequest request) {
        log.info("About to add habit for user {}", userId);

        UserEntity userEntity = userService.findEntityById(userId);

        int[] coins = coinCalculator.computeBaseCoins(request.difficultyLevel());

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

        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setFrequency(request.getFrequency());
        entity.setDifficultyLevel(request.getDifficultyLevel());

        if (difficultyChanged) {
            int[] coins = coinCalculator.computeBaseCoins(request.getDifficultyLevel());
            entity.setCoinReward(coins[0]);
            entity.setCoinPenalty(coins[1]);
        }

        return HabitResponse.from(
                habitMapper.toDomain(
                        habitRepository.save(entity)));
    }

    @Transactional
    public void deleteHabit(Long habitId) {
        log.info("About to delete habit {}", habitId);

        HabitEntity entity = loadActiveHabit(habitId);
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
