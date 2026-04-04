package service.exceptions;

public class ItemAlreadyOwnedException extends ConflictException {

    public ItemAlreadyOwnedException(Long itemId) {
        super("Item already owned", "User already owns catalog item with ID " + itemId);
    }
}
