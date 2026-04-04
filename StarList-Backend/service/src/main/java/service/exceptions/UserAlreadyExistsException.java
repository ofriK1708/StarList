package service.exceptions;

import lombok.Getter;

@Getter
public class UserAlreadyExistsException extends ConflictException {
    private final String field;
    private final String value;

    public UserAlreadyExistsException(String field, String value) {
        super("User already exists", "User already exists with the same " + field);
        this.field = field;
        this.value = value;
    }

}
