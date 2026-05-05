package service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record AiChatRequest(
        @NotBlank String message,
        Boolean newConversation
) {}
