package service.exceptions;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(Long userId) {
        super("User not found", "User with the ID: " + userId + " not found");
    }

}
