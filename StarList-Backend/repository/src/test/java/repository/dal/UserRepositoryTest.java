package repository.dal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import repository.api.UserRepository;
import repository.entity.UserEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        userRepository.save(UserEntity.builder()
                .email("alice@example.com")
                .cognitoUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .displayName("Alice")
                .build());

        assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
    }

    @Test
    void existsByEmail_unknownEmail_returnsFalse() {
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void existsByCognitoUserId_existingId_returnsTrue() {
        userRepository.save(UserEntity.builder()
                .email("bob@example.com")
                .cognitoUserId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .displayName("Bob")
                .build());

        assertThat(userRepository.existsByCognitoUserId(UUID.fromString("00000000-0000-0000-0000-000000000002"))).isTrue();
    }

    @Test
    void existsByCognitoUserId_unknownId_returnsFalse() {
        assertThat(userRepository.existsByCognitoUserId(UUID.fromString("00000000-0000-0000-0000-000000000099"))).isFalse();
    }

    @Test
    void save_duplicateEmail_throwsDataIntegrityViolationException() {
        userRepository.save(UserEntity.builder()
                .email("dup@example.com")
                .cognitoUserId(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                .displayName("User One")
                .build());

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(UserEntity.builder()
                .email("dup@example.com")
                .cognitoUserId(UUID.fromString("00000000-0000-0000-0000-000000000004"))
                .displayName("User Two")
                .build()));
    }

    @Test
    void save_duplicateCognitoUserId_throwsDataIntegrityViolationException() {
        userRepository.save(UserEntity.builder()
                .email("user1@example.com")
                .cognitoUserId(UUID.fromString("00000000-0000-0000-0000-000000000005"))
                .displayName("User One")
                .build());

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(UserEntity.builder()
                .email("user2@example.com")
                .cognitoUserId(UUID.fromString("00000000-0000-0000-0000-000000000005"))
                .displayName("User Two")
                .build()));
    }
}
