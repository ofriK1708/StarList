package model.enums;

public enum HabitFrequency {
    WEEKLY,
    DAILY,
    CUSTOM,
    /** User selects 2–6 specific days per week; each selected day is an independent completion slot. */
    MULTI_DAY
}

