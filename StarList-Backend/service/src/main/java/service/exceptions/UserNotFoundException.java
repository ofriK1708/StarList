package service.exceptions;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User with the ID: " + userId + " not found");
    }

}
