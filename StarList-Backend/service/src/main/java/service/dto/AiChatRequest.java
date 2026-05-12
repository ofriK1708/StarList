package service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record AiChatRequest(
        @NotBlank String message,
        Boolean newConversation,
        /** IANA timezone string from the client (e.g. "Asia/Jerusalem"). Null falls back to UTC. */
        String userTimezone
) {}
