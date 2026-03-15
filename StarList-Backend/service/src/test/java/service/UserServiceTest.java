package service;

import model.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.api.UserRepository;
import repository.entity.UserEntity;
import repository.mapper.UserMapper;
import service.dto.CreateUserRequest;
import service.dto.UpdateUserRequest;
import service.dto.UserResponse;
import service.exceptions.UserAlreadyExistsException;
import service.exceptions.UserNotFoundException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;

    @InjectMocks UserService userService;

    // ── createUser happy path ─────────────────────────────────────────────────

    @Test
    void createUser_newUser_savesAndReturnsResponse() {
        CreateUserRequest request = new CreateUserRequest("alice@example.com", "cognito-abc", "Alice");

        User savedDomain = User.builder()
                .id(1L)
                .email("alice@example.com")
                .cognitoUserId("cognito-abc")
                .displayName("Alice")
                .totalCoins(0)
                .lifetimeCoinsEarned(0)
                .currentGalaxyCycle(1)
                .galaxyResetDate(LocalDate.now())
                .createdAt(Instant.now())
                .build();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByCognitoUserId("cognito-abc")).thenReturn(false);
        when(userMapper.fromDomain(any(User.class))).thenReturn(UserEntity.builder().build());
        when(userRepository.save(any(UserEntity.class))).thenReturn(UserEntity.builder().id(1L).build());
        when(userMapper.toDomain(any(UserEntity.class))).thenReturn(savedDomain);

        UserResponse response = userService.createUser(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.displayName()).isEqualTo("Alice");
        assertThat(response.totalCoins()).isEqualTo(0);

        verify(userRepository).save(any(UserEntity.class));
    }

    // ── createUser sad paths ──────────────────────────────────────────────────

    @Test
    void createUser_duplicateEmail_throwsUserAlreadyExistsException() {
        CreateUserRequest request = new CreateUserRequest("alice@example.com", "cognito-abc", "Alice");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_duplicateCognitoUserId_throwsUserAlreadyExistsException() {
        CreateUserRequest request = new CreateUserRequest("alice@example.com", "cognito-abc", "Alice");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByCognitoUserId("cognito-abc")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("cognitoUserId");

        verify(userRepository, never()).save(any());
    }

    // ── getUser ───────────────────────────────────────────────────────────────

    @Test
    void getUser_existingId_returnsResponse() {
        UserEntity entity = UserEntity.builder().id(1L).email("alice@example.com").displayName("Alice").build();
        User domain = User.builder().id(1L).email("alice@example.com").displayName("Alice").totalCoins(0)
                .lifetimeCoinsEarned(0).currentGalaxyCycle(1).galaxyResetDate(LocalDate.now())
                .createdAt(Instant.now()).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userMapper.toDomain(entity)).thenReturn(domain);

        UserResponse response = userService.getUser(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("alice@example.com");
    }

    @Test
    void getUser_unknownId_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── updateUser ────────────────────────────────────────────────────────────

    @Test
    void updateUser_existingId_updatesDisplayNameAndReturnsResponse() {
        UserEntity entity = UserEntity.builder().id(1L).email("alice@example.com").displayName("Alice")
                .totalCoins(0).lifetimeCoinsEarned(0).currentGalaxyCycle(1)
                .galaxyResetDate(LocalDate.now()).createdAt(Instant.now()).build();
        User updatedDomain = User.builder().id(1L).email("alice@example.com").displayName("Alice Updated")
                .totalCoins(0).lifetimeCoinsEarned(0).currentGalaxyCycle(1)
                .galaxyResetDate(LocalDate.now()).createdAt(Instant.now()).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toDomain(entity)).thenReturn(updatedDomain);

        UserResponse response = userService.updateUser(1L, new UpdateUserRequest("Alice Updated"));

        assertThat(entity.getDisplayName()).isEqualTo("Alice Updated");
        assertThat(response.displayName()).isEqualTo("Alice Updated");
        verify(userRepository).save(entity);
    }

    @Test
    void updateUser_unknownId_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, new UpdateUserRequest("New Name")))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    void deleteUser_existingId_deletesEntity() {
        UserEntity entity = UserEntity.builder().id(1L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        userService.deleteUser(1L);

        verify(userRepository).delete(entity);
    }

    @Test
    void deleteUser_unknownId_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).delete(any());
    }
}
