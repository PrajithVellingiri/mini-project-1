package com.transport.backend.dto;

public record CorridorDemandDto(
        String corridorKey,
        String pickupStopId,
        String pickupStopName,
        String destinationStopId,
        String destinationStopName,
        int waitingPassengers,
        int batchCount
) {}
