package repository.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
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
    private String cognitoUserId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "total_coins", nullable = false)
    private Integer totalCoins;

    @Column(name = "lifetime_coins_earned", nullable = false)
    private Integer lifetimeCoinsEarned;

    @Column(name = "current_galaxy_cycle", nullable = false)
    private Integer currentGalaxyCycle;

    @Column(name = "galaxy_reset_date", nullable = false)
    private LocalDate galaxyResetDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskEntity> tasks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HabitEntity> habits = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HabitCompletionEntity> habitCompletions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GalaxyItemEntity> galaxyItems = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CoinTransactionEntity> coinTransactions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiConversationEntity> aiConversations = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (totalCoins == null) {
            totalCoins = 0;
        }
        if (lifetimeCoinsEarned == null) {
            lifetimeCoinsEarned = 0;
        }
        if (currentGalaxyCycle == null) {
            currentGalaxyCycle = 1;
        }
        if (galaxyResetDate == null) {
            galaxyResetDate = LocalDate.now();
        }
    }
}
