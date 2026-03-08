package repository.service;

import model.domain.User;
import org.springframework.stereotype.Service;
import repository.api.UserRepository;
import repository.mapper.UserMapper;

@Service
public class UserStore {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserStore(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    public User save(User user) {
        return userMapper.toDomain(userRepository.save(userMapper.fromDomain(user)));
    }
}
