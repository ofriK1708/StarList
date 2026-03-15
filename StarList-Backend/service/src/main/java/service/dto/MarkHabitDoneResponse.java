package service.dto;

import lombok.Builder;

@Builder
public record MarkHabitDoneResponse(
        Long habitId,
        Integer coinsEarned,
        Integer newTotalCoins,
        Integer currentStreak,
        Integer bestStreak
) {}
