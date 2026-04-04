package service.exceptions;

public class TaskAlreadyCompletedException extends ConflictException {

    public TaskAlreadyCompletedException(Long taskId) {
        super("Task already completed", "Task with the ID: " + taskId + " is already completed");
    }
}
