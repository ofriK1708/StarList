package service;

import model.enums.DifficultyLevel;
import model.enums.ReferenceType;
import model.enums.TaskStatus;
import model.enums.TransactionType;
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
import service.exceptions.TaskAlreadyCompletedException;
import service.exceptions.TaskNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock UserService userService;
    @Mock CoinTransactionService coinTransactionService;
    @Mock TaskMapper taskMapper;
    @Mock CoinCalculator coinCalculator;

    @InjectMocks TaskService taskService;

    // ── completeTask happy path ───────────────────────────────────────────────

    @Test
    void completeTask_pendingTask_setsCompletedAndAwardsCoins() {
        UserEntity user = UserEntity.builder().id(1L).totalCoins(100).lifetimeCoinsEarned(0).build();
        TaskEntity task = TaskEntity.builder()
                .id(42L)
                .title("Learn testing")
                .status(TaskStatus.PENDING)
                .difficultyLevel(DifficultyLevel.HARD)
                .coinReward(50)
                .user(user)
                .build();

        when(taskRepository.findById(42L)).thenReturn(Optional.of(task));

        MarkTaskDoneResponse response = taskService.completeTask(42L);

        // response fields
        assertThat(response.taskId()).isEqualTo(42L);
        assertThat(response.coinsEarned()).isEqualTo(50);
        assertThat(response.newTotalCoins()).isEqualTo(100); // user.totalCoins as-is (addCoins is mocked)

        // task entity was mutated correctly
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isNotNull();

        // side effects were triggered
        verify(taskRepository).save(task);
        verify(coinTransactionService).record(
                eq(user), eq(50), eq(TransactionType.TASK_COMPLETION),
                eq(ReferenceType.TASK), eq(42L), any(String.class));
        verify(userService).addCoins(user, 50);
    }

    // ── completeTask sad paths ────────────────────────────────────────────────

    @Test
    void completeTask_alreadyCompleted_throwsTaskAlreadyCompletedException() {
        TaskEntity task = TaskEntity.builder()
                .id(42L)
                .status(TaskStatus.COMPLETED)
                .build();

        when(taskRepository.findById(42L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.completeTask(42L))
                .isInstanceOf(TaskAlreadyCompletedException.class);

        verifyNoInteractions(coinTransactionService, userService);
    }

    @Test
    void completeTask_taskNotFound_throwsTaskNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.completeTask(99L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void completeTask_softDeletedTask_throwsTaskNotFoundException() {
        // soft-deleted tasks (deletedAt != null) are treated the same as not found
        TaskEntity deleted = TaskEntity.builder()
                .id(42L)
                .status(TaskStatus.DELETED)
                .deletedAt(java.time.Instant.now())
                .build();

        when(taskRepository.findById(42L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> taskService.completeTask(42L))
                .isInstanceOf(TaskNotFoundException.class);
    }
}
