package repository.mapper;

import model.domain.Habit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import repository.entity.HabitEntity;
import repository.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface HabitMapper {

    @Mapping(target = "userId", source = "entity.user.id")
    Habit toDomain(HabitEntity entity);

    @Mapping(target = "user", source = "user")
    @Mapping(target = "completions", ignore = true)
    @Mapping(target = "id", source = "habit.id")
    @Mapping(target = "createdAt", source = "habit.createdAt")
    HabitEntity fromDomain(Habit habit, UserEntity user);
}
