package repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.util.List;
import repository.converter.IntegerListConverter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import jakarta.persistence.Table;
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
import model.enums.ScheduledTimeType;

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
public class HabitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 16)
    private HabitFrequency frequency;

    @Column(name = "scheduled_day_of_week")
    private Integer scheduledDayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "scheduled_time_type", length = 16)
    private ScheduledTimeType scheduledTimeType;

    @Column(name = "scheduled_hour")
    private Integer scheduledHour;

    @Column(name = "custom_interval_days")
    private Integer customIntervalDays;

    /**
     * ISO days of week selected for MULTI_DAY habits (1=Mon … 7=Sun), stored as a
     * comma-separated string (e.g. "1,3,5"). Null for all other frequencies.
     */
    @Convert(converter = IntegerListConverter.class)
    @Column(name = "scheduled_days_of_week", length = 20)
    private List<Integer> scheduledDaysOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false, length = 16)
    private DifficultyLevel difficultyLevel;

    @Builder.Default
    @Column(name = "coin_reward", nullable = false)
    private Integer coinReward = 0;

    @Column(name = "coin_penalty")
    private Integer coinPenalty;

    @Builder.Default
    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak = 0;

    @Builder.Default
    @Column(name = "best_streak", nullable = false)
    private Integer bestStreak = 0;

    @Builder.Default
    @Column(name = "total_completions", nullable = false)
    private Integer totalCompletions = 0;

    @Column(name = "last_completed_date")
    private LocalDate lastCompletedDate;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder.Default
    @OneToMany(mappedBy = "habit")
    private List<HabitCompletionEntity> completions = new ArrayList<>();
}
