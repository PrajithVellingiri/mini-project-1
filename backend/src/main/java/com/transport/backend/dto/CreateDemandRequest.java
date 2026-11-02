package com.transport.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateDemandRequest(
        @NotBlank(message = "Pickup stop ID cannot be blank")
        String pickupStopId,

        @NotBlank(message = "Destination stop ID cannot be blank")
        String destinationStopId,

        @Min(value = 1, message = "Passenger count must be at least 1")
        int passengerCount
) {}
