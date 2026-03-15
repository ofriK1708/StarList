package service.exceptions;

public class HabitAlreadyCompletedTodayException extends RuntimeException {

    public HabitAlreadyCompletedTodayException(Long habitId) {
        super("Habit with the ID: " + habitId + " has already been completed today");
    }
}
