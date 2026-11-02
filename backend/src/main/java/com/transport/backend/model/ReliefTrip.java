package com.transport.backend.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Tracks a relief or dynamic dispatch trip, including its assigned bus, passenger demand,
 * scheduled route sequence, estimated travel time, and lifecycle status.
 */
public class ReliefTrip {

    private final String tripId;
    private final Bus assignedBus;
    private final DemandRequest demandRequest;
    private final List<Stop> route;
    private final double estimatedTravelTimeMinutes;
    private TripStatus status;

    public ReliefTrip(String tripId, Bus assignedBus, DemandRequest demandRequest, List<Stop> route, double estimatedTravelTimeMinutes) {
        this(tripId, assignedBus, demandRequest, route, estimatedTravelTimeMinutes, TripStatus.SCHEDULED);
    }

    public ReliefTrip(String tripId, Bus assignedBus, DemandRequest demandRequest, List<Stop> route, double estimatedTravelTimeMinutes, TripStatus status) {
        if (tripId == null || tripId.trim().isEmpty()) {
            throw new IllegalArgumentException("Trip ID must not be null or blank");
        }
        if (assignedBus == null) {
            throw new IllegalArgumentException("Assigned bus cannot be null");
        }
        if (demandRequest == null) {
            throw new IllegalArgumentException("Demand request cannot be null");
        }
        if (route == null || route.isEmpty()) {
            throw new IllegalArgumentException("Route must contain at least one stop");
        }
        if (estimatedTravelTimeMinutes < 0.0) {
            throw new IllegalArgumentException("Estimated travel time cannot be negative, got: " + estimatedTravelTimeMinutes);
        }
        if (status == null) {
            throw new IllegalArgumentException("TripStatus cannot be null");
        }

        this.tripId = tripId.trim();
        this.assignedBus = assignedBus;
        this.demandRequest = demandRequest;
        this.route = new ArrayList<>(route);
        this.estimatedTravelTimeMinutes = estimatedTravelTimeMinutes;
        this.status = status;
    }

    public String getTripId() {
        return tripId;
    }

    public Bus getAssignedBus() {
        return assignedBus;
    }

    public DemandRequest getDemandRequest() {
        return demandRequest;
    }

    public List<Stop> getRoute() {
        return Collections.unmodifiableList(route);
    }

    public double getEstimatedTravelTimeMinutes() {
        return estimatedTravelTimeMinutes;
    }

    public TripStatus getStatus() {
        return status;
    }

    /**
     * Starts this relief trip, verifying state and updating the assigned bus status.
     */
    public void startTrip() {
        if (this.status != TripStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot start trip in status: " + this.status);
        }
        this.status = TripStatus.IN_PROGRESS;
        this.assignedBus.startTrip();
    }

    /**
     * Completes this relief trip and updates the assigned bus position to the route destination.
     */
    public void completeTrip() {
        if (this.status != TripStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot complete trip in status: " + this.status);
        }
        this.status = TripStatus.COMPLETED;
        Stop terminalStop = route.get(route.size() - 1);
        this.assignedBus.completeTrip(terminalStop);
    }

    /**
     * Cancels this relief trip, releasing the assigned bus to AVAILABLE status.
     */
    public void cancelTrip() {
        if (this.status == TripStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel an already completed trip");
        }
        this.status = TripStatus.CANCELLED;
        if (this.assignedBus.getStatus() == BusStatus.IN_TRANSIT) {
            this.assignedBus.setStatus(BusStatus.AVAILABLE);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReliefTrip that = (ReliefTrip) o;
        return Objects.equals(tripId, that.tripId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tripId);
    }

    @Override
    public String toString() {
        return "ReliefTrip{" +
                "tripId='" + tripId + '\'' +
                ", bus=" + assignedBus.getId() +
                ", demand=" + demandRequest.getRequestId() +
                ", stops=" + route.size() +
                ", travelTime=" + estimatedTravelTimeMinutes +
                ", status=" + status +
                '}';
    }
}
