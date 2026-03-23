package service.exceptions;

import lombok.Getter;

/**
 * Base class for all 404 Not Found exceptions.
 * Subclasses supply the human-readable {@code title} shown in the problem-detail response.
 */
@Getter
public abstract class NotFoundException extends RuntimeException {

    private final String title;

    protected NotFoundException(String title, String message) {
        super(message);
        this.title = title;
    }

}
