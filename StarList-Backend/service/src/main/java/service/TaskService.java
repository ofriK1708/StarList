package service;

import java.time.Instant;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import model.domain.Task;
import model.enums.TaskStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.api.TaskRepository;
import repository.entity.TaskEntity;
import repository.entity.UserEntity;
import repository.mapper.TaskMapper;
import model.enums.ReferenceType;
import model.enums.TransactionType;
import service.dto.AddTaskRequest;
import service.dto.AddTaskResponse;
import service.dto.MarkTaskDoneResponse;
import service.dto.TaskResponse;
import service.dto.UpdateTaskRequest;
import service.exceptions.TaskAlreadyCompletedException;
import service.exceptions.TaskNotFoundException;

@Slf4j
@Service
public class TaskService {

    private final UserService userService;
    private final CoinTransactionService coinTransactionService;
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final CoinCalculator coinCalculator;

    public TaskService(UserService userService, CoinTransactionService coinTransactionService,
                       TaskRepository taskRepository, TaskMapper taskMapper, CoinCalculator coinCalculator) {
        this.userService = userService;
        this.coinTransactionService = coinTransactionService;
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.coinCalculator = coinCalculator;
    }

    @Transactional
    public AddTaskResponse addTask(Long userId, @NonNull AddTaskRequest request) {
        log.info("About to add task for user {}", userId);

        UserEntity userEntity = userService.findEntityById(userId);

        int[] coins = coinCalculator.computeBaseCoins(request.difficultyLevel());
        log.debug("Computed coins for {}: reward={}, penalty={}", request.difficultyLevel(), coins[0], coins[1]);

        Task task = Task.builder()
                .userId(userId)
                .title(request.title())
                .description(request.description())
                .difficultyLevel(request.difficultyLevel())
                .durationMinutes(request.durationMinutes())
                .dueDate(request.dueDate())
                .coinReward(coins[0])
                .coinPenalty(coins[1])
                .createdByAi(false)
                .build();

        return AddTaskResponse.from(
                taskMapper.toDomain(
                        taskRepository.save(
                                taskMapper.fromDomain(task, userEntity))));
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId) {
        log.info("About to get task {}", taskId);

        TaskEntity entity = loadActiveTask(taskId);

        return TaskResponse.from(taskMapper.toDomain(entity));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getUserTasks(Long userId) {
        log.info("About to list tasks for user {}", userId);

        return taskRepository.findAllByUser_IdAndDeletedAtIsNull(userId)
                .stream()
                .map(taskMapper::toDomain)
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, @NonNull UpdateTaskRequest request) {
        log.info("About to update task {}", taskId);
        TaskEntity entity = loadActiveTask(taskId);

        boolean difficultyChanged = !entity.getDifficultyLevel().equals(request.difficultyLevel());
        if (difficultyChanged) {
            log.debug("Difficulty changed for task {}: {} -> {}", taskId, entity.getDifficultyLevel(), request.difficultyLevel());
        }

        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setDifficultyLevel(request.difficultyLevel());
        entity.setDurationMinutes(request.durationMinutes());
        entity.setDueDate(request.dueDate());

        if (difficultyChanged) {
            int[] coins = coinCalculator.computeBaseCoins(request.difficultyLevel());
            entity.setCoinReward(coins[0]);
            entity.setCoinPenalty(coins[1]);
            log.debug("Recalculated coins for task {}: reward={}, penalty={}", taskId, coins[0], coins[1]);
        }

        return TaskResponse.from(
                taskMapper.toDomain(
                        taskRepository.save(entity)));
    }

    @Transactional
    public MarkTaskDoneResponse completeTask(Long taskId) {
        log.info("About to complete task {}", taskId);

        TaskEntity entity = concurrentSafeLoadActiveTask(taskId);

        if (entity.getStatus() == TaskStatus.COMPLETED) {
            throw new TaskAlreadyCompletedException(taskId);
        }

        entity.setStatus(TaskStatus.COMPLETED);
        entity.setCompletedAt(Instant.now());
        taskRepository.save(entity);

        UserEntity user = entity.getUser();
        int coinsEarned = entity.getCoinReward();
        log.debug("Task {} '{}' completed: earning {} coins (user total before: {})",
                taskId, entity.getTitle(), coinsEarned, user.getTotalCoins());

        coinTransactionService.record(user, coinsEarned, TransactionType.TASK_COMPLETION,
                ReferenceType.TASK, taskId, "Completed task: " + entity.getTitle());
        userService.addCoins(user, coinsEarned);

        return MarkTaskDoneResponse.builder()
                .taskId(taskId)
                .coinsEarned(coinsEarned)
                .newTotalCoins(user.getTotalCoins())
                .build();
    }

    @Transactional
    public void deleteTask(Long taskId) {
        log.info("About to delete task {}", taskId);

        TaskEntity entity = loadActiveTask(taskId);
        log.debug("Soft-deleting task '{}' (id={})", entity.getTitle(), taskId);
        entity.setDeletedAt(Instant.now());
        entity.setStatus(TaskStatus.DELETED);
        taskRepository.save(entity);
    }

    /** Loads an active (non-deleted) task by ID, throwing {@link TaskNotFoundException} if absent or soft-deleted. */
    private TaskEntity loadActiveTask(Long taskId) {
        return taskRepository.findById(taskId)
                .filter(e -> e.getDeletedAt() == null)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    /**
     * Loads a task entity by ID with a pessimistic write lock, throwing {@link TaskNotFoundException}
     * if absent or soft-deleted. The lock prevents concurrent status mutations (e.g. double-complete).
     */
    private TaskEntity concurrentSafeLoadActiveTask(Long taskId){
        return taskRepository.concurrentSafeFindById(taskId)
                .filter(e -> e.getDeletedAt() == null)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

}
