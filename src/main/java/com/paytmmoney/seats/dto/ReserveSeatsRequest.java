package com.paytmmoney.seats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class ReserveSeatsRequest {

    @NotEmpty(message = "seats cannot be empty")
    private List<String> seats;

    @NotBlank(message = "idempotency_key is required")
    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    public ReserveSeatsRequest() {
    }

    public ReserveSeatsRequest(List<String> seats, String idempotencyKey) {
        this.seats = seats;
        this.idempotencyKey = idempotencyKey;
    }

    public List<String> getSeats() {
        return seats;
    }

    public void setSeats(List<String> seats) {
        this.seats = seats;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
