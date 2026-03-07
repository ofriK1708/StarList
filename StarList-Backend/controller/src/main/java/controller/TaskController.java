package controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.TaskService;
import service.dto.AddTaskRequest;
import service.dto.AddTaskResponse;

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
}
