package com.paytmmoney.seats.service;

import com.paytmmoney.seats.auth.UserContext;
import com.paytmmoney.seats.dto.ReserveSeatsRequest;
import com.paytmmoney.seats.dto.ReserveSeatsResponse;
import com.paytmmoney.seats.entity.IdempotencyKey;
import com.paytmmoney.seats.entity.Reservation;
import com.paytmmoney.seats.entity.ReservationSeat;
import com.paytmmoney.seats.repository.IdempotencyKeyRepository;
import com.paytmmoney.seats.repository.ReservationRepository;
import com.paytmmoney.seats.repository.ReservationSeatRepository;
import com.paytmmoney.seats.repository.SeatRepository;
import com.paytmmoney.seats.repository.ShowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final JdbcTemplate jdbcTemplate;

    public ReservationService(ReservationRepository reservationRepository, SeatRepository seatRepository,
                              ShowRepository showRepository, IdempotencyKeyRepository idempotencyKeyRepository,
                              ReservationSeatRepository reservationSeatRepository, JdbcTemplate jdbcTemplate) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ReserveSeatsResponse reserveSeats(UUID showId, ReserveSeatsRequest request) {
        UUID userId = UserContext.getUserId();

        // Validate show exists
        if (!showRepository.existsById(showId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found");
        }

        // Validate unique seats
        Set<String> uniqueSeats = new HashSet<>(request.getSeats());
        if (uniqueSeats.size() != request.getSeats().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate seat labels in request");
        }

        // Check idempotency
        IdempotencyKey existingKey = idempotencyKeyRepository.findByKeyAndUserIdAndShowId(
                request.getIdempotencyKey(), userId, showId);
        if (existingKey != null) {
            // Return existing reservation
            Reservation existingReservation = reservationRepository.findById(existingKey.getReservationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Reservation not found"));
            return buildResponse(existingReservation);
        }

        // Check per-user limit
        long currentConfirmedCount = reservationRepository.countConfirmedByUserAndShow(userId, showId);
        long currentHeldCount = reservationRepository.countHeldByUserAndShow(userId, showId);
        long totalCurrent = currentConfirmedCount + currentHeldCount;
        var show = showRepository.findById(showId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found"));
        if (totalCurrent + request.getSeats().size() > show.getPerUserLimit()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Per-user limit exceeded");
        }

        // Calculate amount before creating reservation
        Long amountPaise = show.getPricePaise() * uniqueSeats.size();

        // Create reservation with calculated amount
        Reservation reservation = new Reservation(showId, userId, amountPaise);
        reservation.setStatus(Reservation.Status.held);
        Reservation savedReservation = reservationRepository.save(reservation);

        // Use PostgreSQL function for atomic seat reservation
        String[] seatArray = uniqueSeats.toArray(new String[0]);
        Integer rowsUpdated = jdbcTemplate.queryForObject(
                "SELECT reserve_seats(?, ?, ?, ?, ?)",
                Integer.class,
                showId,
                seatArray,
                userId,
                savedReservation.getId(),
                Timestamp.from(Instant.now().plusSeconds(300)) // 5 minute hold
        );

        if (rowsUpdated == null || rowsUpdated != uniqueSeats.size()) {
            // Not all seats were available - rollback
            throw new ResponseStatusException(HttpStatus.CONFLICT, "One or more seats are not available");
        }

        // Create reservation seats
        for (String seatLabel : uniqueSeats) {
            ReservationSeat reservationSeat = new ReservationSeat(savedReservation.getId(), showId, seatLabel);
            reservationSeatRepository.save(reservationSeat);
        }

        // Store idempotency key
        IdempotencyKey idempotencyKey = new IdempotencyKey(request.getIdempotencyKey(), userId, showId);
        idempotencyKey.setReservationId(savedReservation.getId());
        idempotencyKeyRepository.save(idempotencyKey);

        return buildResponse(savedReservation);
    }

    private ReserveSeatsResponse buildResponse(Reservation reservation) {
        List<ReservationSeat> reservationSeats = reservationSeatRepository.findById_ReservationId(reservation.getId());
        List<String> seats = reservationSeats.stream()
                .map(rs -> rs.getId().getSeatLabel())
                .collect(Collectors.toList());

        return new ReserveSeatsResponse(
                reservation.getId().toString(),
                reservation.getShowId().toString(),
                reservation.getUserId().toString(),
                seats,
                reservation.getAmountPaise(),
                reservation.getStatus().name(),
                reservation.getCreatedAt()
        );
    }

    @Transactional
    public void cancelReservation(UUID reservationId) {
        UUID userId = UserContext.getUserId();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

        // Only the owner can cancel
        if (!reservation.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only cancel your own reservations");
        }

        // Use PostgreSQL function to cancel reservation
        jdbcTemplate.update("SELECT cancel_reservation(?, ?)", reservationId, userId);
    }
}
