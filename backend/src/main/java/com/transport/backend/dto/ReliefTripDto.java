package com.transport.backend.dto;

import java.util.List;

public record ReliefTripDto(
        String tripId,
        String busId,
        String busType,
        String demandRequestId,
        List<String> routeStopIds,
        List<String> routeStopNames,
        double estimatedTravelTimeMinutes,
        String status,
        int passengerCount
) {}
