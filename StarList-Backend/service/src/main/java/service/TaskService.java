package service;

import model.domain.Task;
import model.enums.DifficultyLevel;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.api.TaskRepository;
import repository.entity.UserEntity;
import repository.mapper.TaskMapper;
import service.dto.AddTaskRequest;
import service.dto.AddTaskResponse;

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

        UserEntity userEntity = userService.findEntityById(userId);

        int[] coins = computeCoins(request.getDifficultyLevel());

        Task task = Task.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .difficultyLevel(request.getDifficultyLevel())
                .durationMinutes(request.getDurationMinutes())
                .dueDate(request.getDueDate())
                .coinReward(coins[0])
                .coinPenalty(coins[1])
                .createdByAi(false)
                .build();

        return AddTaskResponse.from(
                taskMapper.toDomain(
                        taskRepository.save(
                                taskMapper.fromDomain(task, userEntity))));
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
