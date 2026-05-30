package repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
@Table(name = "users", indexes = {
        @Index(name = "idx_users_galaxy_reset_date", columnList = "galaxy_reset_date"),
        @Index(name = "idx_users_last_login", columnList = "last_login")
})
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, updatable = false)
    private String email;

    @Column(name = "cognito_user_id", nullable = false, unique = true, updatable = false)
    private UUID cognitoUserId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Builder.Default
    @Column(name = "total_coins", nullable = false)
    private Integer totalCoins = 0;

    @Builder.Default
    @Column(name = "lifetime_coins_earned", nullable = false)
    private Integer lifetimeCoinsEarned = 0;

    @Builder.Default
    @Column(name = "current_galaxy_cycle", nullable = false)
    private Integer currentGalaxyCycle = 1;

    @Builder.Default
    @Column(name = "galaxy_reset_date", nullable = false)
    private LocalDate galaxyResetDate = LocalDate.now();

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_login")
    private Instant lastLogin;

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<TaskEntity> tasks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<HabitEntity> habits = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<HabitCompletionEntity> habitCompletions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<GalaxyItemEntity> galaxyItems = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<CoinTransactionEntity> coinTransactions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<AiConversationEntity> aiConversations = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<AchievementEntity> achievements = new ArrayList<>();
}
