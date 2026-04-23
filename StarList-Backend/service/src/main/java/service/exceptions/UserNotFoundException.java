package service.exceptions;

import java.util.UUID;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(Long userId) {
        super("User not found", "User with the ID: " + userId + " not found");
    }
    public UserNotFoundException(UUID cognitoId)
    {
        super("User not found", "User with the Cognito ID: " + cognitoId + " not found");
    }
}
