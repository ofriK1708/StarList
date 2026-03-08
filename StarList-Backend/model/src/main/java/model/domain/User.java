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
    private Integer totalCoins;
    private Integer lifetimeCoinsEarned;
    private Integer currentGalaxyCycle;
    private LocalDate galaxyResetDate;
    private Instant createdAt;
    private Instant lastLogin;
}
