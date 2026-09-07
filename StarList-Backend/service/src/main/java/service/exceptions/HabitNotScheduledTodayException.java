package service.exceptions;

public class HabitNotScheduledTodayException extends ConflictException {

    public HabitNotScheduledTodayException(Long habitId) {
        super("Habit not scheduled today",
                "Habit " + habitId + " is not scheduled for today — complete it on a scheduled day");
    }
}
