package repository.api;

import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {}
