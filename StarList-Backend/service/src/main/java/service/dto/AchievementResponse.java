package service.dto;

import java.time.Instant;
import lombok.Builder;

@Builder
public record AchievementResponse(
        String id,
        String name,
        String icon,
        String description,
        boolean unlocked,
        Instant unlockedAt
) {}
