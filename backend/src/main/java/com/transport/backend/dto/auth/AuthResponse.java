package com.transport.backend.dto.auth;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UserProfileDto user
) {
    public AuthResponse(String token, long expiresInMs, UserProfileDto user) {
        this(token, "Bearer", expiresInMs, user);
    }
}
