package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"app", "controller", "service", "repository"})
@EnableScheduling
@EntityScan("repository.entity")
@EnableJpaRepositories("repository.api")
public class StarListApp {
    public static void main(String[] args) {
        SpringApplication.run(StarListApp.class, args);
    }
}
