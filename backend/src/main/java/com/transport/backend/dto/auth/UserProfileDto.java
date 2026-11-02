package com.transport.backend.dto.auth;

import com.transport.backend.model.Role;

public record UserProfileDto(
        Long id,
        String fullName,
        String email,
        Role role,
        boolean enabled
) {}
