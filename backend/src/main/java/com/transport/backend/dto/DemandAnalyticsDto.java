package com.transport.backend.dto;

import java.util.List;

public record DemandAnalyticsDto(
        int totalWaitingBatches,
        int totalWaitingPassengers,
        List<CorridorDemandDto> topCorridors
) {}
