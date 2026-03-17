package controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Checks {
    private static final Logger logger = LoggerFactory.getLogger(Checks.class);

    @GetMapping("/")
    public String health() {
        logger.info("Health check endpoint called");
        return "starList is alive and well!";
    }
}
