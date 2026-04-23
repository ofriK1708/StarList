package repository.dal;

import model.enums.DifficultyLevel;
import model.enums.HabitFrequency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import repository.api.HabitCompletionRepository;
import repository.api.HabitRepository;
import repository.api.UserRepository;
import repository.entity.HabitCompletionEntity;
import repository.entity.HabitEntity;
import repository.entity.UserEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class HabitCompletionRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private HabitCompletionRepository habitCompletionRepository;

    private UserEntity savedUser;
    private HabitEntity savedHabit;
    private HabitEntity savedHabit2;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(UserEntity.builder()
                .email("completionuser@example.com")
                .cognitoUserId(UUID.randomUUID())
                .displayName("Completion User")
                .build());
        savedHabit = habitRepository.save(HabitEntity.builder()
                .title("Morning Run")
                .frequency(HabitFrequency.DAILY)
                .difficultyLevel(DifficultyLevel.EASY)
                .user(savedUser)
                .build());
        savedHabit2 = habitRepository.save(HabitEntity.builder()
                .title("Meditate")
                .frequency(HabitFrequency.DAILY)
                .difficultyLevel(DifficultyLevel.EASY)
                .user(savedUser)
                .build());
    }

    @Test
    void existsByHabit_IdAndCompletedDate_existingCompletion_returnsTrue() {
        LocalDate today = LocalDate.now();
        habitCompletionRepository.save(completion(savedHabit, today));

        assertThat(habitCompletionRepository.existsByHabit_IdAndCompletedDate(savedHabit.getId(), today)).isTrue();
    }

    @Test
    void existsByHabit_IdAndCompletedDate_noCompletion_returnsFalse() {
        assertThat(habitCompletionRepository.existsByHabit_IdAndCompletedDate(savedHabit.getId(), LocalDate.now())).isFalse();
    }

    @Test
    void existsByHabit_IdAndCompletedDate_differentDate_returnsFalse() {
        LocalDate today = LocalDate.now();
        habitCompletionRepository.save(completion(savedHabit, today));

        assertThat(habitCompletionRepository.existsByHabit_IdAndCompletedDate(savedHabit.getId(), today.plusDays(1))).isFalse();
    }

    @Test
    void existsByHabit_IdAndCompletedDate_differentHabit_returnsFalse() {
        habitCompletionRepository.save(completion(savedHabit, LocalDate.now()));

        assertThat(habitCompletionRepository.existsByHabit_IdAndCompletedDate(savedHabit2.getId(), LocalDate.now())).isFalse();
    }

    @Test
    void findAllByHabit_IdInAndCompletedDateBetween_withinRange_returnsMatching() {
        LocalDate today = LocalDate.now();
        habitCompletionRepository.save(completion(savedHabit, today.minusDays(5)));
        habitCompletionRepository.save(completion(savedHabit, today.minusDays(3)));
        habitCompletionRepository.save(completion(savedHabit, today));

        List<HabitCompletionEntity> result = habitCompletionRepository
                .findAllByHabit_IdInAndCompletedDateBetween(List.of(savedHabit.getId()), today.minusDays(7), today);

        assertThat(result).hasSize(3);
    }

    @Test
    void findAllByHabit_IdInAndCompletedDateBetween_outsideRange_returnsEmpty() {
        LocalDate today = LocalDate.now();
        habitCompletionRepository.save(completion(savedHabit, today.minusDays(10)));
        habitCompletionRepository.save(completion(savedHabit, today.minusDays(8)));

        List<HabitCompletionEntity> result = habitCompletionRepository
                .findAllByHabit_IdInAndCompletedDateBetween(List.of(savedHabit.getId()), today.minusDays(5), today);

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByHabit_IdInAndCompletedDateBetween_multipleHabits_returnsAllMatchingHabits() {
        LocalDate today = LocalDate.now();
        habitCompletionRepository.save(completion(savedHabit, today.minusDays(1)));
        habitCompletionRepository.save(completion(savedHabit2, today.minusDays(2)));

        List<HabitCompletionEntity> result = habitCompletionRepository
                .findAllByHabit_IdInAndCompletedDateBetween(
                        List.of(savedHabit.getId(), savedHabit2.getId()),
                        today.minusDays(7), today);

        assertThat(result).hasSize(2);
    }

    @Test
    void findAllByHabit_IdInAndCompletedDateBetween_habitNotInList_excludedFromResults() {
        LocalDate today = LocalDate.now();
        habitCompletionRepository.save(completion(savedHabit2, today));

        List<HabitCompletionEntity> result = habitCompletionRepository
                .findAllByHabit_IdInAndCompletedDateBetween(List.of(savedHabit.getId()), today.minusDays(7), today);

        assertThat(result).isEmpty();
    }

    @Test
    void save_duplicateHabitAndDate_throwsDataIntegrityViolationException() {
        LocalDate today = LocalDate.now();
        habitCompletionRepository.save(completion(savedHabit, today));

        assertThrows(DataIntegrityViolationException.class,
                () -> habitCompletionRepository.save(completion(savedHabit, today)));
    }

    private HabitCompletionEntity completion(HabitEntity habit, LocalDate date) {
        return HabitCompletionEntity.builder()
                .habit(habit)
                .user(savedUser)
                .completedDate(date)
                .build();
    }
}
