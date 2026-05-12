package repository.api;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.AiConversationEntity;

public interface AiConversationRepository extends JpaRepository<AiConversationEntity, Long> {

    /** Returns the most recent {@code pageable.getPageSize()} turns for a user, newest-first. */
    List<AiConversationEntity> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** Returns all conversation turns for a user, newest-first. */
    List<AiConversationEntity> findAllByUser_IdOrderByCreatedAtDesc(Long userId);
}
