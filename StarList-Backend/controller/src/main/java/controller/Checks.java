package controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/check")
public class Checks {
    private static final Logger logger = LoggerFactory.getLogger(Checks.class);

    @GetMapping("/health")
    public String health(){
        logger.info("Health check endpoint called");
        return "OK";
    }
}
