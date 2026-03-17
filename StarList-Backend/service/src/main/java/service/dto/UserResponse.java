package service.dto;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;
import model.domain.User;

@Builder
public record UserResponse(
        Long id,
        String email,
        String displayName,
        Integer totalCoins,
        Integer lifetimeCoinsEarned,
        Integer currentGalaxyCycle,
        LocalDate galaxyResetDate,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .totalCoins(user.getTotalCoins())
                .lifetimeCoinsEarned(user.getLifetimeCoinsEarned())
                .currentGalaxyCycle(user.getCurrentGalaxyCycle())
                .galaxyResetDate(user.getGalaxyResetDate())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
