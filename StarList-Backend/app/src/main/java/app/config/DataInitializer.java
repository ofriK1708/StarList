package app.config;

import model.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import service.UserService;

@Slf4j
@Component
@Profile("test")
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;

    public DataInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (int i = 1; i <= 5; i++) {
            User user = User.builder()
                    .email("user" + i + "@test.com")
                    .cognitoUserId("cognito-test-user-" + i)
                    .displayName("Test User " + i)
                    .build();
            User saved = userService.save(user);
            log.info("Seeded user id={} email={}", saved.getId(), saved.getEmail());
        }
    }
}
