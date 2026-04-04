package service.exceptions;

public class InsufficientCoinsException extends ConflictException {

    public InsufficientCoinsException(int required, int available) {
        super("Insufficient coins", "Purchase requires " + required + " coins but user only has " + available);
    }
}
