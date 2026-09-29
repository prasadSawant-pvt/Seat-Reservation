package com.paytmmoney.seats.controller;

import com.paytmmoney.seats.auth.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/auth/token")
    public ResponseEntity<Map<String, String>> generateToken(@RequestBody Map<String, String> request) {
        String userIdStr = request.get("user_id");
        if (userIdStr == null || userIdStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "user_id is required"));
        }

        try {
            UUID userId = UUID.fromString(userIdStr);
            String token = jwtUtil.generateToken(userId);
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user_id", userId.toString(),
                    "type", "Bearer"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user_id format"));
        }
    }
}
