package service.exceptions;

public class ItemNotFoundException extends NotFoundException {

    public ItemNotFoundException(Long itemId) {
        super("Item not found", "Catalog item with ID " + itemId + " not found");
    }
}
