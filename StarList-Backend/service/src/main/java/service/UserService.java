package service;

import lombok.extern.slf4j.Slf4j;
import model.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.api.UserRepository;
import repository.entity.UserEntity;
import repository.mapper.UserMapper;
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
