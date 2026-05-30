package service;

import model.domain.Task;
import model.enums.DifficultyLevel;
import model.enums.ReferenceType;
import model.enums.TaskStatus;
import model.enums.TransactionType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.api.TaskRepository;
import repository.entity.TaskEntity;
import repository.entity.UserEntity;
import repository.mapper.TaskMapper;
import service.dto.MarkTaskDoneResponse;
import service.dto.UpdateTaskRequest;
import service.exceptions.TaskAlreadyCompletedException;
import service.exceptions.TaskNotFoundException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    TaskRepository taskRepository;
    @Mock
    UserService userService;
    @Mock
    CoinTransactionService coinTransactionService;
    @Mock
    AchievementService achievementService;
    @Mock
    TaskMapper taskMapper;
    @Mock
    CoinCalculator coinCalculator;

    @InjectMocks
    TaskService taskService;
    long testTaskID = 42L;

    // ── completeTask happy path ───────────────────────────────────────────────

    @Test
    void completeTask_pendingTask_setsCompletedAndAwardsCoins() {

        UserEntity user = UserEntity.builder().id(1L).totalCoins(100).lifetimeCoinsEarned(0).build();
        TaskEntity task = TaskEntity.builder()
                .id(testTaskID)
                .title("Learn testing")
                .status(TaskStatus.PENDING)
                .difficultyLevel(DifficultyLevel.HARD)
                .coinReward(50)
                .user(user)
                .build();

        when(taskRepository.findById(testTaskID)).thenReturn(Optional.of(task));

        MarkTaskDoneResponse response = taskService.completeTask(testTaskID);

        // response fields
        assertThat(response.taskId()).isEqualTo(testTaskID);
        assertThat(response.coinsEarned()).isEqualTo(50);
        assertThat(response.newTotalCoins()).isEqualTo(100);// (addCoins is mocked, we late check it in verify)

        // task entity was mutated correctly
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isNotNull();

        // side effects were triggered
        verify(taskRepository).save(task);
        verify(coinTransactionService).record(
                eq(user), eq(50), eq(TransactionType.TASK_COMPLETION),
                eq(ReferenceType.TASK), eq(testTaskID), any(String.class));
        verify(userService).addCoins(user, 50); // check if the user received his rewards
    }

    // ── completeTask sad paths ────────────────────────────────────────────────

    @Test
    void completeTask_alreadyCompleted_throwsTaskAlreadyCompletedException() {
        TaskEntity task = TaskEntity.builder()
                .id(testTaskID)
                .status(TaskStatus.COMPLETED)
                .build();

        when(taskRepository.findById(testTaskID)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.completeTask(testTaskID))
                .isInstanceOf(TaskAlreadyCompletedException.class);

        verify(taskRepository).findById(testTaskID);
        verifyNoMoreInteractions(taskRepository);
        verifyNoInteractions(coinTransactionService, userService);
    }

    @Test
    void completeTask_taskNotFound_throwsTaskNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.completeTask(99L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void completeTask_concurrentDuplicate_optimisticLockCollisionTranslatedToTaskAlreadyCompletedException() {
        // Simulates the race: both requests read the same version, first commits, second hits optimistic lock failure
        UserEntity user = UserEntity.builder().id(1L).totalCoins(100).lifetimeCoinsEarned(0).build();
        TaskEntity task = TaskEntity.builder()
                .id(testTaskID).title("Learn testing")
                .status(TaskStatus.PENDING).difficultyLevel(DifficultyLevel.HARD)
                .coinReward(50).user(user).build();

        when(taskRepository.findById(testTaskID)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenThrow(new ObjectOptimisticLockingFailureException(TaskEntity.class,
                testTaskID));

        assertThatThrownBy(() -> taskService.completeTask(testTaskID))
                .isInstanceOf(TaskAlreadyCompletedException.class);

        verifyNoInteractions(coinTransactionService, userService);
    }

    @Test
    void completeTask_softDeletedTask_throwsTaskNotFoundException() {
        // soft-deleted tasks (deletedAt != null) are treated the same as not found
        TaskEntity deleted = TaskEntity.builder()
                .id(testTaskID)
                .status(TaskStatus.DELETED)
                .deletedAt(java.time.Instant.now())
                .build();

        when(taskRepository.findById(testTaskID)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> taskService.completeTask(testTaskID))
                .isInstanceOf(TaskNotFoundException.class);
    }

    // ── updateTask ───────────────────────────────────────────────────────────

    @Test
    void updateTask_omittedFields_keepCurrentValues() {
        TaskEntity task = TaskEntity.builder()
                .id(testTaskID)
                .title("Old Title")
                .description("Old Description")
                .difficultyLevel(DifficultyLevel.EASY)
                .durationMinutes(30)
                .dueDate(Instant.parse("2099-01-01T00:00:00Z"))
                .build();

        // only title is provided; all other fields are null → should keep current values
        UpdateTaskRequest request = UpdateTaskRequest.builder().title("New Title").build();

        when(taskRepository.findById(testTaskID)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toDomain(task)).thenReturn(Task.builder().id(testTaskID).build());

        taskService.updateTask(testTaskID, request);

        assertThat(task.getTitle()).isEqualTo("New Title");
        assertThat(task.getDescription()).isEqualTo("Old Description");
        assertThat(task.getDurationMinutes()).isEqualTo(30);
        assertThat(task.getDueDate()).isEqualTo(Instant.parse("2099-01-01T00:00:00Z"));
        assertThat(task.getDifficultyLevel()).isEqualTo(DifficultyLevel.EASY);
    }

    @Test
    void updateTask_setDescriptionToEmpty_entityGetsEmptyString() {
        TaskEntity task = TaskEntity.builder()
                .id(testTaskID)
                .title("Title")
                .description("Old description")
                .difficultyLevel(DifficultyLevel.EASY)
                .build();

        UpdateTaskRequest request = UpdateTaskRequest.builder().description("").build();

        when(taskRepository.findById(testTaskID)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toDomain(task)).thenReturn(Task.builder().id(testTaskID).build());

        taskService.updateTask(testTaskID, request);

        assertThat(task.getDescription()).isEmpty();
    }

    @Test
    void updateTask_difficultyChange_recalculatesCoins() {
        TaskEntity task = TaskEntity.builder()
                .id(testTaskID)
                .title("Title")
                .difficultyLevel(DifficultyLevel.EASY)
                .coinReward(10)
                .coinPenalty(5)
                .build();

        UpdateTaskRequest request = UpdateTaskRequest.builder().difficultyLevel(DifficultyLevel.HARD).build();

        when(taskRepository.findById(testTaskID)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toDomain(task)).thenReturn(Task.builder().id(testTaskID).build());
        when(coinCalculator.computeBaseCoins(DifficultyLevel.HARD)).thenReturn(new int[]{50, 25});

        taskService.updateTask(testTaskID, request);

        verify(coinCalculator).computeBaseCoins(DifficultyLevel.HARD);
        assertThat(task.getDifficultyLevel()).isEqualTo(DifficultyLevel.HARD);
        assertThat(task.getCoinReward()).isEqualTo(50);
        assertThat(task.getCoinPenalty()).isEqualTo(25);
    }

    // ── clearDueDate / clearDurationMinutes ──────────────────────────────────

    @Test
    void clearDueDate_setsNullOnEntity() {
        TaskEntity task = TaskEntity.builder()
                .id(testTaskID)
                .title("Title")
                .difficultyLevel(DifficultyLevel.EASY)
                .dueDate(Instant.parse("2099-01-01T00:00:00Z"))
                .build();

        when(taskRepository.findById(testTaskID)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toDomain(task)).thenReturn(Task.builder().id(testTaskID).build());

        taskService.clearDueDate(testTaskID);

        assertThat(task.getDueDate()).isNull();
        verify(taskRepository).save(task);
    }

    @Test
    void clearDurationMinutes_setsNullOnEntity() {
        TaskEntity task = TaskEntity.builder()
                .id(testTaskID)
                .title("Title")
                .difficultyLevel(DifficultyLevel.EASY)
                .durationMinutes(45)
                .build();

        when(taskRepository.findById(testTaskID)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toDomain(task)).thenReturn(Task.builder().id(testTaskID).build());

        taskService.clearDurationMinutes(testTaskID);

        assertThat(task.getDurationMinutes()).isNull();
        verify(taskRepository).save(task);
    }

    @Test
    void clearDueDate_unknownTask_throwsTaskNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.clearDueDate(99L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void clearDurationMinutes_unknownTask_throwsTaskNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.clearDurationMinutes(99L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void clearDueDate_softDeletedTask_throwsTaskNotFoundException() {
        TaskEntity deleted = TaskEntity.builder()
                .id(testTaskID)
                .deletedAt(Instant.now())
                .build();

        when(taskRepository.findById(testTaskID)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> taskService.clearDueDate(testTaskID))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void clearDurationMinutes_softDeletedTask_throwsTaskNotFoundException() {
        TaskEntity deleted = TaskEntity.builder()
                .id(testTaskID)
                .deletedAt(Instant.now())
                .build();

        when(taskRepository.findById(testTaskID)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> taskService.clearDurationMinutes(testTaskID))
                .isInstanceOf(TaskNotFoundException.class);
    }
}
