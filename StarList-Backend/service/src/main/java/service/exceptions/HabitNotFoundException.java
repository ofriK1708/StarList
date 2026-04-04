package service.exceptions;

public class HabitNotFoundException extends NotFoundException {

    public HabitNotFoundException(Long habitId) {
        super("Habit not found", "Habit with the ID: " + habitId + " not found");
    }
}
