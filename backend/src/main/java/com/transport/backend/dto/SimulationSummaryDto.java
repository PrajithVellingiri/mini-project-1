package com.transport.backend.dto;

public record SimulationSummaryDto(
        int totalStops,
        int totalRoads,
        int totalBuses,
        int availableBuses,
        int waitingDemandBatches,
        int waitingPassengers,
        int allocatedPassengers,
        int activeTripsCount
) {}
