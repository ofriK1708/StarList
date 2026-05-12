package controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.AiService;
import service.UserService;
import service.dto.AiChatRequest;
import service.dto.AiChatResponse;
import service.dto.AiConversationHistoryResponse;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;
    private final UserService userService;

    public AiController(AiService aiService, UserService userService) {
        this.aiService = aiService;
        this.userService = userService;
    }

    /** Sends a message to the AI assistant and returns its response. */
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AiChatRequest request) {
        Long userId = userService.getUserByCognitoSub(jwt.getSubject()).id();
        return ResponseEntity.ok(aiService.chat(userId, request));
    }

    /** Returns a concise daily briefing: due-soon tasks, habits needing attention, and a motivational note. */
    @GetMapping("/briefing")
    public ResponseEntity<AiChatResponse> getDailyBriefing(@AuthenticationPrincipal Jwt jwt) {
        Long userId = userService.getUserByCognitoSub(jwt.getSubject()).id();
        return ResponseEntity.ok(aiService.getDailyBriefing(userId));
    }

    /** Returns the full conversation history for the authenticated user, newest-first. */
    @GetMapping("/history")
    public ResponseEntity<List<AiConversationHistoryResponse>> getHistory(
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = userService.getUserByCognitoSub(jwt.getSubject()).id();
        return ResponseEntity.ok(aiService.getHistory(userId));
    }
}
