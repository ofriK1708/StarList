package repository.mapper;

import model.domain.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import repository.entity.TaskEntity;
import repository.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "userId", source = "entity.user.id")
    @Mapping(target = "aiConversationId",
             expression = "java(entity.getAiConversation() != null ? entity.getAiConversation().getId() : null)")
    Task toDomain(TaskEntity entity);

    @Mapping(target = "user", source = "user")
    @Mapping(target = "aiConversation", ignore = true)
    @Mapping(target = "version", ignore = true) // version is only relevant to db and should not be touched by service
    @Mapping(target = "id", source = "task.id")
    @Mapping(target = "createdAt", source = "task.createdAt")
    TaskEntity fromDomain(Task task, UserEntity user);
}
