package com.transport.backend.service;

import com.transport.backend.model.*;
import com.transport.backend.network.RoadNetwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AllocationServiceTest {

    private RoadNetwork network;
    private RoutePlanner routePlanner;
    private AllocationService allocationService;
    private DemandQueueService queueService;

    private Stop depot;
    private Stop stopA;
    private Stop stopB;
    private Stop stopC;
    private Stop stopD;
    private Stop isolatedStop;

    @BeforeEach
    void setUp() {
        network = new RoadNetwork();
        depot = new Stop("DEPOT", "Central Bus Depot");
        stopA = new Stop("STOP_A", "Station A");
        stopB = new Stop("STOP_B", "Station B");
        stopC = new Stop("STOP_C", "Station C");
        stopD = new Stop("STOP_D", "Station D");
        isolatedStop = new Stop("STOP_ISO", "Isolated Stop");

        network.addStop(depot);
        network.addStop(stopA);
        network.addStop(stopB);
        network.addStop(stopC);
        network.addStop(stopD);
        network.addStop(isolatedStop);

        // Standard roads
        network.addBidirectionalRoad("DEPOT", "STOP_A", 10.0, false);
        network.addBidirectionalRoad("STOP_A", "STOP_B", 12.0, false);
        network.addBidirectionalRoad("STOP_B", "STOP_C", 15.0, false);
        network.addBidirectionalRoad("DEPOT", "STOP_D", 8.0, false);

        routePlanner = new RoutePlanner(network);
        allocationService = new AllocationService(routePlanner);
        queueService = new DemandQueueService();
    }

    @Test
    @DisplayName("1. Priority by passenger count: larger batch receives bus first across different corridors")
    void shouldPrioritizeLargerPassengerBatch() {
        // Group 1: STOP_A -> STOP_B (10 passengers, t=1)
        DemandRequest reqSmall = new DemandRequest("REQ-SMALL", stopA, stopB, 10, 1L);
        // Group 2: STOP_D -> STOP_B (35 passengers, t=2)
        DemandRequest reqLarge = new DemandRequest("REQ-LARGE", stopD, stopB, 35, 2L);

        queueService.enqueue(reqSmall);
        queueService.enqueue(reqLarge);

        // Fleet has only 1 available bus
        Bus bus = new StandardBus("BUS-01", 40, depot);
        List<Bus> fleet = new ArrayList<>(List.of(bus));

        AllocationResult result = allocationService.allocate(fleet, queueService);

        assertEquals(1, result.getAssignedTripCount());
        assertEquals("REQ-LARGE-boarded", result.getAssignedTrips().get(0).getDemandRequest().getRequestId());
        assertEquals(35, result.getAllocatedPassengerCount());
        assertEquals(10, result.getPendingPassengerCount());
        assertEquals(1, result.getPendingDemands().size());
        assertEquals("REQ-SMALL", result.getPendingDemands().get(0).getRequestId());
    }

    @Test
    @DisplayName("2. Arrival-order tie-breaking: equal-sized batches prioritize earlier arrival order")
    void shouldTieBreakByArrivalOrder() {
        // Two 20-passenger batches in different corridors
        DemandRequest reqEarly = new DemandRequest("REQ-EARLY", stopA, stopB, 20, 5L);
        DemandRequest reqLate = new DemandRequest("REQ-LATE", stopD, stopB, 20, 15L);

        queueService.enqueue(reqEarly);
        queueService.enqueue(reqLate);

        Bus bus = new StandardBus("BUS-01", 30, depot);
        AllocationResult result = allocationService.allocate(List.of(bus), queueService);

        assertEquals(1, result.getAssignedTripCount());
        assertEquals("REQ-EARLY-boarded", result.getAssignedTrips().get(0).getDemandRequest().getRequestId());
        assertEquals(20, result.getAllocatedPassengerCount());
        assertEquals(20, result.getPendingPassengerCount());
        assertEquals("REQ-LATE", result.getPendingDemands().get(0).getRequestId());
    }

    @Test
    @DisplayName("3. FIFO within identical pickup-destination corridor: earlier batch cannot be overtaken by larger later batch")
    void shouldEnforceFifoWithinSameCorridor() {
        // Same corridor: STOP_A -> STOP_B
        // Batch 1 (t=1, 10 pax)
        DemandRequest req1 = new DemandRequest("REQ-1", stopA, stopB, 10, 1L);
        // Batch 2 (t=2, 50 pax) arrives later in the same corridor
        DemandRequest req2 = new DemandRequest("REQ-2", stopA, stopB, 50, 2L);

        queueService.enqueue(req1);
        queueService.enqueue(req2);

        Bus bus = new StandardBus("BUS-01", 40, depot);
        AllocationResult result = allocationService.allocate(List.of(bus), queueService);

        // Bus must serve req1 first!
        assertEquals(1, result.getAssignedTripCount());
        assertEquals("REQ-1-boarded", result.getAssignedTrips().get(0).getDemandRequest().getRequestId());
        assertEquals(10, result.getAllocatedPassengerCount());
        assertEquals(50, result.getPendingPassengerCount());
        assertEquals("REQ-2", result.getPendingDemands().get(0).getRequestId());
    }

    @Test
    @DisplayName("4. Partial boarding and preservation of remaining passengers with identical arrival order")
    void shouldHandlePartialBoardingAndPreserveArrivalOrder() {
        // 35 passengers waiting, but bus has capacity 20
        DemandRequest req = new DemandRequest("REQ-OVERFLOW", stopA, stopB, 35, 100L);
        queueService.enqueue(req);

        Bus bus1 = new MiniBus("BUS-MINI", 20, depot);
        AllocationResult result1 = allocationService.allocate(List.of(bus1), queueService);

        assertEquals(1, result1.getAssignedTripCount());
        assertEquals(20, result1.getAllocatedPassengerCount());
        assertEquals(15, result1.getPendingPassengerCount());

        // Verify remaining batch retained original arrival order 100L
        DemandRequest remainder = result1.getPendingDemands().get(0);
        assertEquals(15, remainder.getPassengerCount());
        assertEquals(100L, remainder.getOriginalArrivalOrder());

        // Now second bus becomes available and serves the remainder
        Bus bus2 = new StandardBus("BUS-STD", 40, depot);
        AllocationResult result2 = allocationService.allocate(List.of(bus2), queueService);

        assertEquals(1, result2.getAssignedTripCount());
        assertEquals(15, result2.getAllocatedPassengerCount());
        assertEquals(0, result2.getPendingPassengerCount());
        assertTrue(queueService.isEmpty());
    }

    @Test
    @DisplayName("5. No available bus: passengers remain waiting in queue")
    void shouldHandleNoAvailableBuses() {
        queueService.enqueue(new DemandRequest("R1", stopA, stopB, 15, 1L));

        // Empty fleet
        AllocationResult result = allocationService.allocate(List.of(), queueService);
        assertEquals(0, result.getAssignedTripCount());
        assertEquals(0, result.getAllocatedPassengerCount());
        assertEquals(15, result.getPendingPassengerCount());
    }

    @Test
    @DisplayName("6. Bus capacity enforcement: bus occupancy never exceeds seat capacity")
    void shouldEnforceBusCapacityStrictly() {
        DemandRequest req = new DemandRequest("R1", stopA, stopB, 50, 1L);
        queueService.enqueue(req);

        Bus bus = new MiniBus("BUS-01", 15, depot);
        AllocationResult result = allocationService.allocate(List.of(bus), queueService);

        assertEquals(15, bus.getPassengerOccupancy());
        assertEquals(0, bus.getAvailableSeats());
        assertTrue(bus.isFull());
        assertEquals(15, result.getAllocatedPassengerCount());
        assertEquals(35, result.getPendingPassengerCount());
    }

    @Test
    @DisplayName("7. Unreachable pickup stop: demand remains pending when dispatch leg is disconnected")
    void shouldRejectWhenPickupStopIsUnreachable() {
        // isolatedStop has no roads
        DemandRequest req = new DemandRequest("R-UNREACHABLE-PICKUP", isolatedStop, stopB, 10, 1L);
        queueService.enqueue(req);

        Bus bus = new StandardBus("BUS-01", 40, depot);
        AllocationResult result = allocationService.allocate(List.of(bus), queueService);

        assertEquals(0, result.getAssignedTripCount());
        assertEquals(0, result.getAllocatedPassengerCount());
        assertEquals(10, result.getPendingPassengerCount());
        assertEquals(BusStatus.AVAILABLE, bus.getStatus());
        assertEquals(0, bus.getPassengerOccupancy());
    }

    @Test
    @DisplayName("8. Unreachable destination: demand remains pending when transit leg is disconnected")
    void shouldRejectWhenDestinationIsUnreachable() {
        DemandRequest req = new DemandRequest("R-UNREACHABLE-DEST", stopA, isolatedStop, 10, 1L);
        queueService.enqueue(req);

        Bus bus = new StandardBus("BUS-01", 40, depot);
        AllocationResult result = allocationService.allocate(List.of(bus), queueService);

        assertEquals(0, result.getAssignedTripCount());
        assertEquals(10, result.getPendingPassengerCount());
        assertEquals(BusStatus.AVAILABLE, bus.getStatus());
    }

    @Test
    @DisplayName("9. Vehicle-specific road restrictions: MiniBus selected over StandardBus for narrow-road corridors")
    void shouldRespectVehicleSpecificRoadRestrictions() {
        Stop narrowStop = new Stop("STOP_NARROW", "Narrow Alley Stop");
        network.addStop(narrowStop);
        // Only accessible via narrow road from STOP_A
        network.addRoad("STOP_A", "STOP_NARROW", 5.0, true);
        network.addRoad("STOP_NARROW", "STOP_B", 5.0, false);

        DemandRequest req = new DemandRequest("REQ-NARROW", stopA, narrowStop, 12, 1L);
        queueService.enqueue(req);

        // Fleet has a StandardBus and a MiniBus, both available at depot
        Bus standardBus = new StandardBus("BUS-STD", 40, depot);
        Bus miniBus = new MiniBus("BUS-MINI", 15, depot);

        AllocationResult result = allocationService.allocate(List.of(standardBus, miniBus), queueService);

        assertEquals(1, result.getAssignedTripCount());
        ReliefTrip trip = result.getAssignedTrips().get(0);
        // StandardBus cannot traverse narrow road, so MiniBus must be assigned
        assertEquals("BUS-MINI", trip.getAssignedBus().getId());
        assertEquals(BusStatus.IN_TRANSIT, miniBus.getStatus());
        assertEquals(BusStatus.AVAILABLE, standardBus.getStatus());
    }

    @Test
    @DisplayName("10. Bus unavailability and breakdown: maintenance and out-of-service buses are not assigned")
    void shouldSkipUnavailableAndBrokenDownBuses() {
        DemandRequest req = new DemandRequest("R1", stopA, stopB, 20, 1L);
        queueService.enqueue(req);

        Bus brokenBus = new StandardBus("BUS-BROKEN", 40, depot);
        brokenBus.setStatus(BusStatus.MAINTENANCE);

        Bus outOfServiceBus = new StandardBus("BUS-OUT", 40, depot);
        outOfServiceBus.setStatus(BusStatus.OUT_OF_SERVICE);

        AllocationResult result = allocationService.allocate(List.of(brokenBus, outOfServiceBus), queueService);

        assertEquals(0, result.getAssignedTripCount());
        assertEquals(0, result.getAllocatedPassengerCount());
        assertEquals(20, result.getPendingPassengerCount());
    }

    @Test
    @DisplayName("11. Multiple batches and multiple buses: coordinated allocation across corridors")
    void shouldCoordinateMultipleBatchesAndBuses() {
        DemandRequest r1 = new DemandRequest("R1", stopA, stopB, 25, 1L);
        DemandRequest r2 = new DemandRequest("R2", stopD, stopC, 15, 2L);
        DemandRequest r3 = new DemandRequest("R3", stopB, stopC, 30, 3L);

        queueService.enqueue(r1);
        queueService.enqueue(r2);
        queueService.enqueue(r3);

        Bus b1 = new StandardBus("B1", 40, depot);
        Bus b2 = new StandardBus("B2", 40, depot);
        Bus b3 = new MiniBus("B3", 20, depot);

        AllocationResult result = allocationService.allocate(List.of(b1, b2, b3), queueService);

        assertEquals(3, result.getAssignedTripCount());
        assertEquals(70, result.getAllocatedPassengerCount());
        assertEquals(0, result.getPendingPassengerCount());
        assertTrue(queueService.isEmpty());
    }

    @Test
    @DisplayName("12. Passenger accounting: no passenger is silently lost or double-counted")
    void shouldPreserveStrictPassengerAccounting() {
        int initialPassengers = 0;
        for (int i = 1; i <= 5; i++) {
            int count = i * 12; // 12, 24, 36, 48, 60
            initialPassengers += count;
            queueService.enqueue(new DemandRequest("REQ-" + i, stopA, stopB, count, (long) i));
        }
        assertEquals(180, initialPassengers);

        Bus b1 = new StandardBus("B1", 50, depot);
        Bus b2 = new MiniBus("B2", 15, depot);

        AllocationResult result = allocationService.allocate(List.of(b1, b2), queueService);

        int allocated = result.getAllocatedPassengerCount();
        int pending = result.getPendingPassengerCount();

        assertEquals(initialPassengers, allocated + pending, "Total passengers must strictly equal allocated + pending");
        assertEquals(27, allocated);
        assertEquals(153, pending);
    }

    @Test
    @DisplayName("13. Ensuring an already-started trip bus is not reassigned")
    void shouldNotReassignInTransitBus() {
        DemandRequest r1 = new DemandRequest("R1", stopA, stopB, 20, 1L);
        queueService.enqueue(r1);

        Bus inTransitBus = new StandardBus("BUS-BUSY", 40, depot);
        inTransitBus.startTrip(); // IN_TRANSIT
        assertEquals(BusStatus.IN_TRANSIT, inTransitBus.getStatus());

        AllocationResult result = allocationService.allocate(List.of(inTransitBus), queueService);

        assertEquals(0, result.getAssignedTripCount());
        assertEquals(0, result.getAllocatedPassengerCount());
        assertEquals(20, result.getPendingPassengerCount());
    }

    @Test
    @DisplayName("14. Route combination: combines bus-to-pickup and pickup-to-destination without duplicating pickup stop")
    void shouldCombineRouteSegmentsWithoutDuplicatingPickup() {
        List<Stop> dispatchLeg = List.of(depot, stopA);
        List<Stop> transitLeg = List.of(stopA, stopB, stopC);

        List<Stop> combined = AllocationService.combineRoutes(dispatchLeg, transitLeg);

        assertEquals(4, combined.size());
        assertEquals(List.of(depot, stopA, stopB, stopC), combined);

        // When bus is already at pickup stop
        List<Stop> alreadyAtPickup = List.of(stopA);
        List<Stop> combinedSameStop = AllocationService.combineRoutes(alreadyAtPickup, transitLeg);
        assertEquals(3, combinedSameStop.size());
        assertEquals(List.of(stopA, stopB, stopC), combinedSameStop);
    }
}
