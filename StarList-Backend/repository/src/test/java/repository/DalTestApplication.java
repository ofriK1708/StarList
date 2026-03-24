package repository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Minimal Spring Boot bootstrap for DAL integration tests.
 * Mirrors the EntityScan/EnableJpaRepositories config from StarListApp so that
 * @SpringBootTest can locate entities and repositories without the full app context.
 */
@SpringBootApplication
@EntityScan("repository.entity")
@EnableJpaRepositories("repository.api")
public class DalTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(DalTestApplication.class, args);
    }
}
