package com.paytmmoney.seats.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ReservationMetrics {

    private final Counter reservationsConfirmed;
    private final Counter reservationsDeclinedSeatTaken;
    private final Counter reservationsDeclinedPerUserLimit;
    private final Counter reservationsDeclinedIdempotentReplay;
    private final AtomicLong seatsAvailable = new AtomicLong(0);

    public ReservationMetrics(MeterRegistry registry) {
        this.reservationsConfirmed = Counter.builder("reservations_confirmed_total")
                .description("Total number of confirmed reservations")
                .register(registry);

        this.reservationsDeclinedSeatTaken = Counter.builder("reservations_declined_total")
                .description("Total number of declined reservations")
                .tag("reason", "seat_taken")
                .register(registry);

        this.reservationsDeclinedPerUserLimit = Counter.builder("reservations_declined_total")
                .description("Total number of declined reservations")
                .tag("reason", "per_user_limit")
                .register(registry);

        this.reservationsDeclinedIdempotentReplay = Counter.builder("reservations_declined_total")
                .description("Total number of declined reservations")
                .tag("reason", "idempotent_replay")
                .register(registry);

        Gauge.builder("seats_available", seatsAvailable, AtomicLong::get)
                .description("Number of available seats (global)")
                .register(registry);
    }

    public void incrementConfirmed() {
        reservationsConfirmed.increment();
    }

    public void incrementDeclinedSeatTaken() {
        reservationsDeclinedSeatTaken.increment();
    }

    public void incrementDeclinedPerUserLimit() {
        reservationsDeclinedPerUserLimit.increment();
    }

    public void incrementDeclinedIdempotentReplay() {
        reservationsDeclinedIdempotentReplay.increment();
    }

    public void setSeatsAvailable(long count) {
        seatsAvailable.set(count);
    }
}
