package controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.TaskService;
import service.dto.AddTaskRequest;
import service.dto.AddTaskResponse;
import service.dto.MarkTaskDoneResponse;
import service.dto.TaskResponse;
import service.dto.UpdateTaskRequest;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<AddTaskResponse> addTask(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddTaskRequest request) {
        AddTaskResponse result = taskService.addTask(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTask(taskId));
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getUserTasks(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(taskService.getUserTasks(userId));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(taskId, request));
    }

    @PostMapping("/{taskId}/complete")
    public ResponseEntity<MarkTaskDoneResponse> completeTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.completeTask(taskId));
    }

    @DeleteMapping("/{taskId}/due-date")
    public ResponseEntity<TaskResponse> clearDueDate(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.clearDueDate(taskId));
    }

    @DeleteMapping("/{taskId}/duration")
    public ResponseEntity<TaskResponse> clearDurationMinutes(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.clearDurationMinutes(taskId));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
}
