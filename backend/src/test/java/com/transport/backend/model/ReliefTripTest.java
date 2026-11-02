package com.transport.backend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReliefTripTest {

    private Stop depot;
    private Stop stopA;
    private Stop stopB;
    private Bus bus;
    private DemandRequest demand;

    @BeforeEach
    void setUp() {
        depot = new Stop("DEPOT", "Central Depot");
        stopA = new Stop("STOP_A", "Station A");
        stopB = new Stop("STOP_B", "Station B");
        bus = new StandardBus("BUS-10", depot);
        demand = new DemandRequest("DEM-01", stopA, stopB, 20, 1L);
    }

    @Test
    @DisplayName("Should track bus, demand, route, travel time, and lifecycle transitions")
    void shouldTrackTripLifecycleTransitions() {
        List<Stop> route = List.of(depot, stopA, stopB);
        ReliefTrip trip = new ReliefTrip("TRIP-001", bus, demand, route, 35.0);

        assertEquals("TRIP-001", trip.getTripId());
        assertEquals(bus, trip.getAssignedBus());
        assertEquals(demand, trip.getDemandRequest());
        assertEquals(3, trip.getRoute().size());
        assertEquals(35.0, trip.getEstimatedTravelTimeMinutes());
        assertEquals(TripStatus.SCHEDULED, trip.getStatus());
        assertEquals(BusStatus.AVAILABLE, bus.getStatus());

        // Start Trip
        trip.startTrip();
        assertEquals(TripStatus.IN_PROGRESS, trip.getStatus());
        assertEquals(BusStatus.IN_TRANSIT, bus.getStatus());

        // Cannot start already running trip
        assertThrows(IllegalStateException.class, trip::startTrip);

        // Complete Trip
        trip.completeTrip();
        assertEquals(TripStatus.COMPLETED, trip.getStatus());
        assertEquals(BusStatus.AVAILABLE, bus.getStatus());
        assertEquals(stopB, bus.getCurrentStop(), "Bus must be positioned at terminal stop upon completion");

        // Cannot cancel completed trip
        assertThrows(IllegalStateException.class, trip::cancelTrip);
    }

    @Test
    @DisplayName("Should handle trip cancellation and release bus")
    void shouldHandleTripCancellation() {
        ReliefTrip trip = new ReliefTrip("TRIP-002", bus, demand, List.of(depot, stopA), 15.0);
        trip.startTrip();
        assertEquals(BusStatus.IN_TRANSIT, bus.getStatus());

        trip.cancelTrip();
        assertEquals(TripStatus.CANCELLED, trip.getStatus());
        assertEquals(BusStatus.AVAILABLE, bus.getStatus());
    }
}
