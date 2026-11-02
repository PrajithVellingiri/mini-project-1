package com.transport.backend.dto;

import java.util.List;

public record RouteResponseDto(
        boolean reachable,
        double travelTimeMinutes,
        double distanceKm,
        double executionTimeMicros,
        List<StopDto> stops,
        List<String> stopIds,
        String vehicleTypeUsed
) {
    public RouteResponseDto(boolean reachable, double travelTimeMinutes, double distanceKm, List<StopDto> stops, List<String> stopIds, String vehicleTypeUsed) {
        this(reachable, travelTimeMinutes, distanceKm, 0.0, stops, stopIds, vehicleTypeUsed);
    }
}
