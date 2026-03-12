package service;

import java.time.Instant;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import model.domain.Task;
import model.enums.DifficultyLevel;
import model.enums.TaskStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.api.TaskRepository;
import repository.entity.TaskEntity;
import repository.entity.UserEntity;
import repository.mapper.TaskMapper;
import service.dto.AddTaskRequest;
import service.dto.AddTaskResponse;
import service.dto.TaskResponse;
import service.dto.UpdateTaskRequest;
import service.exceptions.TaskNotFoundException;

@Slf4j
@Service
public class TaskService {

    private final UserService userService;
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskService(UserService userService, TaskRepository taskRepository, TaskMapper taskMapper) {
        this.userService = userService;
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    @Transactional
    public AddTaskResponse addTask(Long userId, @NonNull AddTaskRequest request) {
        log.info("About to add task for user {}", userId);

        UserEntity userEntity = userService.findEntityById(userId);

        int[] coins = computeCoins(request.difficultyLevel());

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

        boolean difficultyChanged = !entity.getDifficultyLevel().equals(request.getDifficultyLevel());

        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setDifficultyLevel(request.getDifficultyLevel());
        entity.setDurationMinutes(request.getDurationMinutes());
        entity.setDueDate(request.getDueDate());

        if (difficultyChanged) {
            int[] coins = computeCoins(request.getDifficultyLevel());
            entity.setCoinReward(coins[0]);
            entity.setCoinPenalty(coins[1]);
        }

        return TaskResponse.from(
                taskMapper.toDomain(
                        taskRepository.save(entity)));
    }

    @Transactional
    public void deleteTask(Long taskId) {
        log.info("About to delete task {}", taskId);

        TaskEntity entity = loadActiveTask(taskId);
        entity.setDeletedAt(Instant.now());
        entity.setStatus(TaskStatus.DELETED);
        taskRepository.save(entity);
    }

    /**
     * Loads a task entity by ID, throwing {@link TaskNotFoundException} if absent or soft-deleted.
     */
    private TaskEntity loadActiveTask(Long taskId) {
        return taskRepository.findById(taskId)
                .filter(e -> e.getDeletedAt() == null)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    @Contract(pure = true)
    private int @NonNull [] computeCoins(DifficultyLevel level) {
        return switch (level) {
            case NONE -> new int[]{0, 0};
            case EASY -> new int[]{10, 5};
            case MEDIUM -> new int[]{25, 10};
            case HARD -> new int[]{50, 25};
            case VERY_HARD -> new int[]{100, 50};
            case EXTREME -> new int[]{200, 100};
        };
    }
}
