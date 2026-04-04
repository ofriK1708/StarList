package model.domain;

import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String email;
    private String cognitoUserId;
    private String displayName;

    @Builder.Default
    private Integer totalCoins = 0;

    @Builder.Default
    private Integer lifetimeCoinsEarned = 0;

    @Builder.Default
    private Integer currentGalaxyCycle = 1;

    @Builder.Default
    private LocalDate galaxyResetDate = LocalDate.now();

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant lastLogin;
}
