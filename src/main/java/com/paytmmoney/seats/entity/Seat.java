package com.paytmmoney.seats.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seats")
public class Seat {

    public enum Status {
        available,
        held,
        confirmed
    }

    @EmbeddedId
    private SeatId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.available;

    @Column(name = "held_by")
    private UUID heldBy;

    @Column(name = "held_until")
    private Instant heldUntil;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Seat() {
    }

    public Seat(java.util.UUID showId, String seatLabel) {
        this.id = new SeatId(showId, seatLabel);
    }

    public SeatId getId() {
        return id;
    }

    public void setId(SeatId id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public UUID getHeldBy() {
        return heldBy;
    }

    public void setHeldBy(UUID heldBy) {
        this.heldBy = heldBy;
    }

    public Instant getHeldUntil() {
        return heldUntil;
    }

    public void setHeldUntil(Instant heldUntil) {
        this.heldUntil = heldUntil;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public void setReservationId(UUID reservationId) {
        this.reservationId = reservationId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
