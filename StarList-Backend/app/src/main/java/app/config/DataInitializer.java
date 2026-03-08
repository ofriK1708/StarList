package app.config;

import model.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import repository.service.UserStore;

@Component
@Profile("test")
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserStore userStore;

    public DataInitializer(UserStore userStore) {
        this.userStore = userStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (int i = 1; i <= 5; i++) {
            User user = User.builder()
                    .email("user" + i + "@test.com")
                    .cognitoUserId("cognito-test-user-" + i)
                    .displayName("Test User " + i)
                    .build();
            User saved = userStore.save(user);
            log.info("Seeded user id={} email={}", saved.getId(), saved.getEmail());
        }
    }
}
