package com.transport.backend.dto;

public record BreakdownResponseDto(
        String busId,
        String previousStatus,
        String currentStatus,
        boolean hadActiveTrip,
        String cancelledTripId,
        int strandedPassengers,
        String strandedStopId,
        String strandedStopName,
        String reliefDemandRequestId,
        String message
) {}
