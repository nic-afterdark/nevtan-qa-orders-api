package com.nevtan.qa.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @GetMapping("/")
    public Map<String, String> root() {
        log.info("GET / - orders-api is up, QA routes are listed at /qa/");
        return Map.of("app", "orders-api", "qaRoutes", "/qa/", "health", "/health");
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        log.debug("GET /health - ok");
        return Map.of("status", "UP");
    }
}
