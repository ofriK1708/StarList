package service;

import lombok.extern.slf4j.Slf4j;
import model.domain.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.api.UserRepository;
import repository.entity.UserEntity;
import repository.mapper.UserMapper;
import service.dto.CreateUserRequest;
import service.dto.UpdateUserRequest;
import service.dto.UserResponse;
import service.exceptions.UserAlreadyExistsException;
import service.exceptions.UserNotFoundException;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public boolean existsById(Long id) {
        log.info("Checking existence of user {}", id);
        return userRepository.existsById(id);
    }

    /**
     * Finds a user by ID and maps it to the domain model.
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    public User findById(Long id) {
        log.info("Finding user {}", id);
        return userMapper.toDomain(userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id)));
    }

    /**
     * Finds a user by ID and returns the raw JPA entity.
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    public UserEntity findEntityById(Long id) {
        log.info("Finding user entity {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User save(User user) {
        log.info("Saving user {}", user.getEmail());
        return userMapper.toDomain(userRepository.save(userMapper.fromDomain(user)));
    }

    /**
     * Creates a new user after checking for duplicate email and cognitoUserId.
     *
     * @throws UserAlreadyExistsException if a user with the same email or cognitoUserId exists
     */
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user \"{}\"", request.displayName());
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("email", request.email());
        }
        if (userRepository.existsByCognitoUserId(request.cognitoUserId())) {
            throw new UserAlreadyExistsException("cognitoUserId", request.cognitoUserId());
        }
        User user = User.builder()
                .email(request.email())
                .cognitoUserId(request.cognitoUserId())
                .displayName(request.displayName())
                .build();
        try {
            return UserResponse.from(save(user));
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("unique") || msg.contains("duplicate") || msg.contains("constraint")) {
                throw new UserAlreadyExistsException("email or cognitoUserId", "duplicate value");
            }
            throw e;
        }
    }

    /**
     * Returns the user with the given ID as a response DTO.
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    public UserResponse getUser(Long id) {
        return UserResponse.from(findById(id));
    }

    /**
     * Updates the display name of the user with the given ID.
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating displayName for user {}", id);
        UserEntity entity = findEntityById(id);
        entity.setDisplayName(request.displayName());
        return UserResponse.from(userMapper.toDomain(userRepository.save(entity)));
    }

    /**
     * Hard-deletes the user with the given ID. All child entities are cascade-deleted.
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    public void deleteUser(Long id) {
        log.info("Deleting user {}", id);
        UserEntity entity = findEntityById(id);
        userRepository.delete(entity);
    }

    /**
     * Adds {@code amount} to the user's {@code totalCoins} and {@code lifetimeCoinsEarned},
     * then persists the entity.
     */
    @Transactional
    public void addCoins(UserEntity user, int amount) {
        log.info("Adding {} coins to user {}", amount, user.getId());
        int before = user.getTotalCoins();
        user.setTotalCoins(before + amount);
        user.setLifetimeCoinsEarned(user.getLifetimeCoinsEarned() + amount);
        log.debug("User {} total coins: {} -> {}", user.getId(), before, user.getTotalCoins());
        userRepository.save(user);
    }
}
