package controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class Checks {

    @GetMapping("/")
    public String health() {
        log.info("Health check endpoint called");
        return "starList is alive and well!";
    }
}
