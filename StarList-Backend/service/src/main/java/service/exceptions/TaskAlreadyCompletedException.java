package service.exceptions;

public class TaskAlreadyCompletedException extends RuntimeException {

    public TaskAlreadyCompletedException(Long taskId) {
        super("Task with the ID: " + taskId + " is already completed");
    }
}
