package com.transport.backend.service;

import com.transport.backend.model.*;

import java.util.*;

/**
 * Capacity-aware bus allocation engine for Smart Public Transportation.
 *
 * Coordinates:
 * 1. Waiting passenger demand ranking via DemandQueueService.
 * 2. Available fleet capacity and vehicle health status.
 * 3. Dijkstra shortest paths via RoutePlanner for both dispatch and passenger transit legs.
 * 4. Vehicle-specific road constraints (e.g. MiniBus vs StandardBus on narrow roads).
 * 5. Safe partial-boarding and arrival-order preservation.
 * 6. ReliefTrip generation and lifecycle management.
 */
public class AllocationService {

    private final RoutePlanner routePlanner;
    private long tripIdSequence = 1;

    public AllocationService(RoutePlanner routePlanner) {
        if (routePlanner == null) {
            throw new IllegalArgumentException("RoutePlanner cannot be null");
        }
        this.routePlanner = routePlanner;
    }

    public synchronized void setTripIdSequence(long nextSeq) {
        if (nextSeq > this.tripIdSequence) {
            this.tripIdSequence = nextSeq;
        }
    }

    /**
     * Executes an allocation pass over waiting demand and currently available buses.
     *
     * @param buses              Fleet buses
     * @param demandQueueService Queue service holding waiting passenger demand
     * @return AllocationResult with assigned relief trips and passenger accounting
     */
    public synchronized AllocationResult allocate(List<Bus> buses, DemandQueueService demandQueueService) {
        if (buses == null || demandQueueService == null) {
            throw new IllegalArgumentException("Buses and DemandQueueService cannot be null");
        }

        List<ReliefTrip> assignedTrips = new ArrayList<>();
        int totalAllocatedPassengers = 0;

        // Allocation loop: continues while available buses with capacity exist and eligible demand can be served
        while (hasAvailableBuses(buses) && !demandQueueService.isEmpty()) {
            boolean allocationMade = false;

            // Retrieve eligible candidate batches (the heads of each corridor), sorted by global priority
            List<DemandRequest> eligibleCandidates = demandQueueService.getEligibleRequestsInPriorityOrder();

            for (DemandRequest demand : eligibleCandidates) {
                // Find the optimal available bus for this demand
                BusAssignment candidateAssignment = findBestAvailableBus(buses, demand);

                if (candidateAssignment != null) {
                    Bus bus = candidateAssignment.bus();
                    RouteResult busToPickup = candidateAssignment.busToPickup();
                    RouteResult pickupToDest = candidateAssignment.pickupToDest();

                    // Determine passenger boarding count respecting bus remaining capacity
                    int availableSeats = bus.getAvailableSeats();
                    int passengersToBoard = Math.min(demand.getPassengerCount(), availableSeats);

                    // Board passengers onto the bus
                    bus.boardPassengers(passengersToBoard);

                    // Combine dispatch and transit route segments without duplicating pickup stop
                    List<Stop> combinedRoute = combineRoutes(busToPickup.getStops(), pickupToDest.getStops());
                    double totalTravelTime = busToPickup.getTotalTravelTimeMinutes() + pickupToDest.getTotalTravelTimeMinutes();

                    // Create demand record representing the portion boarded on this trip
                    DemandRequest boardedDemand = new DemandRequest(
                            demand.getRequestId() + "-boarded",
                            demand.getPickupStop(),
                            demand.getDestinationStop(),
                            passengersToBoard,
                            demand.getOriginalArrivalOrder()
                    );

                    // Create and start the relief trip
                    String tripId = "TRIP-" + String.format("%04d", tripIdSequence++);
                    ReliefTrip trip = new ReliefTrip(tripId, bus, boardedDemand, combinedRoute, totalTravelTime);
                    trip.startTrip();
                    assignedTrips.add(trip);

                    totalAllocatedPassengers += passengersToBoard;

                    // Update demand queue: full vs partial boarding
                    if (passengersToBoard == demand.getPassengerCount()) {
                        demandQueueService.markServed(demand);
                    } else {
                        // Batch partially served: remaining passengers retain the exact same original arrival order
                        DemandRequest remainingDemand = demand.createRemainingBatch(
                                demand.getRequestId() + "-rem",
                                passengersToBoard
                        );
                        demandQueueService.markPartiallyServed(demand, remainingDemand);
                    }

                    allocationMade = true;
                    // Break inner candidate loop to re-evaluate eligible demands with newly updated fleet state
                    break;
                }
            }

            // If a complete pass through all eligible candidates yielded no possible assignments, stop
            if (!allocationMade) {
                break;
            }
        }

        int pendingPassengers = demandQueueService.getTotalWaitingPassengers();
        List<DemandRequest> pendingDemands = demandQueueService.getAllWaitingRequests();

        return new AllocationResult(assignedTrips, totalAllocatedPassengers, pendingPassengers, pendingDemands);
    }

