package repository.mapper;

import model.domain.User;
import org.mapstruct.Mapper;
import repository.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toDomain(UserEntity entity);
    UserEntity fromDomain(User user);
}
