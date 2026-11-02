package com.transport.backend.dto;

public record AlgorithmBenchmarkDto(
        long totalCalculations,
        double averageLatencyMicros,
        double minLatencyMicros,
        double maxLatencyMicros,
        double lastLatencyMicros
) {}
