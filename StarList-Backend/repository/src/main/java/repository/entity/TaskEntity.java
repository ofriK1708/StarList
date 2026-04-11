package repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import model.enums.DifficultyLevel;
import model.enums.TaskStatus;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_user_id", columnList = "user_id"),
        @Index(name = "idx_tasks_due_date", columnList = "due_date"),
        @Index(name = "idx_tasks_status", columnList = "status"),
        @Index(name = "idx_tasks_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_tasks_ai_conversation_id", columnList = "ai_conversation_id")
})
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Optimistic lock version — JPA increments this on every UPDATE and adds
     * {@code AND version = ?} to the WHERE clause. If two transactions read the
     * same version and both try to save, the second one finds the version already
     * bumped and throws {@link org.springframework.orm.ObjectOptimisticLockingFailureException},
     * preventing double-completion and double coin awards.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false, length = 16)
    private DifficultyLevel difficultyLevel;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Builder.Default
    @Column(name = "coin_reward", nullable = false)
    private Integer coinReward = 0;

    @Column(name = "coin_penalty")
    private Integer coinPenalty;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private TaskStatus status = TaskStatus.PENDING;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder.Default
    @Column(name = "created_by_ai", nullable = false)
    private Boolean createdByAi = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_conversation_id")
    private AiConversationEntity aiConversation;
}
