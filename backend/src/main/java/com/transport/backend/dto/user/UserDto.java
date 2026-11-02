package com.transport.backend.dto.user;

import com.transport.backend.model.Role;
import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String fullName,
        String email,
        Role role,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
