package service.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;
import model.domain.Task;
import model.enums.DifficultyLevel;
import model.enums.TaskStatus;

@Value
@Builder
public class AddTaskResponse {

    String title;
    String description;
    DifficultyLevel difficultyLevel;
    Integer durationMinutes;
    Integer coinReward;
    Integer coinPenalty;
    TaskStatus status;
    Instant dueDate;
    Instant createdAt;

    public static AddTaskResponse from(Task task) {
        return AddTaskResponse.builder()
                .title(task.getTitle())
                .description(task.getDescription())
                .difficultyLevel(task.getDifficultyLevel())
                .durationMinutes(task.getDurationMinutes())
                .coinReward(task.getCoinReward())
                .coinPenalty(task.getCoinPenalty())
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
