package com.transport.backend.model;

import java.util.Objects;

/**
 * Represents a directional road segment between two transit stops.
 */
public class Road {

    private final String fromStopId;
    private final String toStopId;
    private double travelTimeMinutes;
    private final boolean narrowRoad;
    private double distanceKm;
    private boolean open = true;

    public Road(String fromStopId, String toStopId, double travelTimeMinutes, boolean narrowRoad, double distanceKm) {
        if (fromStopId == null || fromStopId.trim().isEmpty()) {
            throw new IllegalArgumentException("From-stop ID must not be null or blank");
        }
        if (toStopId == null || toStopId.trim().isEmpty()) {
            throw new IllegalArgumentException("To-stop ID must not be null or blank");
        }
        if (fromStopId.trim().equalsIgnoreCase(toStopId.trim())) {
            throw new IllegalArgumentException("Road cannot have identical source and destination stops");
        }
        if (travelTimeMinutes <= 0.0) {
            throw new IllegalArgumentException("Travel time must be strictly positive (> 0), got: " + travelTimeMinutes);
        }

        this.fromStopId = fromStopId.trim();
        this.toStopId = toStopId.trim();
        this.travelTimeMinutes = travelTimeMinutes;
        this.narrowRoad = narrowRoad;
        this.distanceKm = distanceKm > 0.0 ? distanceKm : Math.round(travelTimeMinutes * 0.65 * 10.0) / 10.0;
        this.open = true;
    }

    public Road(String fromStopId, String toStopId, double travelTimeMinutes, boolean narrowRoad) {
        this(fromStopId, toStopId, travelTimeMinutes, narrowRoad, Math.round(travelTimeMinutes * 0.65 * 10.0) / 10.0);
    }

    public Road(String fromStopId, String toStopId, double travelTimeMinutes) {
        this(fromStopId, toStopId, travelTimeMinutes, false);
    }

    public String getFromStopId() {
        return fromStopId;
    }

    public String getToStopId() {
        return toStopId;
    }

    public double getTravelTimeMinutes() {
        return travelTimeMinutes;
    }

    public void setTravelTimeMinutes(double travelTimeMinutes) {
        if (travelTimeMinutes <= 0.0) {
            throw new IllegalArgumentException("Travel time must be strictly positive (> 0), got: " + travelTimeMinutes);
        }
        this.travelTimeMinutes = travelTimeMinutes;
    }

    public boolean isNarrowRoad() {
        return narrowRoad;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        if (distanceKm > 0.0) {
            this.distanceKm = distanceKm;
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isClosed() {
        return !open;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Road road = (Road) o;
        return Objects.equals(fromStopId, road.fromStopId) && Objects.equals(toStopId, road.toStopId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromStopId, toStopId);
    }

    @Override
    public String toString() {
        return "Road{" +
                "from='" + fromStopId + '\'' +
                ", to='" + toStopId + '\'' +
                ", travelTimeMinutes=" + travelTimeMinutes +
                ", narrowRoad=" + narrowRoad +
                ", distanceKm=" + distanceKm +
                ", open=" + open +
                '}';
    }
}
