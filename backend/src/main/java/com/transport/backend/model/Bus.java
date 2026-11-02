package com.transport.backend.model;

import java.util.Objects;

/**
 * Abstract base class representing a transit bus in the fleet.
 * Enforces capacity limits, passenger tracking, and lifecycle status transitions.
 */
public abstract class Bus {

    private final String id;
    private final int capacity;
    private int passengerOccupancy;
    private Stop currentStop;
    private BusStatus status;

    public Bus(String id, int capacity, Stop currentStop) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Bus ID must not be null or blank");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Bus capacity must be strictly positive (> 0), got: " + capacity);
        }
        if (currentStop == null) {
            throw new IllegalArgumentException("Current stop must not be null");
        }

        this.id = id.trim();
        this.capacity = capacity;
        this.passengerOccupancy = 0;
        this.currentStop = currentStop;
        this.status = BusStatus.AVAILABLE;
    }

    public String getId() {
        return id;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getPassengerOccupancy() {
        return passengerOccupancy;
    }

    public Stop getCurrentStop() {
        return currentStop;
    }

    public BusStatus getStatus() {
        return status;
    }

    public void setStatus(BusStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        this.status = status;
    }

    public int getAvailableSeats() {
        return capacity - passengerOccupancy;
    }

    public boolean isFull() {
        return passengerOccupancy >= capacity;
    }

    /**
     * Boards passengers onto the bus, validating against total capacity.
     *
     * @param count Number of passengers boarding
     * @throws IllegalArgumentException if count <= 0
     * @throws IllegalStateException    if boarding exceeds available seat capacity
     */
    public void boardPassengers(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Passenger count to board must be positive (> 0), got: " + count);
        }
        if (this.status != BusStatus.AVAILABLE && this.status != BusStatus.IN_TRANSIT) {
            throw new IllegalStateException("Cannot board passengers when bus is in status: " + this.status);
        }
        if (passengerOccupancy + count > capacity) {
            throw new IllegalStateException(String.format(
                    "Cannot board %d passengers: current occupancy is %d/%d (available: %d)",
                    count, passengerOccupancy, capacity, getAvailableSeats()
            ));
        }
        this.passengerOccupancy += count;
    }

    /**
     * Alights passengers from the bus.
     *
     * @param count Number of passengers alighting
     */
    public void alightPassengers(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Passenger count to alight must be positive (> 0), got: " + count);
        }
        if (count > passengerOccupancy) {
            throw new IllegalArgumentException(String.format(
                    "Cannot alight %d passengers: only %d currently on board",
                    count, passengerOccupancy
            ));
        }
        this.passengerOccupancy -= count;
    }

    /**
     * Clears all passengers from the bus.
     */
    public void clearPassengers() {
        this.passengerOccupancy = 0;
    }

    /**
     * Transitions the bus to IN_TRANSIT status.
     *
     * @throws IllegalStateException if bus is not currently AVAILABLE
     */
    public void startTrip() {
        if (this.status != BusStatus.AVAILABLE) {
            throw new IllegalStateException("Bus " + id + " cannot start trip while in status: " + this.status);
        }
        this.status = BusStatus.IN_TRANSIT;
    }

    /**
     * Completes an in-progress trip, arriving at the destination stop and resetting status to AVAILABLE.
     *
     * @param arrivalStop The terminal stop reached
     * @throws IllegalStateException    if bus is not currently IN_TRANSIT
     * @throws IllegalArgumentException if arrivalStop is null
     */
    public void completeTrip(Stop arrivalStop) {
        if (this.status != BusStatus.IN_TRANSIT) {
            throw new IllegalStateException("Bus " + id + " cannot complete trip while in status: " + this.status);
        }
        if (arrivalStop == null) {
            throw new IllegalArgumentException("Arrival stop cannot be null");
        }
        this.currentStop = arrivalStop;
        this.passengerOccupancy = 0;
        this.status = BusStatus.AVAILABLE;
    }

    /**
     * Safely resets and releases the bus at a designated stop, clearing occupancy.
     *
     * @param stop Stop where the bus is positioned
     */
    public void releaseBus(Stop stop) {
        if (stop == null) {
            throw new IllegalArgumentException("Release stop cannot be null");
        }
        this.currentStop = stop;
        this.passengerOccupancy = 0;
        this.status = BusStatus.AVAILABLE;
    }

    /**
     * Polymorphic road-suitability check.
     * Defines whether this vehicle type can traverse a given road segment.
     *
     * @param road The road segment to evaluate
     * @return true if the vehicle is permitted on the road, false otherwise
     */
    public abstract boolean canUseRoad(Road road);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bus bus = (Bus) o;
        return Objects.equals(id, bus.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id='" + id + '\'' +
                ", capacity=" + capacity +
                ", occupancy=" + passengerOccupancy +
                ", currentStop=" + currentStop.getId() +
                ", status=" + status +
                '}';
    }
}
