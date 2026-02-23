package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"app", "controller"})
public class StarListApp {
    static void main(String[] args) {
        SpringApplication.run(StarListApp.class, args);

    }
}
