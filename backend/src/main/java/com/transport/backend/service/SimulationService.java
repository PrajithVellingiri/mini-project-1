package com.transport.backend.service;

import com.transport.backend.dto.*;
import com.transport.backend.model.*;
import com.transport.backend.network.RoadNetwork;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages the in-memory simulation state for Smart Public Transportation Route Optimization.
 * Owns the road network, fleet buses, demand queues, and active relief trips.
 */
@Service
public class SimulationService {

    private static final AtomicLong USER_DEMAND_COUNTER = new AtomicLong(100);

    private RoadNetwork roadNetwork;
    private RoutePlanner routePlanner;
    private DemandQueueService demandQueueService;
    private AllocationService allocationService;

    private final List<Bus> buses = new ArrayList<>();
    private final List<ReliefTrip> activeTrips = new ArrayList<>();
    private final List<ReliefTrip> completedTrips = new ArrayList<>();
    private final List<ReliefTrip> cancelledTrips = new ArrayList<>();
    private int totalInitialPassengers = 0;
    private int totalAllocatedPassengers = 0;

    private final SimulationPersistenceService persistenceService;

    public SimulationService(SimulationPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    /**
     * Hydrates the in-memory simulation state directly from the loaded persistent dataset.
     */
    public synchronized void initializeFromDataset(SimulationPersistenceService.LoadedDataset dataset) {
        if (dataset == null) {
            throw new IllegalArgumentException("LoadedDataset cannot be null");
        }
        this.roadNetwork = dataset.roadNetwork();
        this.routePlanner = new RoutePlanner(this.roadNetwork);
        this.demandQueueService = dataset.demandQueueService();
        this.allocationService = new AllocationService(this.routePlanner);
        this.allocationService.setTripIdSequence(dataset.nextTripSequence());

        this.buses.clear();
        this.buses.addAll(dataset.buses());

        this.activeTrips.clear();
        this.activeTrips.addAll(dataset.activeTrips());
        this.completedTrips.clear();
        this.cancelledTrips.clear();

        this.totalInitialPassengers = dataset.totalInitialPassengers();
        this.totalAllocatedPassengers = dataset.totalAllocatedPassengers();
    }

    public synchronized void init() {
        resetSimulation();
    }

    /**
     * Completely restores the deterministic initial simulation state.
     */
    public synchronized void resetSimulation() {
        roadNetwork = new RoadNetwork();
        routePlanner = new RoutePlanner(roadNetwork);
        demandQueueService = new DemandQueueService();
        allocationService = new AllocationService(routePlanner);

        buses.clear();
        activeTrips.clear();
        completedTrips.clear();
        cancelledTrips.clear();
        totalAllocatedPassengers = 0;

        // 1. Seed Stops (1 Depot + 12 Stops = 13 Stops total)
        Stop depot = new Stop("DEPOT", "Central Transit Depot");
        Stop s1 = new Stop("S1", "Downtown Hub");
        Stop s2 = new Stop("S2", "Central Station");
        Stop s3 = new Stop("S3", "University Campus");
        Stop s4 = new Stop("S4", "Tech Park North");
        Stop s5 = new Stop("S5", "City Hospital");
        Stop s6 = new Stop("S6", "Market Square");
        Stop s7 = new Stop("S7", "Old Town Historic");
        Stop s8 = new Stop("S8", "South Harbor");
        Stop s9 = new Stop("S9", "Airport Terminal");
        Stop s10 = new Stop("S10", "West End Mall");
        Stop s11 = new Stop("S11", "Industrial Zone");
        Stop s12 = new Stop("S12", "Riverside Park");

        List<Stop> allStops = List.of(depot, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12);
        allStops.forEach(roadNetwork::addStop);

        // 2. Seed Roads with positive travel times
        // Depot arterial links
        roadNetwork.addBidirectionalRoad("DEPOT", "S1", 8.0, false);
        roadNetwork.addBidirectionalRoad("DEPOT", "S2", 10.0, false);
        roadNetwork.addBidirectionalRoad("DEPOT", "S11", 12.0, false);

        // Transit corridors
        roadNetwork.addBidirectionalRoad("S1", "S2", 6.0, false);
        roadNetwork.addBidirectionalRoad("S1", "S3", 9.0, false);
        roadNetwork.addBidirectionalRoad("S2", "S4", 11.0, false);
        roadNetwork.addBidirectionalRoad("S3", "S4", 7.0, false);
        roadNetwork.addBidirectionalRoad("S3", "S5", 8.0, false);
        roadNetwork.addBidirectionalRoad("S4", "S9", 14.0, false);
        roadNetwork.addBidirectionalRoad("S5", "S6", 5.0, false);

        // Narrow road corridor (demonstrates MiniBus vs StandardBus restrictions)
        roadNetwork.addBidirectionalRoad("S6", "S7", 4.0, true);
        roadNetwork.addBidirectionalRoad("S7", "S1", 5.0, true);

        // Southern & Outer links
        roadNetwork.addBidirectionalRoad("S6", "S8", 10.0, false);
        roadNetwork.addBidirectionalRoad("S8", "S9", 12.0, false);
        roadNetwork.addBidirectionalRoad("S2", "S10", 8.0, false);
        roadNetwork.addBidirectionalRoad("S10", "S11", 9.0, false);
        roadNetwork.addBidirectionalRoad("S11", "S12", 7.0, false);
        roadNetwork.addBidirectionalRoad("S12", "S8", 11.0, false);
        roadNetwork.addBidirectionalRoad("S10", "S12", 6.0, false);

        // 3. Seed Fleet (4 Buses: 2 StandardBus + 2 MiniBus with distinct capacities)
        Bus bus1 = new StandardBus("BUS-01", 45, depot);
        Bus bus2 = new StandardBus("BUS-02", 40, depot);
        Bus bus3 = new MiniBus("BUS-03", 20, depot);
        Bus bus4 = new MiniBus("BUS-04", 15, s1);
        buses.addAll(List.of(bus1, bus2, bus3, bus4));

        // 4. Seed 22 Demand Batches
        List<DemandRequest> sampleDemands = List.of(
                new DemandRequest("DEM-01", s1, s2, 25, 1L),
                new DemandRequest("DEM-02", s1, s2, 40, 2L),  // Same corridor as DEM-01 (tests FIFO)
                new DemandRequest("DEM-03", s2, s4, 30, 3L),
                new DemandRequest("DEM-04", s3, s9, 35, 4L),
                new DemandRequest("DEM-05", s5, s7, 14, 5L),  // Destination via narrow street
                new DemandRequest("DEM-06", s4, s2, 20, 6L),
                new DemandRequest("DEM-07", s10, s1, 28, 7L),
                new DemandRequest("DEM-08", s1, s3, 15, 8L),
                new DemandRequest("DEM-09", s8, s9, 22, 9L),
                new DemandRequest("DEM-10", s11, s2, 32, 10L),
                new DemandRequest("DEM-11", s1, s2, 18, 11L), // Third batch for S1->S2
                new DemandRequest("DEM-12", s6, s7, 10, 12L), // Pickup/dest in narrow historic sector
                new DemandRequest("DEM-13", s9, s1, 45, 13L), // High volume airport demand
                new DemandRequest("DEM-14", s12, s10, 16, 14L),
                new DemandRequest("DEM-15", s3, s5, 24, 15L),
                new DemandRequest("DEM-16", s2, s11, 20, 16L),
                new DemandRequest("DEM-17", s10, s8, 18, 17L),
                new DemandRequest("DEM-18", s4, s1, 26, 18L),
                new DemandRequest("DEM-19", s7, s3, 12, 19L),
                new DemandRequest("DEM-20", s5, s9, 30, 20L),
                new DemandRequest("DEM-21", s8, s1, 25, 21L),
                new DemandRequest("DEM-22", s11, s12, 15, 22L)
        );

        totalInitialPassengers = 0;
        for (DemandRequest dem : sampleDemands) {
            totalInitialPassengers += dem.getPassengerCount();
            demandQueueService.enqueue(dem);
        }

        if (persistenceService != null) {
            try {
                persistenceService.seedInitialData(allStops, roadNetwork.getAllRoads(), buses, sampleDemands);
            } catch (Exception e) {
                // If database is not reachable during reset, continue in-memory
            }
        }
    }

    /**
     * Executes an allocation pass over available fleet and waiting demand.
     */
    public synchronized AllocationResult runSimulation() {
        AllocationResult result = allocationService.allocate(buses, demandQueueService);
        activeTrips.addAll(result.getAssignedTrips());
        totalAllocatedPassengers += result.getAllocatedPassengerCount();

        if (persistenceService != null && !result.getAssignedTrips().isEmpty()) {
            try {
                persistenceService.persistAllocationRun(result, buses, demandQueueService.getTotalWaitingPassengers());
            } catch (Exception e) {
                // Silently preserve algorithm execution even if DB write fails
            }
        }

        return result;
    }

    /**
     * Marks all in-progress trips completed, arriving at destinations and freeing buses.
     */
    public synchronized void completeAllActiveTrips() {
        Iterator<ReliefTrip> it = activeTrips.iterator();
        while (it.hasNext()) {
            ReliefTrip trip = it.next();
            if (trip.getStatus() == TripStatus.IN_PROGRESS || trip.getStatus() == TripStatus.SCHEDULED) {
                if (trip.getStatus() == TripStatus.SCHEDULED) {
                    trip.startTrip();
                }
                trip.completeTrip();
                completedTrips.add(trip);
                if (persistenceService != null) {
                    try {
                        persistenceService.persistTripCompletion(trip);
                    } catch (Exception ignored) {}
                }
                it.remove();
            }
        }
    }

    /**
     * Marks a specific in-progress trip completed, moving its bus to the terminal stop and freeing it.
     */
    public synchronized ReliefTrip completeTrip(String tripId) {
        if (tripId == null || tripId.trim().isEmpty()) {
            throw new IllegalArgumentException("Trip ID cannot be null or blank");
        }
        ReliefTrip trip = activeTrips.stream()
                .filter(t -> t.getTripId().equalsIgnoreCase(tripId.trim()))
                .findFirst()
                .orElse(null);

        if (trip == null) {
            boolean alreadyCompleted = completedTrips.stream()
                    .anyMatch(t -> t.getTripId().equalsIgnoreCase(tripId.trim()));
            if (alreadyCompleted) {
                throw new IllegalStateException("Trip is already completed: " + tripId);
            }
            boolean alreadyCancelled = cancelledTrips.stream()
                    .anyMatch(t -> t.getTripId().equalsIgnoreCase(tripId.trim()));
            if (alreadyCancelled) {
                throw new IllegalStateException("Trip is already cancelled: " + tripId);
            }
            throw new NoSuchElementException("Trip not found with ID: " + tripId);
        }

        if (trip.getStatus() == TripStatus.SCHEDULED) {
            trip.startTrip();
        }
        trip.completeTrip();
        completedTrips.add(trip);
        activeTrips.remove(trip);

        if (persistenceService != null) {
            try {
                persistenceService.persistTripCompletion(trip);
            } catch (Exception ignored) {}
        }

        return trip;
    }

    /**
     * Updates travel time on a directed road edge.
     */
    public synchronized void updateRoadTravelTime(String fromStopId, String toStopId, double travelTimeMinutes) {
        if (travelTimeMinutes <= 0.0) {
            throw new IllegalArgumentException("Travel time must be strictly positive (> 0)");
        }
        double oldTime = 0.0;
        try {
            Road existing = roadNetwork.getRoad(fromStopId, toStopId);
            if (existing != null) {
                oldTime = existing.getTravelTimeMinutes();
            }
        } catch (Exception ignored) {}

        roadNetwork.updateTravelTime(fromStopId, toStopId, travelTimeMinutes);

        if (persistenceService != null) {
            try {
                persistenceService.persistRoadTravelTimeUpdate(fromStopId, toStopId, oldTime, travelTimeMinutes);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Updates road availability (open or closed to all traffic).
     *
     * @param fromStopId    Origin stop ID
     * @param toStopId      Destination stop ID
     * @param open          True if open, false if closed
     * @param bidirectional If true, also updates the reverse direction (toStopId -> fromStopId)
     */
    public synchronized void setRoadOpen(String fromStopId, String toStopId, boolean open, boolean bidirectional) {
        if (fromStopId == null || toStopId == null) {
            throw new IllegalArgumentException("fromStopId and toStopId must not be null");
        }
        roadNetwork.setRoadOpen(fromStopId, toStopId, open);
        if (bidirectional && roadNetwork.getRoad(toStopId, fromStopId) != null) {
            roadNetwork.setRoadOpen(toStopId, fromStopId, open);
        }
    }

    /**
     * Updates a vehicle's status (e.g. MAINTENANCE or AVAILABLE).
     */
    public synchronized void updateBusStatus(String busId, String statusName) {
        if (busId == null || statusName == null) {
            throw new IllegalArgumentException("Bus ID and status cannot be null");
        }
        Bus bus = buses.stream()
                .filter(b -> b.getId().equalsIgnoreCase(busId.trim()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Bus not found with ID: " + busId));

        BusStatus newStatus = BusStatus.valueOf(statusName.trim().toUpperCase());
        bus.setStatus(newStatus);

        if (persistenceService != null) {
            try {
                persistenceService.persistBusStatus(busId, statusName);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Finds the minimum-travel-time route between any two stops.
     */
    public synchronized RouteResponseDto findRoute(String sourceId, String destinationId, String vehicleType) {
        Bus testVehicle = null;
        if ("MINIBUS".equalsIgnoreCase(vehicleType)) {
            testVehicle = new MiniBus("TEST-MINI", roadNetwork.getStop(sourceId));
        } else if ("STANDARD".equalsIgnoreCase(vehicleType) || "STANDARDBUS".equalsIgnoreCase(vehicleType)) {
            testVehicle = new StandardBus("TEST-STD", roadNetwork.getStop(sourceId));
        }

        RouteResult result = routePlanner.findShortestRoute(sourceId, destinationId, testVehicle);

        List<StopDto> stopDtos = result.getStops().stream()
                .map(s -> new StopDto(s.getId(), s.getName()))
                .toList();

        return new RouteResponseDto(
                result.isReachable(),
                result.getTotalTravelTimeMinutes(),
                result.getTotalDistanceKm(),
                result.getExecutionTimeMicros(),
                stopDtos,
                result.getStopIds(),
                vehicleType != null ? vehicleType.toUpperCase() : "GENERAL"
        );
    }

    /**
     * Returns full snapshot of current simulation state.
     */
    public synchronized SimulationStateDto getState() {
        List<StopDto> stopDtos = roadNetwork.getAllStops().stream()
                .map(s -> new StopDto(s.getId(), s.getName()))
                .toList();

        List<RoadDto> roadDtos = roadNetwork.getAllRoads().stream()
                .map(r -> new RoadDto(r.getFromStopId(), r.getToStopId(), r.getTravelTimeMinutes(), r.isNarrowRoad(), r.isOpen()))
                .toList();

        List<BusDto> busDtos = buses.stream()
                .map(b -> new BusDto(
                        b.getId(),
                        b.getClass().getSimpleName(),
                        b.getCapacity(),
                        b.getPassengerOccupancy(),
                        b.getAvailableSeats(),
                        b.getCurrentStop().getId(),
                        b.getCurrentStop().getName(),
                        b.getStatus().name()
                ))
                .toList();

        List<DemandRequestDto> demandDtos = demandQueueService.getAllWaitingRequests().stream()
                .map(d -> new DemandRequestDto(
                        d.getRequestId(),
                        d.getPickupStop().getId(),
                        d.getPickupStop().getName(),
                        d.getDestinationStop().getId(),
                        d.getDestinationStop().getName(),
                        d.getPassengerCount(),
                        d.getOriginalArrivalOrder()
                ))
                .toList();

        List<ReliefTrip> allTrips = new ArrayList<>();
        allTrips.addAll(activeTrips);
        allTrips.addAll(completedTrips);
        allTrips.addAll(cancelledTrips);

        List<ReliefTripDto> tripDtos = allTrips.stream()
                .map(t -> new ReliefTripDto(
                        t.getTripId(),
                        t.getAssignedBus().getId(),
                        t.getAssignedBus().getClass().getSimpleName(),
                        t.getDemandRequest().getRequestId(),
                        t.getRoute().stream().map(Stop::getId).toList(),
                        t.getRoute().stream().map(Stop::getName).toList(),
                        t.getEstimatedTravelTimeMinutes(),
                        t.getStatus().name(),
                        t.getDemandRequest().getPassengerCount()
                ))
                .toList();

        int availableBusesCount = (int) buses.stream()
                .filter(b -> b.getStatus() == BusStatus.AVAILABLE && b.getAvailableSeats() > 0)
                .count();

        SimulationSummaryDto summary = new SimulationSummaryDto(
                stopDtos.size(),
                roadDtos.size(),
                buses.size(),
                availableBusesCount,
                demandDtos.size(),
                demandQueueService.getTotalWaitingPassengers(),
                totalAllocatedPassengers,
                activeTrips.size()
        );

        return new SimulationStateDto(summary, stopDtos, roadDtos, busDtos, demandDtos, tripDtos);
    }

    public synchronized RoadNetwork getRoadNetwork() {
        return roadNetwork;
    }

    /**
     * Creates and dynamically enqueues a new passenger travel demand batch.
     */
    public synchronized DemandRequest createDemand(String pickupStopId, String destinationStopId, int passengerCount) {
        if (pickupStopId == null || destinationStopId == null) {
            throw new IllegalArgumentException("Pickup and destination stops must be specified");
        }
        if (pickupStopId.trim().equalsIgnoreCase(destinationStopId.trim())) {
            throw new IllegalArgumentException("Pickup and destination stops cannot be identical");
        }
        if (passengerCount <= 0) {
            throw new IllegalArgumentException("Passenger count must be strictly positive (> 0)");
        }

        Stop pickup = roadNetwork.getStop(pickupStopId.trim());
        if (pickup == null) {
            throw new NoSuchElementException("Pickup stop not found: " + pickupStopId);
        }
        Stop dest = roadNetwork.getStop(destinationStopId.trim());
        if (dest == null) {
            throw new NoSuchElementException("Destination stop not found: " + destinationStopId);
        }

        long arrivalOrder = System.currentTimeMillis();
        String reqId = "DEM-USR-" + USER_DEMAND_COUNTER.incrementAndGet();
        DemandRequest newDemand = new DemandRequest(reqId, pickup, dest, passengerCount, arrivalOrder);
        demandQueueService.enqueue(newDemand);
        totalInitialPassengers += passengerCount;

        if (persistenceService != null) {
            try {
                persistenceService.persistDemandRequest(newDemand);
            } catch (Exception ignored) {}
        }
        return newDemand;
    }

    /**
     * Triggers a breakdown for an eligible bus:
     * - Bus marked as MAINTENANCE.
     * - Active trip cancelled.
     * - Boarded passengers alighted at current stop and re-queued as emergency relief demand.
     * - Total allocated passengers decremented to preserve passenger conservation invariant.
     */
    public synchronized BreakdownResponseDto triggerBusBreakdown(String busId) {
        if (busId == null || busId.trim().isEmpty()) {
            throw new IllegalArgumentException("Bus ID must not be null or blank");
        }
        Bus bus = buses.stream()
                .filter(b -> b.getId().equalsIgnoreCase(busId.trim()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Bus not found with ID: " + busId));

        BusStatus prevStatus = bus.getStatus();
        if (prevStatus == BusStatus.MAINTENANCE || prevStatus == BusStatus.OUT_OF_SERVICE) {
            throw new IllegalStateException("Bus " + busId + " is already out of service (" + prevStatus + ")");
        }

        ReliefTrip activeTrip = activeTrips.stream()
                .filter(t -> t.getAssignedBus().getId().equalsIgnoreCase(bus.getId())
                        && t.getStatus() != TripStatus.COMPLETED
                        && t.getStatus() != TripStatus.CANCELLED)
                .findFirst()
                .orElse(null);

        boolean hadActiveTrip = false;
        String cancelledTripId = null;
        int strandedPax = 0;
        String strandedStopId = bus.getCurrentStop().getId();
        String strandedStopName = bus.getCurrentStop().getName();
        String reliefDemandId = null;
        String message;

        if (activeTrip != null) {
            hadActiveTrip = true;
            cancelledTripId = activeTrip.getTripId();
            strandedPax = bus.getPassengerOccupancy();
            Stop strandedStop = bus.getCurrentStop();
            Stop originalDestination = activeTrip.getDemandRequest().getDestinationStop();

            // 1. Cancel active trip
            activeTrip.cancelTrip();
            cancelledTrips.add(activeTrip);
            activeTrips.remove(activeTrip);

            // 2. Set bus to MAINTENANCE and alight all passengers
            bus.setStatus(BusStatus.MAINTENANCE);
            if (strandedPax > 0) {
                bus.clearPassengers();
            }

            // 3. Deduct uncompleted stranded pax from totalAllocatedPassengers to preserve invariant:
            // Total = Waiting + In-Transit + Completed
            boolean arrivedAtDestination = strandedStop.getId().equalsIgnoreCase(originalDestination.getId());
            if (strandedPax > 0 && !arrivedAtDestination) {
                totalAllocatedPassengers = Math.max(0, totalAllocatedPassengers - strandedPax);

                // 4. Generate emergency relief demand for stranded passengers
                reliefDemandId = "DEM-EMG-" + cancelledTripId;
                DemandRequest reliefDemand = new DemandRequest(
                        reliefDemandId,
                        strandedStop,
                        originalDestination,
                        strandedPax,
                        0L // Top emergency priority
                );
                demandQueueService.enqueue(reliefDemand);

                if (persistenceService != null) {
                    try {
                        persistenceService.persistDemandRequest(reliefDemand);
                    } catch (Exception ignored) {}
                }
            }

            if (persistenceService != null) {
                try {
                    persistenceService.persistTripCancellation(activeTrip);
                } catch (Exception ignored) {}
            }

            message = String.format("Bus %s broke down with active trip %s. %d passengers de-boarded at %s (%s) and re-queued with emergency priority.",
                    bus.getId(), cancelledTripId, strandedPax, strandedStopId, strandedStopName);
        } else {
            // Bus was idle without active trip
            bus.setStatus(BusStatus.MAINTENANCE);
            if (persistenceService != null) {
                try {
                    persistenceService.persistBusStatus(bus.getId(), "MAINTENANCE");
                } catch (Exception ignored) {}
            }
            message = String.format("Bus %s operational status updated to MAINTENANCE. Bus is unavailable for future trips.", bus.getId());
        }

        return new BreakdownResponseDto(
                bus.getId(),
                prevStatus.name(),
                bus.getStatus().name(),
                hadActiveTrip,
                cancelledTripId,
                strandedPax,
                strandedStopId,
                strandedStopName,
                reliefDemandId,
                message
        );
    }

    /**
     * Restores a broken-down or maintenance bus back to AVAILABLE status.
     */
    public synchronized Map<String, Object> restoreBus(String busId) {
        if (busId == null || busId.trim().isEmpty()) {
            throw new IllegalArgumentException("Bus ID must not be null or blank");
        }
        Bus bus = buses.stream()
                .filter(b -> b.getId().equalsIgnoreCase(busId.trim()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Bus not found with ID: " + busId));

        bus.setStatus(BusStatus.AVAILABLE);

        if (persistenceService != null) {
            try {
                persistenceService.persistBusStatus(bus.getId(), "AVAILABLE");
            } catch (Exception ignored) {}
        }

        return Map.of(
                "busId", bus.getId(),
                "status", "AVAILABLE",
                "message", "Bus " + bus.getId() + " successfully restored to service and ready for dispatch."
        );
    }

    /**
     * Computes genuine, real-time analytics across passengers, fleet, trips, demand, and telemetry.
     */
    public synchronized AnalyticsResponseDto getAnalytics() {
        // 1. Passengers
        int waitingPax = demandQueueService.getTotalWaitingPassengers();
        int inTransitPax = buses.stream().mapToInt(Bus::getPassengerOccupancy).sum();
        int completedPax = Math.max(0, totalAllocatedPassengers - inTransitPax);
        int totalPax = totalInitialPassengers;
        boolean conservationValid = (totalPax == (waitingPax + inTransitPax + completedPax));

        PassengerAnalyticsDto passengerAnalytics = new PassengerAnalyticsDto(
                totalPax,
                waitingPax,
                inTransitPax,
                completedPax,
                conservationValid
        );

        // 2. Fleet
        int totalBuses = buses.size();
        int availableBuses = (int) buses.stream().filter(b -> b.getStatus() == BusStatus.AVAILABLE).count();
        int inTransitBuses = (int) buses.stream().filter(b -> b.getStatus() == BusStatus.IN_TRANSIT).count();
        int maintenanceBuses = (int) buses.stream().filter(b -> b.getStatus() == BusStatus.MAINTENANCE).count();
        int outOfServiceBuses = (int) buses.stream().filter(b -> b.getStatus() == BusStatus.OUT_OF_SERVICE).count();

        double fleetUtil = totalBuses > 0
                ? Math.round(((double) inTransitBuses / totalBuses * 100.0) * 10.0) / 10.0
                : 0.0;
        int totalFleetCapacity = buses.stream().mapToInt(Bus::getCapacity).sum();
        int occupiedSeats = inTransitPax;
        double seatUtil = totalFleetCapacity > 0
                ? Math.round(((double) occupiedSeats / totalFleetCapacity * 100.0) * 10.0) / 10.0
                : 0.0;

        FleetAnalyticsDto fleetAnalytics = new FleetAnalyticsDto(
                totalBuses,
                availableBuses,
                inTransitBuses,
                maintenanceBuses,
                outOfServiceBuses,
                fleetUtil,
                totalFleetCapacity,
                occupiedSeats,
                seatUtil
        );

        // 3. Trips
        int activeTripsCount = activeTrips.size();
        int completedTripsCount = completedTrips.size();
        int cancelledTripsCount = cancelledTrips.size();
        int totalDispatchedTrips = activeTripsCount + completedTripsCount + cancelledTripsCount;

        List<ReliefTrip> allKnownTrips = new ArrayList<>(activeTrips);
        allKnownTrips.addAll(completedTrips);

        double avgTripTime = allKnownTrips.isEmpty() ? 0.0
                : Math.round((allKnownTrips.stream().mapToDouble(ReliefTrip::getEstimatedTravelTimeMinutes).average().orElse(0.0)) * 10.0) / 10.0;

        double avgDistance = 0.0;
        if (!allKnownTrips.isEmpty()) {
            double totalDist = 0.0;
            for (ReliefTrip t : allKnownTrips) {
                List<Stop> stops = t.getRoute();
                for (int i = 0; i < stops.size() - 1; i++) {
                    Road r = roadNetwork.getRoad(stops.get(i).getId(), stops.get(i + 1).getId());
                    if (r != null) {
                        totalDist += r.getDistanceKm();
                    }
                }
            }
            avgDistance = Math.round((totalDist / allKnownTrips.size()) * 10.0) / 10.0;
        }

        TripAnalyticsDto tripAnalytics = new TripAnalyticsDto(
                totalDispatchedTrips,
                activeTripsCount,
                completedTripsCount,
                cancelledTripsCount,
                avgTripTime,
                avgDistance
        );

        // 4. Demand by Corridor
        Map<String, List<DemandRequest>> byCorridor = new LinkedHashMap<>();
        for (DemandRequest req : demandQueueService.getAllWaitingRequests()) {
            String corridorKey = req.getPickupStop().getId() + "->" + req.getDestinationStop().getId();
            byCorridor.computeIfAbsent(corridorKey, k -> new ArrayList<>()).add(req);
        }

        List<CorridorDemandDto> corridorDtos = byCorridor.entrySet().stream()
                .map(entry -> {
                    List<DemandRequest> list = entry.getValue();
                    DemandRequest first = list.get(0);
                    int pax = list.stream().mapToInt(DemandRequest::getPassengerCount).sum();
                    return new CorridorDemandDto(
                            entry.getKey(),
                            first.getPickupStop().getId(),
                            first.getPickupStop().getName(),
                            first.getDestinationStop().getId(),
                            first.getDestinationStop().getName(),
                            pax,
                            list.size()
                    );
                })
                .sorted(Comparator.comparingInt(CorridorDemandDto::waitingPassengers).reversed())
                .limit(8)
                .toList();

        DemandAnalyticsDto demandAnalytics = new DemandAnalyticsDto(
                demandQueueService.size(),
                waitingPax,
                corridorDtos
        );

        // 5. Telemetry
        AlgorithmBenchmarkDto telemetry = routePlanner.getBenchmarkMetrics();

        return new AnalyticsResponseDto(
                passengerAnalytics,
                fleetAnalytics,
                tripAnalytics,
                demandAnalytics,
                telemetry
        );
    }

    public synchronized List<Bus> getBuses() {
        return Collections.unmodifiableList(buses);
    }

    public synchronized DemandQueueService getDemandQueueService() {
        return demandQueueService;
    }

    public synchronized int getTotalInitialPassengers() {
        return totalInitialPassengers;
    }

    public synchronized int getTotalAllocatedPassengers() {
        return totalAllocatedPassengers;
    }

    public synchronized List<ReliefTrip> getActiveTrips() {
        return Collections.unmodifiableList(activeTrips);
    }
}
