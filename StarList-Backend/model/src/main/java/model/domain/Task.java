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

    @Builder.Default
    private Integer coinReward = 0;

    private Integer coinPenalty;

    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    private Instant dueDate;
    private Instant completedAt;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant deletedAt;

    @Builder.Default
    private Boolean createdByAi = Boolean.FALSE;

    private Long aiConversationId;
}
