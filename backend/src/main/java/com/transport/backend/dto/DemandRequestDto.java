package com.transport.backend.dto;

public record DemandRequestDto(
        String requestId,
        String pickupStopId,
        String pickupStopName,
        String destinationStopId,
        String destinationStopName,
        int passengerCount,
        long originalArrivalOrder
) {}
