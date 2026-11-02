package com.transport.backend.dto;

public record TripAnalyticsDto(
        int totalDispatchedTrips,
        int activeTripsCount,
        int completedTripsCount,
        int cancelledTripsCount,
        double averageEstimatedTripTimeMinutes,
        double averageTripDistanceKm
) {}
