package repository.dal;

import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import repository.api.HabitRepository;
import repository.api.UserRepository;
import repository.entity.HabitEntity;
import repository.entity.UserEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class HabitRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HabitRepository habitRepository;

    private UserEntity savedUser;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(UserEntity.builder()
                .email("habituser@example.com")
                .cognitoUserId("cog-habituser")
                .displayName("Habit User")
                .build());
    }

    @Test
    void findAllByUser_IdAndDeletedAtIsNull_noDeletedHabits_returnsAll() {
        habitRepository.save(habit("Morning Run").build());
        habitRepository.save(habit("Read").build());
        habitRepository.save(habit("Meditate").build());

        List<HabitEntity> result = habitRepository.findAllByUser_IdAndDeletedAtIsNull(savedUser.getId());

        assertThat(result).hasSize(3);
    }

    @Test
    void findAllByUser_IdAndDeletedAtIsNull_someDeleted_excludesDeletedHabits() {
        habitRepository.save(habit("Active 1").build());
        habitRepository.save(habit("Active 2").build());
        habitRepository.save(habit("Deleted").deletedAt(Instant.now()).build());

        List<HabitEntity> result = habitRepository.findAllByUser_IdAndDeletedAtIsNull(savedUser.getId());

        assertThat(result).hasSize(2)
                .extracting(HabitEntity::getTitle)
                .containsExactlyInAnyOrder("Active 1", "Active 2");
    }

    @Test
    void findAllByUser_IdAndDeletedAtIsNull_allDeleted_returnsEmptyList() {
        habitRepository.save(habit("Deleted 1").deletedAt(Instant.now()).build());
        habitRepository.save(habit("Deleted 2").deletedAt(Instant.now()).build());

        List<HabitEntity> result = habitRepository.findAllByUser_IdAndDeletedAtIsNull(savedUser.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByUser_IdAndDeletedAtIsNull_differentUser_returnsOnlyOwnHabits() {
        UserEntity otherUser = userRepository.save(UserEntity.builder()
                .email("other@example.com")
                .cognitoUserId("cog-other")
                .displayName("Other User")
                .build());
        habitRepository.save(habit("My Habit").build());
        habitRepository.save(HabitEntity.builder()
                .title("Their Habit")
                .frequency(HabitFrequency.DAILY)
                .difficultyLevel(DifficultyLevel.EASY)
                .user(otherUser)
                .build());

        List<HabitEntity> result = habitRepository.findAllByUser_IdAndDeletedAtIsNull(savedUser.getId());

        assertThat(result).hasSize(1)
                .extracting(HabitEntity::getTitle)
                .containsExactly("My Habit");
    }

    /** Returns a pre-configured builder for a non-deleted habit belonging to {@code savedUser}. */
    private HabitEntity.HabitEntityBuilder habit(String title) {
        return HabitEntity.builder()
                .title(title)
                .frequency(HabitFrequency.DAILY)
                .difficultyLevel(DifficultyLevel.EASY)
                .user(savedUser);
    }
}
