package repository.api;

import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.UserEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(String email);

    boolean existsByCognitoUserId(UUID cognitoUserId);

    Optional<UserEntity> findByCognitoUserId(UUID cognitoUserId);
}
