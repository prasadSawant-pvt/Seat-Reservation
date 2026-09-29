package com.paytmmoney.seats.repository;

import com.paytmmoney.seats.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByUserIdAndShowId(UUID userId, UUID showId);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.userId = :userId AND r.showId = :showId AND r.status = 'confirmed'")
    long countConfirmedByUserAndShow(@Param("userId") UUID userId, @Param("showId") UUID showId);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.userId = :userId AND r.showId = :showId AND r.status = 'held'")
    long countHeldByUserAndShow(@Param("userId") UUID userId, @Param("showId") UUID showId);
}
