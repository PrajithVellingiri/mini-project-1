package com.transport.backend.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a batch of passenger travel demand from a pickup stop to a destination stop.
 * Preserves the original arrival order (FIFO priority) even when the batch is partially served.
 */
public class DemandRequest {

    private final String requestId;
    private final Stop pickupStop;
    private final Stop destinationStop;
    private int passengerCount;
    private final long originalArrivalOrder;
    private final LocalDateTime arrivalTime;

    public DemandRequest(String requestId, Stop pickupStop, Stop destinationStop, int passengerCount, long originalArrivalOrder) {
        this(requestId, pickupStop, destinationStop, passengerCount, originalArrivalOrder, LocalDateTime.now());
    }

    public DemandRequest(String requestId, Stop pickupStop, Stop destinationStop, int passengerCount, long originalArrivalOrder, LocalDateTime arrivalTime) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID must not be null or blank");
        }
        if (pickupStop == null) {
            throw new IllegalArgumentException("Pickup stop cannot be null");
        }
        if (destinationStop == null) {
            throw new IllegalArgumentException("Destination stop cannot be null");
        }
        if (pickupStop.equals(destinationStop)) {
            throw new IllegalArgumentException("Pickup and destination stops cannot be identical: " + pickupStop.getId());
        }
        if (passengerCount <= 0) {
            throw new IllegalArgumentException("Passenger count must be strictly positive (> 0), got: " + passengerCount);
        }
        if (originalArrivalOrder < 0) {
            throw new IllegalArgumentException("Original arrival order must be non-negative (>= 0), got: " + originalArrivalOrder);
        }

        this.requestId = requestId.trim();
        this.pickupStop = pickupStop;
        this.destinationStop = destinationStop;
        this.passengerCount = passengerCount;
        this.originalArrivalOrder = originalArrivalOrder;
        this.arrivalTime = (arrivalTime != null) ? arrivalTime : LocalDateTime.now();
    }

    public String getRequestId() {
        return requestId;
    }

    public Stop getPickupStop() {
        return pickupStop;
    }

    public Stop getDestinationStop() {
        return destinationStop;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public long getOriginalArrivalOrder() {
        return originalArrivalOrder;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    /**
     * Checks if this demand batch is an emergency relief request
     * (e.g. stranded passengers from a broken-down transit bus).
     */
    public boolean isEmergency() {
        return this.originalArrivalOrder == 0L || this.requestId.startsWith("DEM-EMG-");
    }

    /**
     * Partially serves a portion of this passenger batch, reducing passenger count
     * while strictly maintaining the original arrival order for future ranking.
     *
     * @param servedCount Number of passengers boarded/served
     */
    public void servePartial(int servedCount) {
        if (servedCount <= 0) {
            throw new IllegalArgumentException("Served count must be positive (> 0), got: " + servedCount);
        }
        if (servedCount > passengerCount) {
            throw new IllegalArgumentException(String.format(
                    "Cannot serve %d passengers from batch %s with only %d passengers",
                    servedCount, requestId, passengerCount
            ));
        }
        this.passengerCount -= servedCount;
    }

    /**
     * Splits this batch, returning a new DemandRequest for the unserved remainder
     * that retains the EXACT original arrival order.
     *
     * @param newRequestId ID for the remaining request
     * @param servedCount  Number of passengers boarded
     * @return New DemandRequest with the remaining passenger count and original arrival order
     */
    public DemandRequest createRemainingBatch(String newRequestId, int servedCount) {
        if (servedCount <= 0 || servedCount >= passengerCount) {
            throw new IllegalArgumentException(String.format(
                    "Invalid servedCount %d for batch with %d passengers",
                    servedCount, passengerCount
            ));
        }
        int remainingCount = this.passengerCount - servedCount;
        return new DemandRequest(newRequestId, this.pickupStop, this.destinationStop, remainingCount, this.originalArrivalOrder, this.arrivalTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DemandRequest that = (DemandRequest) o;
        return Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    @Override
    public String toString() {
        return "DemandRequest{" +
                "id='" + requestId + '\'' +
                ", pickup=" + pickupStop.getId() +
                ", dest=" + destinationStop.getId() +
                ", passengers=" + passengerCount +
                ", arrivalOrder=" + originalArrivalOrder +
                '}';
    }
}
