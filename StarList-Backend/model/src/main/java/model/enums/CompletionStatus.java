package model.enums;

/** Represents the completion state of a habit for a single calendar day. */
public enum CompletionStatus {

    /** The day has passed and a completion record exists. */
    DONE,

    /** The day has passed and no completion record exists. */
    MISSED,

    /** The day is today or in the future — not yet actionable. */
    NA
}
