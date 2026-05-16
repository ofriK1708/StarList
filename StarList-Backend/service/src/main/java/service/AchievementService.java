package service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import model.enums.AchievementType;
import model.enums.HabitFrequency;
import model.enums.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.api.AchievementRepository;
import repository.api.HabitCompletionRepository;
import repository.api.HabitRepository;
import repository.api.TaskRepository;
import repository.api.UserRepository;
import repository.entity.AchievementEntity;
import repository.entity.HabitEntity;
import repository.entity.TaskEntity;
import service.dto.AchievementResponse;

@Slf4j
@Service
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final HabitRepository habitRepository;
    private final HabitCompletionRepository habitCompletionRepository;

    public AchievementService(AchievementRepository achievementRepository,
                              UserRepository userRepository,
                              TaskRepository taskRepository,
                              HabitRepository habitRepository,
                              HabitCompletionRepository habitCompletionRepository) {
        this.achievementRepository = achievementRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.habitRepository = habitRepository;
        this.habitCompletionRepository = habitCompletionRepository;
    }

    /** Returns all achievement types for a user, each marked as unlocked or locked. */
    @Transactional(readOnly = true)
    public List<AchievementResponse> getUserAchievements(Long userId) {
        Map<AchievementType, Instant> unlockedMap = achievementRepository.findAllByUser_Id(userId).stream()
                .collect(Collectors.toMap(AchievementEntity::getType, AchievementEntity::getUnlockedAt));

        return Arrays.stream(AchievementType.values())
                .map(type -> AchievementResponse.builder()
                        .id(type.name())
                        .name(type.getDisplayName())
                        .icon(type.getIcon())
                        .description(type.getDescription())
                        .unlocked(unlockedMap.containsKey(type))
                        .unlockedAt(unlockedMap.get(type))
                        .build())
                .toList();
    }

    /** Called when a new user is created — unlocks STARTER immediately. */
    @Transactional
    public void onUserCreated(Long userId) {
        tryUnlock(userId, AchievementType.STARTER);
    }

    /** Called when a task or habit is created — checks ARCHITECT (10 total created). */
    @Transactional
    public void onTaskOrHabitCreated(Long userId) {
        long total = taskRepository.countByUser_IdAndDeletedAtIsNull(userId)
                + habitRepository.countByUser_IdAndDeletedAtIsNull(userId);
        if (total >= 10) {
            tryUnlock(userId, AchievementType.ARCHITECT);
        }
    }

    /**
     * Called after a habit completion is saved.
     *
     * @param newStreak     the streak after this completion
     * @param oldBestStreak the best streak before this completion (pre-mutation value)
     * @param completedAt   when the completion occurred (used for time-of-day badges)
     */
    @Transactional
    public void onHabitCompleted(Long userId, int newStreak, int oldBestStreak, Instant completedAt) {
        if (newStreak >= 7)   tryUnlock(userId, AchievementType.WEEK_WARRIOR);
        if (newStreak >= 30)  tryUnlock(userId, AchievementType.UNSTOPPABLE);
        if (newStreak >= 100) tryUnlock(userId, AchievementType.STREAK_CENTURION);
        // PHOENIX: restarted a streak after having built one before
        if (newStreak == 1 && oldBestStreak > 0) tryUnlock(userId, AchievementType.PHOENIX);
        checkTimeBadges(userId, completedAt);
        checkCleanSweep(userId);
    }

    /** Called after a task completion is saved. */
    @Transactional
    public void onTaskCompleted(Long userId, Instant completedAt) {
        long completedCount = taskRepository.countByUser_IdAndStatusAndDeletedAtIsNull(
                userId, TaskStatus.COMPLETED);
        if (completedCount >= 50) tryUnlock(userId, AchievementType.TASK_MASTER);
        checkTimeBadges(userId, completedAt);
        checkCleanSweep(userId);
    }

    private void checkTimeBadges(Long userId, Instant completedAt) {
        LocalTime time = completedAt.atZone(ZoneOffset.UTC).toLocalTime();
        if (time.isBefore(LocalTime.of(8, 0)))   tryUnlock(userId, AchievementType.EARLY_BIRD);
        if (!time.isBefore(LocalTime.of(22, 0))) tryUnlock(userId, AchievementType.NIGHT_OWL);
    }

    private void checkCleanSweep(Long userId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfTomorrow = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<TaskEntity> tasksDueToday = taskRepository.findDueTodayByUserId(userId, startOfDay, startOfTomorrow);
        List<HabitEntity> dailyHabits = habitRepository.findAllByUser_IdAndFrequencyAndDeletedAtIsNull(
                userId, HabitFrequency.DAILY);

        if (tasksDueToday.isEmpty() && dailyHabits.isEmpty()) return;

        boolean allTasksDone = tasksDueToday.stream().allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);
        boolean allHabitsDone = dailyHabits.stream()
                .allMatch(h -> habitCompletionRepository.existsByHabit_IdAndCompletedDate(h.getId(), today));

        if (allTasksDone && allHabitsDone) {
            tryUnlock(userId, AchievementType.CLEAN_SWEEP);
        }
    }

    private void tryUnlock(Long userId, AchievementType type) {
        if (!achievementRepository.existsByUser_IdAndType(userId, type)) {
            achievementRepository.save(AchievementEntity.builder()
                    .user(userRepository.getReferenceById(userId))
                    .type(type)
                    .build());
            log.info("Unlocked achievement {} for user {}", type, userId);
        }
    }
}
