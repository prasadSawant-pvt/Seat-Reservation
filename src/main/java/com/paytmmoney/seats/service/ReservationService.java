package com.paytmmoney.seats.service;

import com.paytmmoney.seats.auth.UserContext;
import com.paytmmoney.seats.dto.ReserveSeatsRequest;
import com.paytmmoney.seats.dto.ReserveSeatsResponse;
import com.paytmmoney.seats.dto.ReservationResult;
import com.paytmmoney.seats.entity.IdempotencyKey;
import com.paytmmoney.seats.entity.Reservation;
import com.paytmmoney.seats.entity.ReservationSeat;
import com.paytmmoney.seats.metrics.ReservationMetrics;
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
    private final ReservationMetrics metrics;

    public ReservationService(ReservationRepository reservationRepository, SeatRepository seatRepository,
                              ShowRepository showRepository, IdempotencyKeyRepository idempotencyKeyRepository,
                              ReservationSeatRepository reservationSeatRepository, JdbcTemplate jdbcTemplate,
                              ReservationMetrics metrics) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.metrics = metrics;
    }

    @Transactional
    public ReservationResult reserveSeats(UUID showId, ReserveSeatsRequest request) {
        UUID userId = UserContext.getUserId();

        // Validate show exists
        var show = showRepository.findById(showId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found"));

        // Validate unique seats
        Set<String> uniqueSeats = new HashSet<>(request.getSeats());
        if (uniqueSeats.size() != request.getSeats().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate seat labels in request");
        }

        // Sort seats to prevent deadlocks - consistent ordering ensures
        // concurrent requests lock seats in the same order, avoiding circular wait
        List<String> sortedSeats = new ArrayList<>(uniqueSeats);
        Collections.sort(sortedSeats);

        // Compute request fingerprint from sorted seat labels
        String fingerprint = computeFingerprint(sortedSeats);

        // Atomic idempotency check - check if key exists first
        IdempotencyKey existingKey = idempotencyKeyRepository.findByKeyAndUserIdAndShowId(
                request.getIdempotencyKey(), userId, showId);
        
        if (existingKey != null) {
            // Key already exists - check if it's a replay or conflict
            if (fingerprint.equals(existingKey.getRequestFingerprint())) {
                // Same request - return original reservation (idempotent replay)
                if (existingKey.getReservationId() != null) {
                    Reservation existingReservation = reservationRepository.findById(existingKey.getReservationId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Reservation not found"));
                    return new ReservationResult(buildResponse(existingReservation), false);
                } else {
                    // Key exists but reservation not yet set - race condition, treat as conflict
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key in use");
                }
            } else {
                // Different request with same key - conflict
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key already used with different request");
            }
        }
        
        // Key doesn't exist - create new idempotency record
        IdempotencyKey idempotencyKey = new IdempotencyKey(request.getIdempotencyKey(), userId, showId);
        idempotencyKey.setRequestFingerprint(fingerprint);
        idempotencyKeyRepository.save(idempotencyKey);

        // Per-user limit check - must be inside transaction for concurrency safety
        long currentConfirmedCount = reservationRepository.countConfirmedByUserAndShow(userId, showId);
        long currentHeldCount = reservationRepository.countHeldByUserAndShow(userId, showId);
        long totalCurrent = currentConfirmedCount + currentHeldCount;
        if (totalCurrent + sortedSeats.size() > show.getPerUserLimit()) {
            metrics.incrementDeclinedPerUserLimit();
            throw new ResponseStatusException(HttpStatus.CONFLICT, "per_user_limit");
        }

        // Calculate amount
        Long amountPaise = show.getPricePaise() * sortedSeats.size();

        // Create reservation
        Reservation reservation = new Reservation(showId, userId, amountPaise);
        reservation.setStatus(Reservation.Status.held);
        Reservation savedReservation = reservationRepository.save(reservation);

        // Atomic seat reservation using PostgreSQL function
        String[] seatArray = sortedSeats.toArray(new String[0]);
        Integer rowsUpdated = jdbcTemplate.queryForObject(
                "SELECT reserve_seats(?, ?, ?, ?, ?)",
                Integer.class,
                showId,
                seatArray,
                userId,
                savedReservation.getId(),
                Timestamp.from(Instant.now().plusSeconds(300)) // 5 minute hold
        );

        if (rowsUpdated == null || rowsUpdated != sortedSeats.size()) {
            // Not all seats were available - rollback entire transaction
            metrics.incrementDeclinedSeatTaken();
            throw new ResponseStatusException(HttpStatus.CONFLICT, "seat_taken");
        }

        // Create reservation seats link records
        for (String seatLabel : sortedSeats) {
            ReservationSeat reservationSeat = new ReservationSeat(savedReservation.getId(), showId, seatLabel);
            reservationSeatRepository.save(reservationSeat);
        }

        // Update idempotency key with reservation_id
        idempotencyKey.setReservationId(savedReservation.getId());
        idempotencyKeyRepository.save(idempotencyKey);

        metrics.incrementConfirmed();
        updateSeatsAvailableMetric();
        return new ReservationResult(buildResponse(savedReservation), true);
    }

    private String computeFingerprint(List<String> seats) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String joined = String.join(",", seats);
            byte[] hash = md.digest(joined.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
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

    private void updateSeatsAvailableMetric() {
        long availableCount = seatRepository.countAvailableSeats();
        metrics.setSeatsAvailable(availableCount);
    }
}
