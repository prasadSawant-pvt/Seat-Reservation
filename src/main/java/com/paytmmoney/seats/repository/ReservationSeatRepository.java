package com.paytmmoney.seats.repository;

import com.paytmmoney.seats.entity.ReservationSeat;
import com.paytmmoney.seats.entity.ReservationSeatId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, ReservationSeatId> {
    List<ReservationSeat> findById_ReservationId(UUID reservationId);
}
