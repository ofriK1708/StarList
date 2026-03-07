package model.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.enums.ConversationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConversation {

    private Long id;
    private Long userId;
    private ConversationType conversationType;
    private String userMessage;
    private String aiResponse;
    private Integer tasksCreated;
    private Instant createdAt;
}
