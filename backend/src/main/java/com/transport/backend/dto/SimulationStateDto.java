package com.transport.backend.dto;

import java.util.List;

public record SimulationStateDto(
        SimulationSummaryDto summary,
        List<StopDto> stops,
        List<RoadDto> roads,
        List<BusDto> buses,
        List<DemandRequestDto> pendingDemands,
        List<ReliefTripDto> trips
) {}
