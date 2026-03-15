package repository.mapper;

import model.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import repository.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toDomain(UserEntity entity);

    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "habits", ignore = true)
    @Mapping(target = "habitCompletions", ignore = true)
    @Mapping(target = "galaxyItems", ignore = true)
    @Mapping(target = "coinTransactions", ignore = true)
    @Mapping(target = "aiConversations", ignore = true)
    UserEntity fromDomain(User user);
}
