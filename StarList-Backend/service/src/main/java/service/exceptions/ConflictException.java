package service.exceptions;

import lombok.Getter;

/**
 * Base class for all 409 Conflict exceptions.
 * Subclasses supply the human-readable {@code title} shown in the problem-detail response.
 */
@Getter
public abstract class ConflictException extends RuntimeException {

    private final String title;

    protected ConflictException(String title, String message) {
        super(message);
        this.title = title;
    }

}
