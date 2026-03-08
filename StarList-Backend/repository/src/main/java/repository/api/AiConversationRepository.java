package repository.api;

import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.AiConversationEntity;

public interface AiConversationRepository extends JpaRepository<AiConversationEntity, Long> {}
