package controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.UserService;
import service.dto.CreateUserRequest;
import service.dto.UpdateUserRequest;
import service.dto.UserResponse;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Creates a new backend user. Called once after Cognito sign-up confirmation. */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Returns the backend user record for the given Cognito sub. */
    @GetMapping("/cognito/{sub}")
    public ResponseEntity<UserResponse> getUserByCognitoSub(@PathVariable String sub) {
        UserResponse response = userService.getUserByCognitoSub(sub);
        return ResponseEntity.ok(response);
    }

    /** Updates the display name of the currently authenticated user. */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserRequest request) {
        Long userId = userService.getUserByCognitoSub(jwt.getSubject()).id();
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    /** Deletes the currently authenticated user and all associated data. */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal Jwt jwt) {
        Long userId = userService.getUserByCognitoSub(jwt.getSubject()).id();
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
