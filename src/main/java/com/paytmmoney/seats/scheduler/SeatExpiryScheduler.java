package com.paytmmoney.seats.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SeatExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SeatExpiryScheduler.class);
    private static final String REQUEST_ID_MDC_KEY = "request_id";

    private final JdbcTemplate jdbcTemplate;

    public SeatExpiryScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void expireHeldSeats() {
        String requestId = "scheduler-expire-held-seats-" + System.currentTimeMillis();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);

        try {
            jdbcTemplate.execute("SELECT expire_held_seats()");
            log.info("Expired held seats successfully");
        } catch (Exception e) {
            log.error("Error expiring held seats", e);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
