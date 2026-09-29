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

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'HELD', s.heldBy = :userId, s.heldUntil = :heldUntil WHERE s.id.showId = :showId AND s.id.seatLabel = :seatLabel AND s.status = 'AVAILABLE'")
    int holdSeat(@Param("showId") UUID showId, @Param("seatLabel") String seatLabel, @Param("userId") String userId, @Param("heldUntil") java.time.Instant heldUntil);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'CONFIRMED', s.reservationId = :reservationId WHERE s.id.showId = :showId AND s.id.seatLabel = :seatLabel AND s.status = 'HELD' AND s.heldBy = :userId")
    int confirmSeat(@Param("showId") UUID showId, @Param("seatLabel") String seatLabel, @Param("userId") String userId, @Param("reservationId") String reservationId);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.heldBy = NULL, s.heldUntil = NULL, s.reservationId = NULL WHERE s.id.showId = :showId AND s.id.seatLabel = :seatLabel AND s.status = 'HELD' AND s.heldBy = :userId")
    int releaseSeat(@Param("showId") UUID showId, @Param("seatLabel") String seatLabel, @Param("userId") String userId);

    long countById_ShowIdAndStatus(UUID showId, Seat.Status status);
}
