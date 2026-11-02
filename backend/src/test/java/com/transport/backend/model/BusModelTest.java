package com.transport.backend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusModelTest {

    private Stop centralDepot;
    private Stop stationA;
    private Stop stationB;

    @BeforeEach
    void setUp() {
        centralDepot = new Stop("DEPOT", "Central Bus Depot");
        stationA = new Stop("STOP_A", "Station Square");
        stationB = new Stop("STOP_B", "Market Crossing");
    }

    @Test
    @DisplayName("Should enforce bus capacity and prevent over-occupancy")
    void shouldEnforceBusCapacity() {
        Bus bus = new MiniBus("BUS-01", 15, centralDepot);
        assertEquals(15, bus.getCapacity());
        assertEquals(0, bus.getPassengerOccupancy());
        assertEquals(15, bus.getAvailableSeats());
        assertFalse(bus.isFull());

        // Board within limit
        bus.boardPassengers(10);
        assertEquals(10, bus.getPassengerOccupancy());
        assertEquals(5, bus.getAvailableSeats());

        // Fill remaining
        bus.boardPassengers(5);
        assertEquals(15, bus.getPassengerOccupancy());
        assertEquals(0, bus.getAvailableSeats());
        assertTrue(bus.isFull());

        // Exceed capacity should throw IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> bus.boardPassengers(1));
        assertTrue(ex.getMessage().contains("Cannot board 1 passengers"));
    }

    @Test
    @DisplayName("Should reject invalid passenger counts (negative or zero)")
    void shouldRejectInvalidPassengerCounts() {
        Bus bus = new StandardBus("BUS-02", 40, centralDepot);

        assertThrows(IllegalArgumentException.class, () -> bus.boardPassengers(0));
        assertThrows(IllegalArgumentException.class, () -> bus.boardPassengers(-5));

        bus.boardPassengers(10);
        assertThrows(IllegalArgumentException.class, () -> bus.alightPassengers(0));
        assertThrows(IllegalArgumentException.class, () -> bus.alightPassengers(-3));
        assertThrows(IllegalArgumentException.class, () -> bus.alightPassengers(11)); // more than on board

        bus.alightPassengers(4);
        assertEquals(6, bus.getPassengerOccupancy());

        bus.clearPassengers();
        assertEquals(0, bus.getPassengerOccupancy());
    }

    @Test
    @DisplayName("Should validate valid bus lifecycle status transitions")
    void shouldTransitionBusStatusesCorrectly() {
        Bus bus = new StandardBus("BUS-03", centralDepot);
        assertEquals(BusStatus.AVAILABLE, bus.getStatus());
        assertEquals(centralDepot, bus.getCurrentStop());

        // Available -> In Transit
        bus.startTrip();
        assertEquals(BusStatus.IN_TRANSIT, bus.getStatus());

        // Cannot start trip again while already in transit
        assertThrows(IllegalStateException.class, bus::startTrip);

        // In Transit -> Available on completion
        bus.completeTrip(stationA);
        assertEquals(BusStatus.AVAILABLE, bus.getStatus());
        assertEquals(stationA, bus.getCurrentStop());

        // Cannot complete trip if already available
        assertThrows(IllegalStateException.class, () -> bus.completeTrip(stationB));

        // Maintenance state
        bus.setStatus(BusStatus.MAINTENANCE);
        assertThrows(IllegalStateException.class, bus::startTrip);
        assertThrows(IllegalStateException.class, () -> bus.boardPassengers(5));

        // Release bus
        bus.releaseBus(stationB);
        assertEquals(BusStatus.AVAILABLE, bus.getStatus());
        assertEquals(stationB, bus.getCurrentStop());
        assertEquals(0, bus.getPassengerOccupancy());
    }

    @Test
    @DisplayName("Should test subclass inheritance and polymorphic road restriction policy")
    void shouldApplyPolymorphicRoadRestrictions() {
        Road highway = new Road("DEPOT", "STOP_A", 12.0, false);
        Road narrowLane = new Road("STOP_A", "STOP_B", 8.0, true);

        Bus miniBus = new MiniBus("MINI-01", centralDepot);
        Bus standardBus = new StandardBus("STD-01", centralDepot);

        // Subclass checks
        assertInstanceOf(Bus.class, miniBus);
        assertInstanceOf(Bus.class, standardBus);

        // MiniBus policy: Allowed on all roads including narrow lanes
        assertTrue(miniBus.canUseRoad(highway), "MiniBus should be allowed on regular highway");
        assertTrue(miniBus.canUseRoad(narrowLane), "MiniBus should be allowed on narrow road");

        // StandardBus policy: Allowed on standard roads, blocked on narrow lanes
        assertTrue(standardBus.canUseRoad(highway), "StandardBus should be allowed on highway");
        assertFalse(standardBus.canUseRoad(narrowLane), "StandardBus must NOT be allowed on narrow road");
        assertFalse(standardBus.canUseRoad(null), "StandardBus handles null road gracefully");
    }
}
