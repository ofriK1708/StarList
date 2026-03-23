package service.exceptions;

public class TaskNotFoundException extends NotFoundException {

    public TaskNotFoundException(Long taskId) {
        super("Task not found", "Task with the ID: " + taskId + " not found");
    }
}
