package model.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.enums.DifficultyLevel;
import model.enums.TaskStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    private Long id;
    private Long userId;
    private String title;
    private String description;
    private DifficultyLevel difficultyLevel;
    private Integer durationMinutes;
    private Integer coinReward;
    private Integer coinPenalty;
    private TaskStatus status;
    private Instant dueDate;
    private Instant completedAt;
    private Instant createdAt;
    private Instant deletedAt;
    private Boolean createdByAi;
    private Long aiConversationId;
}
