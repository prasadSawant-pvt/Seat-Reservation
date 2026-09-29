package com.paytmmoney.seats.repository;

import com.paytmmoney.seats.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShowRepository extends JpaRepository<Show, String> {
}
