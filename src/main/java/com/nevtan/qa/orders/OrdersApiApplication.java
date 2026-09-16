package com.nevtan.qa.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrdersApiApplication {

    private static final Logger log = LoggerFactory.getLogger(OrdersApiApplication.class);

    public static void main(String[] args) {
        log.info("[BOOT] orders-api starting, pid={}", ProcessHandle.current().pid());
        SpringApplication.run(OrdersApiApplication.class, args);
        log.info("[BOOT] orders-api ready on port {}, QA routes at /qa/",
                System.getenv().getOrDefault("PORT", "8080"));
    }
}
