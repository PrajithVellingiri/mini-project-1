package com.transport.backend.dto;

public record BusDto(
        String id,
        String type,
        int capacity,
        int passengerOccupancy,
        int availableSeats,
        String currentStopId,
        String currentStopName,
        String status
) {}
