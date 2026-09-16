package com.nevtan.qa.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Steady fake traffic, so the log panel and any charts always have something to
 * show without anyone having to hit an endpoint first. Roughly 70% info,
 * 20% debug, 8% warn, 2% error, plus a deliberate error spike every 5 minutes.
 */
@Component
public class BackgroundTraffic {

    private static final Logger log = LoggerFactory.getLogger(BackgroundTraffic.class);

    private static final List<String> SKUS = List.of("NV-1001", "NV-2049", "NV-3312", "NV-7744");
    private static final List<String> REGIONS = List.of("ap-south-1", "eu-west-1", "us-east-1");

    private final AtomicLong orderId = new AtomicLong(9000);

    @Scheduled(fixedRate = 2000)
    public void traffic() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long id = orderId.incrementAndGet();
        String sku = SKUS.get(rnd.nextInt(SKUS.size()));
        String region = REGIONS.get(rnd.nextInt(REGIONS.size()));
        int roll = rnd.nextInt(100);

        if (roll < 20) {
            log.debug("order={} sku={} region={} cache=miss lookupMs={}", id, sku, region, rnd.nextInt(2, 40));
        } else if (roll < 90) {
            log.info("order={} sku={} region={} status=CONFIRMED amount={}.00 INR latencyMs={}",
                    id, sku, region, rnd.nextInt(199, 4999), rnd.nextInt(20, 300));
        } else if (roll < 98) {
            log.warn("order={} sku={} region={} status=RETRYING attempt={} reason=gateway_timeout",
                    id, sku, region, rnd.nextInt(2, 5));
        } else {
            log.error("order={} sku={} region={} status=FAILED reason=card_declined code=51", id, sku, region);
        }
    }

    /** A predictable error spike, so a chart or an alert has something to catch. */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void errorSpike() {
        log.warn("[SPIKE] payments-db connection pool exhausted, 20 failures follow");
        for (int i = 1; i <= 20; i++) {
            log.error("[SPIKE] order={} status=FAILED reason=pool_timeout waitedMs={}",
                    orderId.incrementAndGet(), 5000 + i);
        }
        log.info("[SPIKE] pool recovered, back to normal traffic");
    }

    /** One heartbeat a minute, an easy anchor when scrolling back through history. */
    @Scheduled(fixedRate = 60000)
    public void heartbeat() {
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        log.info("[HEARTBEAT] orders-api alive, ordersProcessed={} heapUsedMb={}", orderId.get(), usedMb);
    }
}
