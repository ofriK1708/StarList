package repository.dal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import repository.api.UserRepository;
import repository.entity.UserEntity;

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
                .cognitoUserId("cog-alice")
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
                .cognitoUserId("cog-bob")
                .displayName("Bob")
                .build());

        assertThat(userRepository.existsByCognitoUserId("cog-bob")).isTrue();
    }

    @Test
    void existsByCognitoUserId_unknownId_returnsFalse() {
        assertThat(userRepository.existsByCognitoUserId("cog-nobody")).isFalse();
    }

    @Test
    void save_duplicateEmail_throwsDataIntegrityViolationException() {
        userRepository.save(UserEntity.builder()
                .email("dup@example.com")
                .cognitoUserId("cog-dup-1")
                .displayName("User One")
                .build());

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(UserEntity.builder()
                .email("dup@example.com")
                .cognitoUserId("cog-dup-2")
                .displayName("User Two")
                .build()));
    }

    @Test
    void save_duplicateCognitoUserId_throwsDataIntegrityViolationException() {
        userRepository.save(UserEntity.builder()
                .email("user1@example.com")
                .cognitoUserId("cog-shared")
                .displayName("User One")
                .build());

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(UserEntity.builder()
                .email("user2@example.com")
                .cognitoUserId("cog-shared")
                .displayName("User Two")
                .build()));
    }
}
