package controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.AchievementService;
import service.UserService;
import service.dto.AchievementResponse;

@RestController
@RequestMapping("/achievements")
public class AchievementController {

    private final AchievementService achievementService;
    private final UserService userService;

    public AchievementController(AchievementService achievementService, UserService userService) {
        this.achievementService = achievementService;
        this.userService = userService;
    }

    /** Returns all achievements for the authenticated user, each marked unlocked or locked. */
    @GetMapping
    public ResponseEntity<List<AchievementResponse>> getMyAchievements(@AuthenticationPrincipal Jwt jwt) {
        Long userId = userService.getUserByCognitoSub(jwt.getSubject()).id();
        return ResponseEntity.ok(achievementService.getUserAchievements(userId));
    }
}