    /**
     * Finds the best available bus for a given demand batch.
     * Evaluates route reachability to pickup and destination, vehicle road restrictions,
     * and selects the bus that can reach the pickup stop fastest.
     */
    private BusAssignment findBestAvailableBus(List<Bus> buses, DemandRequest demand) {
        Bus bestBus = null;
        RouteResult bestBusToPickup = null;
        RouteResult bestPickupToDest = null;
        double minDispatchTime = Double.POSITIVE_INFINITY;

        for (Bus bus : buses) {
            // Bus must be in AVAILABLE status with at least one free seat
            if (bus.getStatus() != BusStatus.AVAILABLE || bus.getAvailableSeats() <= 0) {
                continue;
            }

            // Check dispatch leg: bus current stop -> pickup stop
            RouteResult busToPickup = routePlanner.findShortestRoute(
                    bus.getCurrentStop().getId(),
                    demand.getPickupStop().getId(),
                    bus
            );
            if (!busToPickup.isReachable()) {
                continue;
            }

            // Check passenger transit leg: pickup stop -> destination stop
            RouteResult pickupToDest = routePlanner.findShortestRoute(
                    demand.getPickupStop().getId(),
                    demand.getDestinationStop().getId(),
                    bus
            );
            if (!pickupToDest.isReachable()) {
                continue;
            }

            // Candidate is feasible; select the bus with minimum dispatch travel time
            double dispatchTime = busToPickup.getTotalTravelTimeMinutes();
            if (dispatchTime < minDispatchTime) {
                minDispatchTime = dispatchTime;
                bestBus = bus;
                bestBusToPickup = busToPickup;
                bestPickupToDest = pickupToDest;
            }
        }

        if (bestBus != null) {
            return new BusAssignment(bestBus, bestBusToPickup, bestPickupToDest);
        }
        return null;
    }

    /**
     * Checks if at least one bus in the fleet is AVAILABLE with remaining seat capacity.
     */
    private boolean hasAvailableBuses(List<Bus> buses) {
        for (Bus bus : buses) {
            if (bus.getStatus() == BusStatus.AVAILABLE && bus.getAvailableSeats() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Combines the dispatch leg (bus to pickup) and transit leg (pickup to destination)
     * ensuring the pickup stop is not duplicated.
     */
    public static List<Stop> combineRoutes(List<Stop> busToPickup, List<Stop> pickupToDest) {
        if (busToPickup == null || busToPickup.isEmpty()) {
            return pickupToDest != null ? new ArrayList<>(pickupToDest) : Collections.emptyList();
        }
        if (pickupToDest == null || pickupToDest.isEmpty()) {
            return new ArrayList<>(busToPickup);
        }

        List<Stop> combined = new ArrayList<>(busToPickup);
        Stop pickupStop = pickupToDest.get(0);

        // If busToPickup already ends at pickupStop, start pickupToDest from index 1
        if (combined.get(combined.size() - 1).equals(pickupStop)) {
            for (int i = 1; i < pickupToDest.size(); i++) {
                combined.add(pickupToDest.get(i));
            }
        } else {
            combined.addAll(pickupToDest);
        }

        return Collections.unmodifiableList(combined);
    }

    private record BusAssignment(Bus bus, RouteResult busToPickup, RouteResult pickupToDest) {}
}
