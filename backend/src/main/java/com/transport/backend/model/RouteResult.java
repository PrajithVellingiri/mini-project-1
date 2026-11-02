package com.transport.backend.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable result of a shortest-path route calculation.
 * Encapsulates the ordered sequence of stops, total travel time, and reachability.
 */
public final class RouteResult {

    private final List<Stop> stops;
    private final double totalTravelTimeMinutes;
    private final double totalDistanceKm;
    private final boolean reachable;
    private final long executionTimeNanos;

    private RouteResult(List<Stop> stops, double totalTravelTimeMinutes, double totalDistanceKm, boolean reachable, long executionTimeNanos) {
        this.stops = stops != null ? Collections.unmodifiableList(new ArrayList<>(stops)) : Collections.emptyList();
        this.totalTravelTimeMinutes = totalTravelTimeMinutes;
        this.totalDistanceKm = Math.round(totalDistanceKm * 10.0) / 10.0;
        this.reachable = reachable;
        this.executionTimeNanos = executionTimeNanos;
    }

    /**
     * Creates a successful, reachable route result with explicit travel time, distance, and telemetry latency.
     *
     * @param stops                  Ordered list of stops from source to destination
     * @param totalTravelTimeMinutes Total minimum travel time in minutes
     * @param totalDistanceKm        Total route distance in kilometers
     * @param executionTimeNanos     High-resolution execution time in nanoseconds
     */
    public static RouteResult reachable(List<Stop> stops, double totalTravelTimeMinutes, double totalDistanceKm, long executionTimeNanos) {
        if (stops == null || stops.isEmpty()) {
            throw new IllegalArgumentException("Reachable route must contain at least one stop");
        }
        if (totalTravelTimeMinutes < 0.0) {
            throw new IllegalArgumentException("Travel time cannot be negative, got: " + totalTravelTimeMinutes);
        }
        return new RouteResult(stops, totalTravelTimeMinutes, totalDistanceKm, true, executionTimeNanos);
    }

    /**
     * Creates a successful, reachable route result with explicit travel time and distance.
     */
    public static RouteResult reachable(List<Stop> stops, double totalTravelTimeMinutes, double totalDistanceKm) {
        return reachable(stops, totalTravelTimeMinutes, totalDistanceKm, 0L);
    }

    /**
     * Creates a successful, reachable route result with travel time.
     */
    public static RouteResult reachable(List<Stop> stops, double totalTravelTimeMinutes) {
        return reachable(stops, totalTravelTimeMinutes, Math.round(totalTravelTimeMinutes * 0.65 * 10.0) / 10.0, 0L);
    }

    /**
     * Creates an unreachable route result indicating no path exists with execution time.
     */
    public static RouteResult unreachable(long executionTimeNanos) {
        return new RouteResult(Collections.emptyList(), Double.POSITIVE_INFINITY, 0.0, false, executionTimeNanos);
    }

    /**
     * Creates an unreachable route result indicating no path exists.
     */
    public static RouteResult unreachable() {
        return unreachable(0L);
    }

    public List<Stop> getStops() {
        return stops;
    }

    public List<String> getStopIds() {
        return stops.stream().map(Stop::getId).toList();
    }

    public double getTotalTravelTimeMinutes() {
        return totalTravelTimeMinutes;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public boolean isReachable() {
        return reachable;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public double getExecutionTimeMicros() {
        return Math.round((executionTimeNanos / 1000.0) * 10.0) / 10.0;
    }

    public int getStopCount() {
        return stops.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteResult that = (RouteResult) o;
        return Double.compare(that.totalTravelTimeMinutes, totalTravelTimeMinutes) == 0 &&
                reachable == that.reachable &&
                Objects.equals(stops, that.stops);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stops, totalTravelTimeMinutes, reachable);
    }

    @Override
    public String toString() {
        if (!reachable) {
            return "RouteResult{reachable=false, travelTime=Infinity, path=[]}";
        }
        return "RouteResult{" +
                "reachable=true" +
                ", travelTime=" + totalTravelTimeMinutes + " mins" +
                ", stops=" + getStopIds() +
                '}';
    }
}
