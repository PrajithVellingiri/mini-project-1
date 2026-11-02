package com.transport.backend.dto;

public record RoadDto(
        String fromStopId,
        String toStopId,
        double travelTimeMinutes,
        boolean narrowRoad,
        boolean open
) {
    public RoadDto(String fromStopId, String toStopId, double travelTimeMinutes, boolean narrowRoad) {
        this(fromStopId, toStopId, travelTimeMinutes, narrowRoad, true);
    }
}
