package com.paytmmoney.seats.repository;

import com.paytmmoney.seats.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, com.paytmmoney.seats.entity.ReservationSeatId> {
    List<ReservationSeat> findById_ReservationId(String reservationId);
}
