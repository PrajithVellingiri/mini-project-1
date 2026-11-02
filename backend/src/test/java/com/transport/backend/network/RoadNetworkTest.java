package com.transport.backend.network;

import com.transport.backend.model.Road;
import com.transport.backend.model.Stop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoadNetworkTest {

    private RoadNetwork network;
    private Stop stop1;
    private Stop stop2;
    private Stop stop3;

    @BeforeEach
    void setUp() {
        network = new RoadNetwork();
        stop1 = new Stop("S1", "Main Terminal");
        stop2 = new Stop("S2", "Central Market");
        stop3 = new Stop("S3", "Tech Park");
    }

    @Test
    @DisplayName("Should add stops and reject duplicate stop IDs")
    void shouldAddStopsAndRejectDuplicates() {
        network.addStop(stop1);
        network.addStop(stop2);

        assertEquals(2, network.getStopCount());
        assertTrue(network.containsStop("S1"));
        assertTrue(network.containsStop("S2"));
        assertFalse(network.containsStop("S3"));

        // Reject duplicate stop ID
        Stop duplicateStop = new Stop("S1", "Duplicate Name");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> network.addStop(duplicateStop));
        assertTrue(ex.getMessage().contains("already exists"));

        // Reject null stop
        assertThrows(IllegalArgumentException.class, () -> network.addStop(null));
    }

    @Test
    @DisplayName("Should validate road travel time is strictly positive (> 0)")
    void shouldEnforcePositiveTravelTimes() {
        network.addStop(stop1);
        network.addStop(stop2);

        // Positive travel time is allowed
        Road road = network.addRoad("S1", "S2", 15.5);
        assertNotNull(road);
        assertEquals(15.5, network.getTravelTime("S1", "S2"));

        // Non-positive travel times must be rejected
        assertThrows(IllegalArgumentException.class, () -> network.addRoad("S2", "S1", 0.0));
        assertThrows(IllegalArgumentException.class, () -> network.addRoad("S2", "S1", -4.2));

        // Self-loops must be rejected
        assertThrows(IllegalArgumentException.class, () -> network.addRoad("S1", "S1", 5.0));

        // Roads with non-existent stops must be rejected
        assertThrows(IllegalArgumentException.class, () -> network.addRoad("S1", "UNKNOWN", 10.0));
    }

    @Test
    @DisplayName("Should reject duplicate roads between the same origin and destination")
    void shouldRejectDuplicateRoads() {
        network.addStop(stop1);
        network.addStop(stop2);

        network.addRoad("S1", "S2", 12.0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> network.addRoad("S1", "S2", 18.0));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Should update travel times accurately and reject non-positive updates")
    void shouldUpdateTravelTimes() {
        network.addStop(stop1);
        network.addStop(stop2);
        network.addRoad("S1", "S2", 10.0);

        assertEquals(10.0, network.getTravelTime("S1", "S2"));

        // Dynamic traffic update increases travel time
        network.updateTravelTime("S1", "S2", 22.5);
        assertEquals(22.5, network.getTravelTime("S1", "S2"));

        // Non-positive update rejected
        assertThrows(IllegalArgumentException.class, () -> network.updateTravelTime("S1", "S2", 0.0));
        assertThrows(IllegalArgumentException.class, () -> network.updateTravelTime("S1", "S2", -1.0));

        // Updating non-existent road rejected
        assertThrows(IllegalArgumentException.class, () -> network.updateTravelTime("S2", "S1", 15.0));
    }

    @Test
    @DisplayName("Should inspect neighbors, adjacent edges, and support disconnected stops")
    void shouldSupportNeighborsAndDisconnectedStops() {
        network.addStop(stop1);
        network.addStop(stop2);
        network.addStop(stop3);

        // S3 is initially disconnected
        assertTrue(network.isStopDisconnected("S3"), "S3 has no edges and should be disconnected");
        assertTrue(network.getAdjacentRoads("S3").isEmpty());

        // Connect S1 -> S2, S2 -> S3
        network.addRoad("S1", "S2", 8.0);
        network.addRoad("S2", "S3", 6.0);

        assertFalse(network.isStopDisconnected("S3"), "S3 now has an incoming road from S2");

        Set<String> s1Neighbors = network.getNeighborStopIds("S1");
        assertEquals(1, s1Neighbors.size());
        assertTrue(s1Neighbors.contains("S2"));

        Collection<Road> s1Edges = network.getAdjacentRoads("S1");
        assertEquals(1, s1Edges.size());

        // Disconnect S2 from the network
        network.disconnectStop("S2");
        assertTrue(network.isStopDisconnected("S2"));
        assertTrue(network.getNeighborStopIds("S2").isEmpty());

        // S1 should no longer have S2 as neighbor
        assertTrue(network.getNeighborStopIds("S1").isEmpty());

        // S3 is once again disconnected because the only road into it was from S2
        assertTrue(network.isStopDisconnected("S3"));
    }

    @Test
    @DisplayName("Should toggle road open status and verify availability accurately")
    void shouldToggleRoadOpenStatusAndVerifyAvailability() {
        network.addStop(stop1);
        network.addStop(stop2);
        network.addRoad("S1", "S2", 14.0);

        // Road is open by default
        assertTrue(network.isRoadOpen("S1", "S2"));

        // Close the road
        network.setRoadOpen("S1", "S2", false);
        assertFalse(network.isRoadOpen("S1", "S2"));
        assertTrue(network.getRoad("S1", "S2").isClosed());

        // Reopen the road
        network.setRoadOpen("S1", "S2", true);
        assertTrue(network.isRoadOpen("S1", "S2"));
        assertTrue(network.getRoad("S1", "S2").isOpen());

        // Attempting to toggle non-existent road throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> network.setRoadOpen("S1", "S3", false));
        assertThrows(IllegalArgumentException.class, () -> network.isRoadOpen("S1", "S3"));
    }
}
