package com.paytmmoney.seats.controller;

import com.paytmmoney.seats.dto.ReserveSeatsRequest;
import com.paytmmoney.seats.dto.ReserveSeatsResponse;
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
        ReserveSeatsResponse response = reservationService.reserveSeats(showId, request);
        // Return 201 for new reservations, 200 for idempotent replays
        // The service determines this based on whether it's a new or existing reservation
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reservations/{id}/cancel")
    public ResponseEntity<Void> cancelReservation(@PathVariable UUID id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.ok().build();
    }
}
