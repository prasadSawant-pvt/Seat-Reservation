package com.paytmmoney.seats.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ReservationSeatId implements Serializable {

    private UUID reservationId;
    private UUID showId;
    private String seatLabel;

    public ReservationSeatId() {
    }

    public ReservationSeatId(UUID reservationId, UUID showId, String seatLabel) {
        this.reservationId = reservationId;
        this.showId = showId;
        this.seatLabel = seatLabel;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public void setReservationId(UUID reservationId) {
        this.reservationId = reservationId;
    }

    public UUID getShowId() {
        return showId;
    }

    public void setShowId(UUID showId) {
        this.showId = showId;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public void setSeatLabel(String seatLabel) {
        this.seatLabel = seatLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReservationSeatId that = (ReservationSeatId) o;
        return Objects.equals(reservationId, that.reservationId) &&
                Objects.equals(showId, that.showId) &&
                Objects.equals(seatLabel, that.seatLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationId, showId, seatLabel);
    }
}
