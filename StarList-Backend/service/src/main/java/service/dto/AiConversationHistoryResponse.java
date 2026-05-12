package service.dto;

import java.time.Instant;
import lombok.Builder;
import model.enums.ConversationType;
import repository.entity.AiConversationEntity;

@Builder
public record AiConversationHistoryResponse(
        Long conversationId,
        String userMessage,
        String aiResponse,
        ConversationType conversationType,
        Integer tasksCreated,
        Instant createdAt
) {

    public static AiConversationHistoryResponse from(AiConversationEntity entity) {
        return AiConversationHistoryResponse.builder()
                .conversationId(entity.getId())
                .userMessage(entity.getUserMessage())
                .aiResponse(entity.getAiResponse())
                .conversationType(entity.getConversationType())
                .tasksCreated(entity.getTasksCreated())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
