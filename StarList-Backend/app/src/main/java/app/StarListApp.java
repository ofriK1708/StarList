package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// NOTE: @EnableScheduling was removed along with HabitMissScheduler — habit lateness is now
// handled at completion time. Restore it here if a @Scheduled job is ever reintroduced.
@SpringBootApplication(scanBasePackages = {"app", "controller", "service", "repository"})
@EntityScan("repository.entity")
@EnableJpaRepositories("repository.api")
public class StarListApp {
    public static void main(String[] args) {
        SpringApplication.run(StarListApp.class, args);
    }
}
