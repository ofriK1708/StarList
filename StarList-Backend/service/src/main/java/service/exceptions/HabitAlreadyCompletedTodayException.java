package service.exceptions;

public class HabitAlreadyCompletedTodayException extends ConflictException {

    public HabitAlreadyCompletedTodayException(Long habitId) {
        super("Habit already completed today", "Habit with the ID: " + habitId + " has already been completed today");
    }
}
