package service.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;
import model.domain.Task;
import model.enums.DifficultyLevel;
import model.enums.TaskStatus;

@Value
@Builder
public class TaskResponse {

    Long taskId;
    String title;
    String description;
    DifficultyLevel difficultyLevel;
    Integer durationMinutes;
    Integer coinReward;
    Integer coinPenalty;
    TaskStatus status;
    Instant dueDate;
    Instant createdAt;

    public static TaskResponse from(Task task) {
        return TaskResponse.builder()
                .taskId(task.getId())
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
