package service.dto;

import lombok.Builder;

@Builder
public record MarkTaskDoneResponse(
        Long taskId,
        Integer coinsEarned,
        Integer newTotalCoins
) {}
