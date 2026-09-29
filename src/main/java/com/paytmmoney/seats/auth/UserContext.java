package com.paytmmoney.seats.auth;

import java.util.UUID;

public class UserContext {

    private static final ThreadLocal<UUID> currentUserId = new ThreadLocal<>();

    public static void setUserId(UUID userId) {
        currentUserId.set(userId);
    }

    public static UUID getUserId() {
        UUID userId = currentUserId.get();
        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return userId;
    }

    public static void clear() {
        currentUserId.remove();
    }
}
