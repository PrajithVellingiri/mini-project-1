package com.transport.backend.dto;

public record AnalyticsResponseDto(
        PassengerAnalyticsDto passengers,
        FleetAnalyticsDto fleet,
        TripAnalyticsDto trips,
        DemandAnalyticsDto demand,
        AlgorithmBenchmarkDto telemetry
) {}
