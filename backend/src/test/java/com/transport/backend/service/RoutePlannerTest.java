package com.transport.backend.service;

import com.transport.backend.model.*;
import com.transport.backend.network.RoadNetwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoutePlannerTest {

    private RoadNetwork network;
    private RoutePlanner planner;

    private Stop depot;
    private Stop stopA;
    private Stop stopB;
    private Stop stopC;
    private Stop stopD;

    @BeforeEach
    void setUp() {
        network = new RoadNetwork();
        depot = new Stop("DEPOT", "Central Bus Depot");
        stopA = new Stop("STOP_A", "Downtown Station");
        stopB = new Stop("STOP_B", "University Hub");
        stopC = new Stop("STOP_C", "Tech City");
        stopD = new Stop("STOP_D", "West Suburb");

        network.addStop(depot);
        network.addStop(stopA);
        network.addStop(stopB);
        network.addStop(stopC);
        network.addStop(stopD);

        planner = new RoutePlanner(network);
    }

    @Test
    @DisplayName("1. Should choose faster multi-edge route over a direct slower route")
    void shouldPreferFasterMultiEdgeRouteOverDirectRoute() {
        // Direct route: DEPOT -> STOP_C takes 35 mins
        network.addRoad("DEPOT", "STOP_C", 35.0);

        // Multi-edge route: DEPOT -> STOP_A (10 mins) -> STOP_C (8 mins) = 18 mins
        network.addRoad("DEPOT", "STOP_A", 10.0);
        network.addRoad("STOP_A", "STOP_C", 8.0);

        RouteResult result = planner.findShortestRoute("DEPOT", "STOP_C");

        assertTrue(result.isReachable());
        assertEquals(18.0, result.getTotalTravelTimeMinutes(), 1e-6);
        assertEquals(List.of("DEPOT", "STOP_A", "STOP_C"), result.getStopIds());
    }

    @Test
    @DisplayName("2. Should minimize travel time instead of hop-count / distance")
    void shouldOptimizeForTravelTimeInsteadOfDistance() {
        // Route 1 (1 hop, congested): DEPOT -> STOP_B = 50.0 mins
        network.addRoad("DEPOT", "STOP_B", 50.0);

        // Route 2 (3 hops, expressway): DEPOT -> STOP_A (5 mins) -> STOP_D (5 mins) -> STOP_B (5 mins) = 15.0 mins
        network.addRoad("DEPOT", "STOP_A", 5.0);
        network.addRoad("STOP_A", "STOP_D", 5.0);
        network.addRoad("STOP_D", "STOP_B", 5.0);

        RouteResult result = planner.findShortestRoute("DEPOT", "STOP_B");

        assertTrue(result.isReachable());
        assertEquals(15.0, result.getTotalTravelTimeMinutes(), 1e-6);
        assertEquals(List.of("DEPOT", "STOP_A", "STOP_D", "STOP_B"), result.getStopIds());
    }

    @Test
    @DisplayName("3. Source equal to destination should return zero travel time and single stop")
    void shouldHandleSourceEqualToDestination() {
        RouteResult result = planner.findShortestRoute("DEPOT", "DEPOT");

        assertTrue(result.isReachable());
        assertEquals(0.0, result.getTotalTravelTimeMinutes());
        assertEquals(1, result.getStopCount());
        assertEquals(List.of("DEPOT"), result.getStopIds());
    }

    @Test
    @DisplayName("4. Should explicitly report unreachable destination when no path exists")
    void shouldReportUnreachableDestination() {
        // Connect DEPOT -> STOP_A only; STOP_C is isolated
        network.addRoad("DEPOT", "STOP_A", 12.0);

        RouteResult result = planner.findShortestRoute("DEPOT", "STOP_C");

        assertFalse(result.isReachable());
        assertTrue(Double.isInfinite(result.getTotalTravelTimeMinutes()));
        assertTrue(result.getStops().isEmpty());
    }

    @Test
    @DisplayName("5. Should route around a disconnected intermediate stop")
    void shouldRouteAroundDisconnectedIntermediateStop() {
        // Primary route: DEPOT -> STOP_A (4 mins) -> STOP_C (4 mins) = 8 mins
        network.addRoad("DEPOT", "STOP_A", 4.0);
        network.addRoad("STOP_A", "STOP_C", 4.0);

        // Backup route: DEPOT -> STOP_B (6 mins) -> STOP_C (6 mins) = 12 mins
        network.addRoad("DEPOT", "STOP_B", 6.0);
        network.addRoad("STOP_B", "STOP_C", 6.0);

        // Initial query uses primary route
        RouteResult initial = planner.findShortestRoute("DEPOT", "STOP_C");
        assertEquals(List.of("DEPOT", "STOP_A", "STOP_C"), initial.getStopIds());
        assertEquals(8.0, initial.getTotalTravelTimeMinutes(), 1e-6);

        // Disconnect intermediate STOP_A
        network.disconnectStop("STOP_A");

        // Subsequent query dynamically routes via backup route
        RouteResult rerouted = planner.findShortestRoute("DEPOT", "STOP_C");
        assertTrue(rerouted.isReachable());
        assertEquals(List.of("DEPOT", "STOP_B", "STOP_C"), rerouted.getStopIds());
        assertEquals(12.0, rerouted.getTotalTravelTimeMinutes(), 1e-6);
    }

    @Test
    @DisplayName("6. Traffic travel-time update should change the optimal route")
    void shouldAdaptToTrafficTravelTimeUpdates() {
        // Route 1: DEPOT -> STOP_A (5 mins) -> STOP_C (5 mins) = 10 mins
        network.addRoad("DEPOT", "STOP_A", 5.0);
        network.addRoad("STOP_A", "STOP_C", 5.0);

        // Route 2: DEPOT -> STOP_B (7 mins) -> STOP_C (7 mins) = 14 mins
        network.addRoad("DEPOT", "STOP_B", 7.0);
        network.addRoad("STOP_B", "STOP_C", 7.0);

        RouteResult before = planner.findShortestRoute("DEPOT", "STOP_C");
        assertEquals(List.of("DEPOT", "STOP_A", "STOP_C"), before.getStopIds());
        assertEquals(10.0, before.getTotalTravelTimeMinutes());

        // Dynamic traffic incident on DEPOT -> STOP_A: travel time surges from 5 to 25 mins
        network.updateTravelTime("DEPOT", "STOP_A", 25.0);

        // Route 1 is now 25 + 5 = 30 mins, so Route 2 (14 mins) becomes optimal
        RouteResult after = planner.findShortestRoute("DEPOT", "STOP_C");
        assertEquals(List.of("DEPOT", "STOP_B", "STOP_C"), after.getStopIds());
        assertEquals(14.0, after.getTotalTravelTimeMinutes(), 1e-6);
    }

    @Test
    @DisplayName("7. MiniBus and StandardBus should select different routes due to narrow road policy")
    void shouldEnforceVehicleSpecificRoadRestrictions() {
        Stop hospital = new Stop("HOSPITAL", "City Hospital");
        network.addStop(hospital);

        // Route 1 (Narrow shortcut alley): DEPOT -> STOP_A (3 mins) -> HOSPITAL (3 mins) = 6 mins (Narrow)
        network.addRoad("DEPOT", "STOP_A", 3.0, false);
        network.addRoad("STOP_A", "HOSPITAL", 3.0, true); // NARROW ROAD

        // Route 2 (Wide boulevard bypass): DEPOT -> STOP_B (8 mins) -> HOSPITAL (8 mins) = 16 mins (Standard)
        network.addRoad("DEPOT", "STOP_B", 8.0, false);
        network.addRoad("STOP_B", "HOSPITAL", 8.0, false);

        Bus miniBus = new MiniBus("MINI-01", depot);
        Bus standardBus = new StandardBus("STD-01", depot);

        // MiniBus can navigate narrow roads: takes the 6 min shortcut
        RouteResult miniBusRoute = planner.findShortestRoute("DEPOT", "HOSPITAL", miniBus);
        assertTrue(miniBusRoute.isReachable());
        assertEquals(6.0, miniBusRoute.getTotalTravelTimeMinutes(), 1e-6);
        assertEquals(List.of("DEPOT", "STOP_A", "HOSPITAL"), miniBusRoute.getStopIds());

        // StandardBus cannot use narrow roads: takes the 16 min bypass
        RouteResult standardBusRoute = planner.findShortestRoute("DEPOT", "HOSPITAL", standardBus);
        assertTrue(standardBusRoute.isReachable());
        assertEquals(16.0, standardBusRoute.getTotalTravelTimeMinutes(), 1e-6);
        assertEquals(List.of("DEPOT", "STOP_B", "HOSPITAL"), standardBusRoute.getStopIds());
    }

    @Test
    @DisplayName("8. Should reject invalid source or destination inputs")
    void shouldRejectInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> planner.findShortestRoute(null, "STOP_A"));
        assertThrows(IllegalArgumentException.class, () -> planner.findShortestRoute("DEPOT", null));
        assertThrows(IllegalArgumentException.class, () -> planner.findShortestRoute("   ", "STOP_A"));
        assertThrows(IllegalArgumentException.class, () -> planner.findShortestRoute("DEPOT", "   "));
        assertThrows(IllegalArgumentException.class, () -> planner.findShortestRoute("UNKNOWN_ORIGIN", "STOP_A"));
        assertThrows(IllegalArgumentException.class, () -> planner.findShortestRoute("DEPOT", "UNKNOWN_DEST"));
    }

    @Test
    @DisplayName("9. Should maintain deterministic route selection for equal-cost routes")
    void shouldBeDeterministicForEqualCostRoutes() {
        // Two alternative paths with identical travel times (10.0 mins)
        // Path A: DEPOT -> STOP_A (5 mins) -> STOP_C (5 mins) = 10 mins
        // Path B: DEPOT -> STOP_B (5 mins) -> STOP_C (5 mins) = 10 mins
        network.addRoad("DEPOT", "STOP_A", 5.0);
        network.addRoad("STOP_A", "STOP_C", 5.0);

        network.addRoad("DEPOT", "STOP_B", 5.0);
        network.addRoad("STOP_B", "STOP_C", 5.0);

        // Repeated queries should return identical deterministic route
        RouteResult firstRun = planner.findShortestRoute("DEPOT", "STOP_C");
        assertTrue(firstRun.isReachable());
        assertEquals(10.0, firstRun.getTotalTravelTimeMinutes(), 1e-6);

        for (int i = 0; i < 10; i++) {
            RouteResult repeatedRun = planner.findShortestRoute("DEPOT", "STOP_C");
            assertEquals(firstRun.getStopIds(), repeatedRun.getStopIds(), "Equal-cost route selection must be 100% deterministic");
        }
    }

    @Test
    @DisplayName("10. Closed roads must be strictly excluded from Dijkstra shortest route")
    void shouldExcludeClosedRoadsFromRouteCalculation() {
        // Fast direct path: DEPOT -> STOP_C (8.0 min)
        network.addRoad("DEPOT", "STOP_C", 8.0);

        // Detour path: DEPOT -> STOP_A (4.0 min) -> STOP_C (6.0 min) = 10.0 min
        network.addRoad("DEPOT", "STOP_A", 4.0);
        network.addRoad("STOP_A", "STOP_C", 6.0);

        // Initially uses fast direct path
        RouteResult initial = planner.findShortestRoute("DEPOT", "STOP_C");
        assertTrue(initial.isReachable());
        assertEquals(List.of("DEPOT", "STOP_C"), initial.getStopIds());
        assertEquals(8.0, initial.getTotalTravelTimeMinutes(), 1e-6);

        // Close the direct road
        network.setRoadOpen("DEPOT", "STOP_C", false);

        // Dijkstra must exclude the closed road and route via STOP_A
        RouteResult rerouted = planner.findShortestRoute("DEPOT", "STOP_C");
        assertTrue(rerouted.isReachable());
        assertEquals(List.of("DEPOT", "STOP_A", "STOP_C"), rerouted.getStopIds());
        assertEquals(10.0, rerouted.getTotalTravelTimeMinutes(), 1e-6);
    }

    @Test
    @DisplayName("11. Should report unreachable when the only connecting road is closed")
    void shouldReportUnreachableWhenOnlyConnectingRoadIsClosed() {
        // Only connection to STOP_B is DEPOT -> STOP_B
        network.addRoad("DEPOT", "STOP_B", 12.0);

        RouteResult beforeClosure = planner.findShortestRoute("DEPOT", "STOP_B");
        assertTrue(beforeClosure.isReachable());

        // Close the road
        network.setRoadOpen("DEPOT", "STOP_B", false);

        RouteResult afterClosure = planner.findShortestRoute("DEPOT", "STOP_B");
        assertFalse(afterClosure.isReachable(), "Destination must be unreachable when connecting road is closed");
        assertTrue(Double.isInfinite(afterClosure.getTotalTravelTimeMinutes()));
        assertTrue(afterClosure.getStops().isEmpty());
    }

    @Test
    @DisplayName("12. Reopening a closed road must immediately restore its usability")
    void shouldRestoreRouteWhenRoadIsReopened() {
        // Fast road: DEPOT -> STOP_C (7.0 min)
        network.addRoad("DEPOT", "STOP_C", 7.0);

        // Slow detour: DEPOT -> STOP_D (10.0 min) -> STOP_C (10.0 min) = 20.0 min
        network.addRoad("DEPOT", "STOP_D", 10.0);
        network.addRoad("STOP_D", "STOP_C", 10.0);

        // 1. Close fast road
        network.setRoadOpen("DEPOT", "STOP_C", false);
        RouteResult closedResult = planner.findShortestRoute("DEPOT", "STOP_C");
        assertEquals(List.of("DEPOT", "STOP_D", "STOP_C"), closedResult.getStopIds());
        assertEquals(20.0, closedResult.getTotalTravelTimeMinutes(), 1e-6);

        // 2. Reopen fast road
        network.setRoadOpen("DEPOT", "STOP_C", true);
        RouteResult reopenedResult = planner.findShortestRoute("DEPOT", "STOP_C");
        assertEquals(List.of("DEPOT", "STOP_C"), reopenedResult.getStopIds());
        assertEquals(7.0, reopenedResult.getTotalTravelTimeMinutes(), 1e-6);
    }

    @Test
    @DisplayName("13. Should record high-resolution latency and rolling benchmark metrics")
    void shouldRecordPerformanceTelemetryAndBenchmarkMetrics() {
        network.addRoad("DEPOT", "STOP_A", 10.0);
        network.addRoad("STOP_A", "STOP_C", 8.0);

        RouteResult result = planner.findShortestRoute("DEPOT", "STOP_C");
        assertTrue(result.getExecutionTimeNanos() > 0, "Execution time in nanoseconds must be recorded and positive");
        assertTrue(result.getExecutionTimeMicros() >= 0.0);

        var benchmark = planner.getBenchmarkMetrics();
        assertNotNull(benchmark);
        assertTrue(benchmark.totalCalculations() >= 1);
        assertTrue(benchmark.averageLatencyMicros() >= 0.0);
    }
}
