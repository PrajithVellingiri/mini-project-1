package com.transport.backend.dto;

import jakarta.validation.constraints.Positive;

public record UpdateTravelTimeRequest(
        String fromStopId,
        String toStopId,
        @Positive(message = "Travel time must be strictly positive")
        Double travelTimeMinutes
) {}
