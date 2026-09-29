package com.paytmmoney.seats.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SeatId implements Serializable {

    private UUID showId;
    private String seatLabel;

    public SeatId() {
    }

    public SeatId(UUID showId, String seatLabel) {
        this.showId = showId;
        this.seatLabel = seatLabel;
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
        SeatId seatId = (SeatId) o;
        return Objects.equals(showId, seatId.showId) && Objects.equals(seatLabel, seatId.seatLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(showId, seatLabel);
    }
}
