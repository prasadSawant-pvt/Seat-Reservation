package com.paytmmoney.seats.controller;

import com.paytmmoney.seats.dto.ReserveSeatsRequest;
import com.paytmmoney.seats.dto.ReserveSeatsResponse;
import com.paytmmoney.seats.dto.ReservationResult;
import com.paytmmoney.seats.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/shows/{showId}/reserve")
    public ResponseEntity<ReserveSeatsResponse> reserveSeats(
            @PathVariable UUID showId,
            @Valid @RequestBody ReserveSeatsRequest request) {
        ReservationResult result = reservationService.reserveSeats(showId, request);
        // Return 201 for new reservations, 200 for idempotent replays
        if (result.isNew()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result.getResponse());
        } else {
            return ResponseEntity.ok(result.getResponse());
        }
    }
}
