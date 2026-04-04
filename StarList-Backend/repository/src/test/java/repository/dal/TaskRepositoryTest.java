package repository.dal;

import model.enums.DifficultyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import repository.api.TaskRepository;
import repository.api.UserRepository;
import repository.entity.TaskEntity;
import repository.entity.UserEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class TaskRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    private UserEntity savedUser;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(UserEntity.builder()
                .email("taskuser@example.com")
                .cognitoUserId("cog-taskuser")
                .displayName("Task User")
                .build());
    }

    @Test
    void findAllByUser_IdAndDeletedAtIsNull_noDeletedTasks_returnsAll() {
        taskRepository.save(task("Task A").build());
        taskRepository.save(task("Task B").build());
        taskRepository.save(task("Task C").build());

        List<TaskEntity> result = taskRepository.findAllByUser_IdAndDeletedAtIsNull(savedUser.getId());

        assertThat(result).hasSize(3);
    }

    @Test
    void findAllByUser_IdAndDeletedAtIsNull_someDeleted_excludesDeletedTasks() {
        taskRepository.save(task("Active 1").build());
        taskRepository.save(task("Active 2").build());
        taskRepository.save(task("Deleted").deletedAt(Instant.now()).build());

        List<TaskEntity> result = taskRepository.findAllByUser_IdAndDeletedAtIsNull(savedUser.getId());

        assertThat(result).hasSize(2)
                .extracting(TaskEntity::getTitle)
                .containsExactlyInAnyOrder("Active 1", "Active 2");
    }

    @Test
    void findAllByUser_IdAndDeletedAtIsNull_allDeleted_returnsEmptyList() {
        taskRepository.save(task("Deleted 1").deletedAt(Instant.now()).build());
        taskRepository.save(task("Deleted 2").deletedAt(Instant.now()).build());

        List<TaskEntity> result = taskRepository.findAllByUser_IdAndDeletedAtIsNull(savedUser.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByUser_IdAndDeletedAtIsNull_differentUser_returnsOnlyOwnTasks() {
        UserEntity otherUser = userRepository.save(UserEntity.builder()
                .email("other@example.com")
                .cognitoUserId("cog-other")
                .displayName("Other User")
                .build());
        taskRepository.save(task("My Task").build());
        taskRepository.save(TaskEntity.builder()
                .title("Their Task")
                .difficultyLevel(DifficultyLevel.EASY)
                .user(otherUser)
                .build());

        List<TaskEntity> result = taskRepository.findAllByUser_IdAndDeletedAtIsNull(savedUser.getId());

        assertThat(result).hasSize(1)
                .extracting(TaskEntity::getTitle)
                .containsExactly("My Task");
    }

    /** Returns a pre-configured builder for a non-deleted task belonging to {@code savedUser}. */
    private TaskEntity.TaskEntityBuilder task(String title) {
        return TaskEntity.builder()
                .title(title)
                .difficultyLevel(DifficultyLevel.EASY)
                .user(savedUser);
    }
}
