package com.transport.backend.dto;

public record FleetAnalyticsDto(
        int totalBuses,
        int availableBuses,
        int inTransitBuses,
        int maintenanceBuses,
        int outOfServiceBuses,
        double fleetUtilizationPercent,
        int totalFleetCapacity,
        int occupiedSeats,
        double seatUtilizationPercent
) {}
