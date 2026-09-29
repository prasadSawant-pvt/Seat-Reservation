package com.paytmmoney.seats.entity;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "reservation_seats")
public class ReservationSeat {

    @EmbeddedId
    private ReservationSeatId id;

    public ReservationSeat() {
    }

    public ReservationSeat(UUID reservationId, UUID showId, String seatLabel) {
        this.id = new ReservationSeatId(reservationId, showId, seatLabel);
    }

    public ReservationSeatId getId() {
        return id;
    }

    public void setId(ReservationSeatId id) {
        this.id = id;
    }
}
