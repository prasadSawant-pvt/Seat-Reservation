package com.paytmmoney.seats.repository;

import com.paytmmoney.seats.entity.Seat;
import com.paytmmoney.seats.entity.SeatId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, SeatId> {

    List<Seat> findById_ShowId(UUID showId);

    @Query("SELECT s.id.seatLabel, s.status FROM Seat s WHERE s.id.showId = :showId ORDER BY s.id.seatLabel")
    List<Object[]> findSeatStatesByShowId(@Param("showId") UUID showId);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'held', s.heldBy = :userId, s.heldUntil = :heldUntil WHERE s.id.showId = :showId AND s.id.seatLabel = :seatLabel AND s.status = 'available'")
    int holdSeat(@Param("showId") UUID showId, @Param("seatLabel") String seatLabel, @Param("userId") UUID userId, @Param("heldUntil") java.time.Instant heldUntil);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'confirmed', s.reservationId = :reservationId WHERE s.id.showId = :showId AND s.id.seatLabel = :seatLabel AND s.status = 'held' AND s.heldBy = :userId")
    int confirmSeat(@Param("showId") UUID showId, @Param("seatLabel") String seatLabel, @Param("userId") UUID userId, @Param("reservationId") UUID reservationId);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'available', s.heldBy = NULL, s.heldUntil = NULL, s.reservationId = NULL WHERE s.id.showId = :showId AND s.id.seatLabel = :seatLabel AND s.status = 'held' AND s.heldBy = :userId")
    int releaseSeat(@Param("showId") UUID showId, @Param("seatLabel") String seatLabel, @Param("userId") UUID userId);

    long countById_ShowIdAndStatus(UUID showId, Seat.Status status);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.status = 'available'")
    long countAvailableSeats();
}
