package com.transport.backend.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable report of a bus allocation pass.
 * Details the created relief trips, allocated passenger counts, and remaining pending demand.
 */
public final class AllocationResult {

    private final List<ReliefTrip> assignedTrips;
    private final int allocatedPassengerCount;
    private final int pendingPassengerCount;
    private final List<DemandRequest> pendingDemands;

    public AllocationResult(List<ReliefTrip> assignedTrips, int allocatedPassengerCount, int pendingPassengerCount, List<DemandRequest> pendingDemands) {
        this.assignedTrips = assignedTrips != null ? Collections.unmodifiableList(new ArrayList<>(assignedTrips)) : Collections.emptyList();
        this.allocatedPassengerCount = Math.max(0, allocatedPassengerCount);
        this.pendingPassengerCount = Math.max(0, pendingPassengerCount);
        this.pendingDemands = pendingDemands != null ? Collections.unmodifiableList(new ArrayList<>(pendingDemands)) : Collections.emptyList();
    }

    public List<ReliefTrip> getAssignedTrips() {
        return assignedTrips;
    }

    public int getAllocatedPassengerCount() {
        return allocatedPassengerCount;
    }

    public int getPendingPassengerCount() {
        return pendingPassengerCount;
    }

    public List<DemandRequest> getPendingDemands() {
        return pendingDemands;
    }

    public int getAssignedTripCount() {
        return assignedTrips.size();
    }

    @Override
    public String toString() {
        return "AllocationResult{" +
                "trips=" + assignedTrips.size() +
                ", allocated=" + allocatedPassengerCount +
                ", pending=" + pendingPassengerCount +
                ", remainingBatches=" + pendingDemands.size() +
                '}';
    }
}
