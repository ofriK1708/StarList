package service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateUserRequest(
        @NotBlank String displayName
) {}
