package model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "habits", indexes = {
        @Index(name = "idx_habits_user_id", columnList = "user_id"),
        @Index(name = "idx_habits_frequency", columnList = "frequency"),
        @Index(name = "idx_habits_deleted_at", columnList = "deleted_at")
})
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 16)
    private HabitFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false, length = 16)
    private DifficultyLevel difficultyLevel;

    @Min(0)
    @Column(name = "coin_reward", nullable = false)
    private Integer coinReward;

    @Min(0)
    @Column(name = "coin_penalty")
    private Integer coinPenalty;

    @Min(0)
    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak;

    @Min(0)
    @Column(name = "best_streak", nullable = false)
    private Integer bestStreak;

    @Min(0)
    @Column(name = "total_completions", nullable = false)
    private Integer totalCompletions;

    @Column(name = "last_completed_date")
    private LocalDate lastCompletedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder.Default
    @OneToMany(mappedBy = "habit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HabitCompletion> completions = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (coinReward == null) {
            coinReward = 0;
        }
        if (currentStreak == null) {
            currentStreak = 0;
        }
        if (bestStreak == null) {
            bestStreak = 0;
        }
        if (totalCompletions == null) {
            totalCompletions = 0;
        }
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }
}



