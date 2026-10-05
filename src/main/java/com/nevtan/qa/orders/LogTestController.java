package com.nevtan.qa.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every QA case can be fired from the browser, so a case can be replayed while
 * watching the log panel instead of waiting for the background traffic to
 * happen to produce it.
 */
@RestController
@RequestMapping("/qa")
public class LogTestController {

    private static final Logger log = LoggerFactory.getLogger(LogTestController.class);

    private final LogScenarios scenarios;
    private final List<byte[]> ballast = new ArrayList<>();

    public LogTestController(LogScenarios scenarios) {
        this.scenarios = scenarios;
    }

    @GetMapping("/")
    public Map<String, String> index() {
        Map<String, String> routes = new LinkedHashMap<>();
        routes.put("GET /health", "liveness, logs one INFO line");
        routes.put("GET /qa/levels", "QA-01 debug, info, warn, error in one shot");
        routes.put("GET /qa/stacktrace", "QA-02 nested exception with cause");
        routes.put("GET /qa/longline", "QA-03 one ~7KB line");
        routes.put("GET /qa/unicode", "QA-04 devanagari, CJK, arabic, emoji");
        routes.put("GET /qa/ansi", "QA-05 ANSI colour escapes");
        routes.put("GET /qa/traps", "QA-06 INFO lines containing the word ERROR");
        routes.put("GET /qa/burst?n=500", "QA-07 n numbered lines as fast as possible");
        routes.put("GET /qa/json", "QA-08 single line JSON payload");
        routes.put("GET /qa/whitespace", "QA-09 blank lines and tabs");
        routes.put("GET /qa/block", "QA-10 multi line ascii table");
        routes.put("GET /qa/backdated", "QA-11 line stamped two hours ago");
        routes.put("GET /qa/secrets", "QA-12 token and password shaped strings");
        routes.put("GET /qa/streams", "QA-13 raw stdout and stderr");
        routes.put("GET /qa/500", "QA-14 unhandled exception, 500 response");
        routes.put("GET /qa/slow?ms=30000", "QA-15 silence, then one line (idle tail test)");
        routes.put("GET /qa/oom", "QA-16 allocate until the container is OOM killed");
        routes.put("GET /qa/crash", "QA-17 exit(1) immediately, restart loop");
        routes.put("GET /qa/cpu?threads=4&seconds=120", "QA-18 burn CPU on n threads (100% per thread)");
        routes.put("GET /qa/all", "fire QA-01 to QA-13 in order");
        return routes;
    }

    @GetMapping("/levels")
    public String levels() {
        scenarios.levels();
        return "QA-01 fired";
    }

    @GetMapping("/stacktrace")
    public String stackTrace() {
        scenarios.stackTrace();
        return "QA-02 fired";
    }

    @GetMapping("/longline")
    public String longLine() {
        scenarios.longLine();
        return "QA-03 fired";
    }

    @GetMapping("/unicode")
    public String unicode() {
        scenarios.unicode();
        return "QA-04 fired";
    }

    @GetMapping("/ansi")
    public String ansi() {
        scenarios.ansi();
        return "QA-05 fired";
    }

    @GetMapping("/traps")
    public String traps() {
        scenarios.levelTraps();
        return "QA-06 fired";
    }

    @GetMapping("/burst")
    public String burst(@RequestParam(defaultValue = "500") int n) {
        scenarios.burst(Math.min(n, 20000));
        return "QA-07 fired with n=" + n;
    }

    @GetMapping("/json")
    public String json() {
        scenarios.json();
        return "QA-08 fired";
    }

    @GetMapping("/whitespace")
    public String whitespace() {
        scenarios.whitespace();
        return "QA-09 fired";
    }

    @GetMapping("/block")
    public String block() {
        scenarios.multiLineBlock();
        return "QA-10 fired";
    }

    @GetMapping("/backdated")
    public String backdated() {
        scenarios.backdated();
        return "QA-11 fired";
    }

    @GetMapping("/secrets")
    public String secrets() {
        scenarios.secrets();
        return "QA-12 fired";
    }

    @GetMapping("/streams")
    public String streams() {
        scenarios.stderr();
        return "QA-13 fired";
    }

    /** QA-14 - the exception escapes the controller, so Spring logs it, not us. */
    @GetMapping("/500")
    public String boom() {
        log.info("[QA-14 UNHANDLED] about to throw out of the controller");
        throw new IllegalArgumentException("[QA-14 UNHANDLED] orderId 'abc' is not a number");
    }

    /** QA-15 - a long quiet gap, to see whether live tail survives an idle period. */
    @GetMapping("/slow")
    public String slow(@RequestParam(defaultValue = "30000") long ms) throws InterruptedException {
        log.info("[QA-15 IDLE] going quiet for {}ms, next line should still arrive live", ms);
        Thread.sleep(Math.min(ms, 120000));
        log.info("[QA-15 IDLE] back after {}ms of silence", ms);
        return "QA-15 fired";
    }

    /** QA-16 - grow the heap until the JVM or the container kills the process. */
    @GetMapping("/oom")
    public String oom() {
        log.warn("[QA-16 OOM] allocating 8MB at a time, expect an OutOfMemoryError or exit 137");
        int mb = 0;
        while (true) {
            ballast.add(new byte[8 * 1024 * 1024]);
            mb += 8;
            log.error("[QA-16 OOM] heap ballast now {}MB", mb);
        }
    }

    /** QA-17 - hard exit, so the container restarts and logs must survive it. */
    @GetMapping("/crash")
    public String crash() {
        log.error("[QA-17 CRASH] calling System.exit(1) now, container should restart");
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            System.exit(1);
        }).start();
        return "QA-17 fired, exiting in 200ms";
    }

    /** QA-18 - pin n cores for a while, to push the container past 100% CPU of its plan. */
    @GetMapping("/cpu")
    public String cpu(@RequestParam(defaultValue = "4") int threads,
                      @RequestParam(defaultValue = "120") long seconds) {
        int n = Math.max(1, Math.min(threads, 16));
        long secs = Math.max(1, Math.min(seconds, 600));
        long deadline = System.currentTimeMillis() + secs * 1000;
        log.warn("[QA-18 CPU] burning {} threads for {}s, expect ~{}% CPU", n, secs, n * 100);
        for (int i = 0; i < n; i++) {
            Thread t = new Thread(() -> {
                double x = 0;
                while (System.currentTimeMillis() < deadline) {
                    x += Math.sqrt(x + 1);
                }
                log.info("[QA-18 CPU] thread {} done ({})", Thread.currentThread().getName(), x > 0);
            }, "qa18-cpu-" + i);
            t.setDaemon(true);
            t.start();
        }
        return "QA-18 fired, " + n + " threads for " + secs + "s";
    }

    @GetMapping("/all")
    public String all() {
        log.info("[QA-ALL] firing QA-01 to QA-13 in order");
        scenarios.levels();
        scenarios.stackTrace();
        scenarios.longLine();
        scenarios.unicode();
        scenarios.ansi();
        scenarios.levelTraps();
        scenarios.burst(50);
        scenarios.json();
        scenarios.whitespace();
        scenarios.multiLineBlock();
        scenarios.backdated();
        scenarios.secrets();
        scenarios.stderr();
        log.info("[QA-ALL] done");
        return "QA-01..QA-13 fired";
    }
}
