package repository.service;

import model.domain.Task;
import org.springframework.stereotype.Service;
import repository.api.TaskRepository;
import repository.api.UserRepository;
import repository.mapper.TaskMapper;

@Service
public class TaskStore {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    public TaskStore(TaskRepository taskRepository, UserRepository userRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
    }

    public Task save(Task task) {
        var user = userRepository.findById(task.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + task.getUserId()));
        return taskMapper.toDomain(taskRepository.save(taskMapper.fromDomain(task, user)));
    }
}
