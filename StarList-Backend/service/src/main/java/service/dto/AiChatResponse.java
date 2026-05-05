package service.dto;

import java.time.Instant;
import lombok.Builder;
import model.enums.ConversationType;

@Builder
public record AiChatResponse(
        Long conversationId,
        String aiMessage,
        ConversationType conversationType,
        Integer tasksCreated,
        Instant createdAt
) {}
