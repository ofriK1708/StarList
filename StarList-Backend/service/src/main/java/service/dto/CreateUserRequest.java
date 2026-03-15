package service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String cognitoUserId,
        @NotBlank String displayName
) {}
