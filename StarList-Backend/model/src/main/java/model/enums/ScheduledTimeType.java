package model.enums;

public enum ScheduledTimeType {
    /** 00:00 – 11:59 */
    MORNING,
    /** 12:00 – 16:59 */
    AFTERNOON,
    /** 17:00 – 22:59 */
    EVENING,
    /** User-specified hour stored in scheduledHour (0–23). */
    CUSTOM
}
