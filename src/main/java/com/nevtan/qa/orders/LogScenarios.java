package com.nevtan.qa.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * One method per QA case. Each line is tagged with its case id so a line seen
 * in the NevTan log viewer can be traced back to the code that produced it.
 */
@Component
public class LogScenarios {

    private static final Logger log = LoggerFactory.getLogger(LogScenarios.class);

    /** QA-01 - all four levels, back to back, for the level filter buttons. */
    public void levels() {
        log.debug("[QA-01 LEVELS] debug line - cache lookup key=order:8821 hit=false");
        log.info("[QA-01 LEVELS] info line - order 8821 accepted, total=1499.00 INR");
        log.warn("[QA-01 LEVELS] warn line - payment gateway p99 latency 2140ms, above 1500ms budget");
        log.error("[QA-01 LEVELS] error line - order 8821 could not be settled, will retry");
    }

    /** QA-02 - a real multi-line stack trace. Every line must stay with its parent. */
    public void stackTrace() {
        try {
            settleOrder("8822");
        } catch (Exception e) {
            log.error("[QA-02 STACKTRACE] settlement failed for order 8822", e);
        }
    }

    private void settleOrder(String id) throws Exception {
        try {
            chargeCard(id);
        } catch (SQLException e) {
            throw new IllegalStateException("settlement aborted for order " + id, e);
        }
    }

    private void chargeCard(String id) throws SQLException {
        throw new SQLException("connection refused: payments-db:5432 (order " + id + ")");
    }

    /** QA-03 - one very long line, to see how the viewer wraps or truncates. */
    public void longLine() {
        StringBuilder sb = new StringBuilder("[QA-03 LONGLINE] payload dump: ");
        for (int i = 0; i < 200; i++) {
            sb.append("field").append(i).append("=value-").append(i).append("-abcdefghij;");
        }
        log.info(sb.toString());
    }

    /** QA-04 - non-latin text and emoji, to check encoding end to end. */
    public void unicode() {
        log.info("[QA-04 UNICODE] customer name: ग्राहक नाम - 顧客名 - العميل - Unicode");
        log.warn("[QA-04 UNICODE] status refund queued for order 8823 🚨 💸 ✅");
    }

    /** QA-05 - ANSI colour codes, which a naive viewer renders as garbage. */
    public void ansi() {
        String esc = "";
        log.info("[QA-05 ANSI] " + esc + "[31mthis text is wrapped in red escape codes" + esc
                + "[0m and this is not");
    }

    /**
     * QA-06 - level traps. These are INFO lines whose text contains the words
     * ERROR, WARN and FATAL. A viewer that guesses the level by grepping the
     * message instead of reading the real level will mis-classify them.
     */
    public void levelTraps() {
        log.info("[QA-06 TRAP] this INFO line contains the word ERROR on purpose");
        log.info("[QA-06 TRAP] retry succeeded after a previous WARN, nothing is wrong here");
        log.debug("[QA-06 TRAP] parsed the literal string FATAL from the upstream response body");
        log.error("[QA-06 TRAP] this really is an ERROR and should be the only red line of QA-06");
    }

    /** QA-07 - a burst, to test live tail, rate limits and dropped lines. */
    public void burst(int count) {
        log.info("[QA-07 BURST] begin burst of {} lines", count);
        for (int i = 1; i <= count; i++) {
            log.info("[QA-07 BURST] line {} of {} seq={}", i, count, i);
        }
        log.info("[QA-07 BURST] end burst of {} lines - expect exactly {} numbered lines", count, count);
    }

    /** QA-08 - single-line JSON, the shape most log shippers expect. */
    public void json() {
        log.info("[QA-08 JSON] {\"event\":\"order.created\",\"orderId\":8824,\"amount\":1499.00,"
                + "\"currency\":\"INR\",\"items\":[{\"sku\":\"NV-1\",\"qty\":2}],\"nested\":{\"a\":{\"b\":\"c\"}}}");
    }

    /** QA-09 - blank lines and tabs. */
    public void whitespace() {
        log.info("[QA-09 WHITESPACE] next three lines are blank or whitespace only");
        System.out.println();
        System.out.println("   ");
        System.out.println("\t\t");
        log.info("[QA-09 WHITESPACE] tabs\there\tand\tthere");
    }

    /** QA-10 - multi-line output that is not a stack trace. */
    public void multiLineBlock() {
        log.info("[QA-10 BLOCK] slow query report:\n"
                + "+------------+---------+--------+\n"
                + "| query      | calls   | ms     |\n"
                + "+------------+---------+--------+\n"
                + "| orders.all | 12043   | 2140   |\n"
                + "| users.byId | 98211   | 12     |\n"
                + "+------------+---------+--------+");
    }

    /** QA-11 - a line carrying its own old timestamp, to see what the viewer sorts on. */
    public void backdated() {
        Instant old = Instant.now().minus(2, ChronoUnit.HOURS);
        System.out.println(old + " INFO [QA-11 BACKDATED] this line claims to be two hours old, "
                + "but was written just now");
    }

    /** QA-12 - secret-shaped strings, to see whether anything redacts them. */
    public void secrets() {
        log.warn("[QA-12 SECRETS] outbound call with Authorization: Bearer "
                + "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.qa-test-token-not-real");
        log.warn("[QA-12 SECRETS] connecting to postgres://qa_user:hunter2@payments-db:5432/orders");
    }

    /** QA-13 - stderr, which some pipelines capture separately from stdout. */
    public void stderr() {
        System.err.println("[QA-13 STDERR] written straight to stderr, not through the logger");
        System.out.println("[QA-13 STDOUT] written straight to stdout, not through the logger");
    }
}
