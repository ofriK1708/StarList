package service.exceptions;

public class ItemNotAvailableException extends ConflictException {

    public ItemNotAvailableException(Long itemId) {
        super("Item not available", "Catalog item with ID " + itemId + " is not currently available for purchase");
    }
}
