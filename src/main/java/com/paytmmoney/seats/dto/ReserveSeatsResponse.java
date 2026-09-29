package com.paytmmoney.seats.dto;

import java.time.Instant;
import java.util.List;

public class ReserveSeatsResponse {

    private String reservationId;
    private String showId;
    private String userId;
    private List<String> seats;
    private Long amountPaise;
    private String status;
    private Instant createdAt;

    public ReserveSeatsResponse() {
    }

    public ReserveSeatsResponse(String reservationId, String showId, String userId, List<String> seats, Long amountPaise, String status, Instant createdAt) {
        this.reservationId = reservationId;
        this.showId = showId;
        this.userId = userId;
        this.seats = seats;
        this.amountPaise = amountPaise;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getShowId() {
        return showId;
    }

    public void setShowId(String showId) {
        this.showId = showId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getSeats() {
        return seats;
    }

    public void setSeats(List<String> seats) {
        this.seats = seats;
    }

    public Long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(Long amountPaise) {
        this.amountPaise = amountPaise;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
