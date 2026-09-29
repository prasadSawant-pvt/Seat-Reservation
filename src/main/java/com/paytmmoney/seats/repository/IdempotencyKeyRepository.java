package com.paytmmoney.seats.repository;

import com.paytmmoney.seats.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
    Optional<IdempotencyKey> findByKeyAndUserId(String key, String userId);
    IdempotencyKey findByKeyAndUserIdAndShowId(String key, UUID userId, UUID showId);
}
