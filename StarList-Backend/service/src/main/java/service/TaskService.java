package service;

import model.domain.Task;
import model.enums.DifficultyLevel;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import repository.service.TaskStore;
import repository.service.UserStore;
import service.dto.AddTaskRequest;
import service.dto.AddTaskResponse;

@Service
public class TaskService {

    private final UserStore userStore;
    private final TaskStore taskStore;

    public TaskService(UserStore userStore, TaskStore taskStore) {
        this.userStore = userStore;
        this.taskStore = taskStore;
    }

    public AddTaskResponse addTask(Long userId, @NonNull AddTaskRequest request) {
        if (!userStore.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }

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

        return AddTaskResponse.from(taskStore.save(task));
    }

    @Contract(pure = true)
    private int @NonNull [] computeCoins(DifficultyLevel level) {
        return switch (level) {
            case NONE      -> new int[]{0,   0};
            case EASY      -> new int[]{10,  5};
            case MEDIUM    -> new int[]{25,  10};
            case HARD      -> new int[]{50,  25};
            case VERY_HARD -> new int[]{100, 50};
            case EXTREME   -> new int[]{200, 100};
        };
    }
}
