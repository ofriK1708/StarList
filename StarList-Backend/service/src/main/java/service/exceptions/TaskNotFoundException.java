package service.exceptions;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long taskId) {
        super("Task with the ID: " + taskId + " not found");
    }
}
