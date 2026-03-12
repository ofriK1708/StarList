package service.exceptions;

public class HabitNotFoundException extends RuntimeException {

    public HabitNotFoundException(Long habitId) {
        super("Habit with the ID: " + habitId + " not found");
    }
}
