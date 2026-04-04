package repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "habit_completions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_habit_completion_per_day", columnNames = {"habit_id", "completed_date"})
        },
        indexes = {
                @Index(name = "idx_habit_completions_user_id", columnList = "user_id"),
                @Index(name = "idx_habit_completions_habit_id", columnList = "habit_id"),
                @Index(name = "idx_habit_completions_completed_date", columnList = "completed_date")
        })
public class HabitCompletionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "habit_id", nullable = false)
    private HabitEntity habit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Builder.Default
    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate = LocalDate.now();

    @Builder.Default
    @Column(name = "coins_earned", nullable = false)
    private Integer coinsEarned = 0;

    @Builder.Default
    @Column(name = "streak_at_completion", nullable = false)
    private Integer streakAtCompletion = 0;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
