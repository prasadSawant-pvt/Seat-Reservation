package com.paytmmoney.seats.dto;

public class ReservationResult {
    private final ReserveSeatsResponse response;
    private final boolean isNew;

    public ReservationResult(ReserveSeatsResponse response, boolean isNew) {
        this.response = response;
        this.isNew = isNew;
    }

    public ReserveSeatsResponse getResponse() {
        return response;
    }

    public boolean isNew() {
        return isNew;
    }
}
