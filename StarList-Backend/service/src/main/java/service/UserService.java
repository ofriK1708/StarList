package service;

import model.domain.User;
import org.springframework.stereotype.Service;
import repository.api.UserRepository;
import repository.entity.UserEntity;
import repository.mapper.UserMapper;
import service.exceptions.UserNotFoundException;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    /**
     * Finds a user by ID and maps it to the domain model.
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    public User findById(Long id) {
        return userMapper.toDomain(userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id)));
    }

    /**
     * Finds a user by ID and returns the raw JPA entity.
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    public UserEntity findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User save(User user) {
        return userMapper.toDomain(userRepository.save(userMapper.fromDomain(user)));
    }
}
