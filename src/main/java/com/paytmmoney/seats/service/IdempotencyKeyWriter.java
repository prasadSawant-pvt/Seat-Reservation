package com.paytmmoney.seats.service;

import com.paytmmoney.seats.entity.IdempotencyKey;
import com.paytmmoney.seats.repository.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IdempotencyKeyWriter {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public IdempotencyKeyWriter(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyKey attemptInsert(String key, UUID userId, UUID showId, String fingerprint) {
        IdempotencyKey idempotencyKey = new IdempotencyKey(key, userId, showId);
        idempotencyKey.setRequestFingerprint(fingerprint);
        return idempotencyKeyRepository.saveAndFlush(idempotencyKey);
    }
}
