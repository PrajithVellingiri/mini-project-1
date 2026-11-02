package com.transport.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record BusStatusUpdateRequest(
        @NotBlank(message = "Status cannot be blank")
        String status
) {}
