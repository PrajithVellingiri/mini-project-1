package com.transport.backend.controller;

import com.transport.backend.service.SimulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.transport.backend.config.UserSeederService;
import com.transport.backend.entity.UserEntity;
import com.transport.backend.repository.UserRepository;
import com.transport.backend.security.CustomUserDetails;
import com.transport.backend.security.JwtService;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class SimulationControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSeederService userSeederService;

    private MockMvc mockMvc;

    @BeforeEach
    void resetState() {
        userSeederService.seedDemoUsersIfEnabled();
        UserEntity admin = userRepository.findByEmailIgnoreCase("admin@smarttransport.demo").orElseThrow();
        String adminToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(admin));

        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .defaultRequest(get("/").header("Authorization", adminToken))
                .build();

        simulationService.resetSimulation();
    }

    @Test
    @DisplayName("1. State endpoint returns the seeded simulation")
    void shouldReturnSeededSimulationState() throws Exception {
        mockMvc.perform(get("/api/simulation/state").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalStops").value(13))
                .andExpect(jsonPath("$.summary.totalBuses").value(4))
                .andExpect(jsonPath("$.summary.availableBuses").value(4))
                .andExpect(jsonPath("$.summary.waitingDemandBatches").value(22))
                .andExpect(jsonPath("$.summary.waitingPassengers").value(520))
                .andExpect(jsonPath("$.summary.allocatedPassengers").value(0))
                .andExpect(jsonPath("$.summary.activeTripsCount").value(0))
                .andExpect(jsonPath("$.stops", hasSize(13)))
                .andExpect(jsonPath("$.buses", hasSize(4)))
                .andExpect(jsonPath("$.pendingDemands", hasSize(22)));
    }

    @Test
    @DisplayName("2. Dataset has 10–15 stops, 3–5 buses, one depot, and at least 20 demand batches")
    void shouldVerifyDatasetScaleConstraints() throws Exception {
        mockMvc.perform(get("/api/simulation/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalStops", allOf(greaterThanOrEqualTo(10), lessThanOrEqualTo(15))))
                .andExpect(jsonPath("$.summary.totalBuses", allOf(greaterThanOrEqualTo(3), lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.stops[?(@.id == 'DEPOT')].name").value("Central Transit Depot"))
                .andExpect(jsonPath("$.summary.waitingDemandBatches", greaterThanOrEqualTo(20)));
    }

    @Test
    @DisplayName("3. Running the simulation returns real allocation results")
    void shouldRunSimulationAndReturnRealAllocations() throws Exception {
        mockMvc.perform(post("/api/simulation/run").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTripCount", greaterThan(0)))
                .andExpect(jsonPath("$.allocatedPassengerCount", greaterThan(0)))
                .andExpect(jsonPath("$.assignedTrips", not(empty())));
    }

    @Test
    @DisplayName("4. Passenger accounting is strictly consistent across allocated and pending demand")
    void shouldPreservePassengerAccounting() throws Exception {
        int initialTotal = simulationService.getTotalInitialPassengers();

        mockMvc.perform(post("/api/simulation/run"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    int allocated = simulationService.getTotalAllocatedPassengers();
                    int pending = simulationService.getDemandQueueService().getTotalWaitingPassengers();
                    org.junit.jupiter.api.Assertions.assertEquals(initialTotal, allocated + pending,
                            "Total passengers must strictly equal allocated + pending");
                });
    }

    @Test
    @DisplayName("5. Repeated runs do not duplicate or lose passengers")
    void shouldHandleRepeatedRunsWithoutDuplicatingOrLosingDemand() throws Exception {
        int initialTotal = simulationService.getTotalInitialPassengers();

        // Pass 1: Allocates available buses
        mockMvc.perform(post("/api/simulation/run")).andExpect(status().isOk());
        int allocatedPass1 = simulationService.getTotalAllocatedPassengers();
        int pendingPass1 = simulationService.getDemandQueueService().getTotalWaitingPassengers();
        org.junit.jupiter.api.Assertions.assertEquals(initialTotal, allocatedPass1 + pendingPass1);

        // Pass 2: Fleet is now IN_TRANSIT, so no duplicate trips should be created
        mockMvc.perform(post("/api/simulation/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTripCount").value(0))
                .andExpect(jsonPath("$.allocatedPassengerCount").value(0));

        int allocatedPass2 = simulationService.getTotalAllocatedPassengers();
        int pendingPass2 = simulationService.getDemandQueueService().getTotalWaitingPassengers();
        org.junit.jupiter.api.Assertions.assertEquals(allocatedPass1, allocatedPass2);
        org.junit.jupiter.api.Assertions.assertEquals(pendingPass1, pendingPass2);
        org.junit.jupiter.api.Assertions.assertEquals(initialTotal, allocatedPass2 + pendingPass2);
    }

    @Test
    @DisplayName("6. Reset restores the initial deterministic scenario")
    void shouldResetToInitialDeterministicScenario() throws Exception {
        // Run simulation to change state
        mockMvc.perform(post("/api/simulation/run")).andExpect(status().isOk());

        // Call reset endpoint
        mockMvc.perform(post("/api/simulation/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.allocatedPassengers").value(0))
                .andExpect(jsonPath("$.summary.waitingDemandBatches").value(22))
                .andExpect(jsonPath("$.summary.waitingPassengers").value(520))
                .andExpect(jsonPath("$.summary.activeTripsCount").value(0))
                .andExpect(jsonPath("$.summary.availableBuses").value(4));
    }

    @Test
    @DisplayName("7. Route endpoint returns the expected path and travel time")
    void shouldCalculateShortestRouteViaApi() throws Exception {
        mockMvc.perform(get("/api/simulation/routes")
                        .param("source", "DEPOT")
                        .param("destination", "S4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reachable").value(true))
                .andExpect(jsonPath("$.travelTimeMinutes").value(21.0)) // DEPOT -> S2 (10) -> S4 (11) = 21
                .andExpect(jsonPath("$.stopIds", contains("DEPOT", "S2", "S4")));
    }

    @Test
    @DisplayName("8. Invalid stop IDs and invalid travel-time updates return appropriate errors")
    void shouldHandleInvalidParametersGracefully() throws Exception {
        // Invalid origin stop
        mockMvc.perform(get("/api/simulation/routes")
                        .param("source", "INVALID_STOP")
                        .param("destination", "S4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));

        // Invalid travel time (<= 0)
        String negativeTravelTimeBody = """
                {
                    "fromStopId": "S1",
                    "toStopId": "S2",
                    "travelTimeMinutes": -5.0
                }
                """;
        mockMvc.perform(patch("/api/simulation/roads/travel-time")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(negativeTravelTimeBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("9. Existing health endpoint remains functional")
    void shouldVerifyHealthEndpointRemainsFunctional() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("Smart Public Transportation Route Optimization"));
    }

    @Test
    @DisplayName("10. Complete specific trip releases bus occupancy and updates position")
    void shouldCompleteSpecificTripAndReleaseBus() throws Exception {
        // Run simulation to allocate trips
        mockMvc.perform(post("/api/simulation/run")).andExpect(status().isOk());

        // TRIP-0001 uses BUS-04
        mockMvc.perform(post("/api/simulation/trips/TRIP-0001/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buses[?(@.id == 'BUS-04')].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.buses[?(@.id == 'BUS-04')].passengerOccupancy").value(0))
                .andExpect(jsonPath("$.buses[?(@.id == 'BUS-04')].availableSeats").value(15))
                .andExpect(jsonPath("$.trips[?(@.tripId == 'TRIP-0001')].status").value("COMPLETED"));

        // Duplicate completion must return 400 Bad Request
        mockMvc.perform(post("/api/simulation/trips/TRIP-0001/complete"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));

        // Non-existent trip must return 404 Not Found
        mockMvc.perform(post("/api/simulation/trips/NON_EXISTENT_TRIP/complete"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("11. Dynamically create passenger demand batch and verify queue enqueue")
    void shouldCreatePassengerDemandBatchSuccessfully() throws Exception {
        String demandJson = """
                {
                    "pickupStopId": "S1",
                    "destinationStopId": "S4",
                    "passengerCount": 35
                }
                """;

        mockMvc.perform(post("/api/simulation/demand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pickupStop").value("S1"))
                .andExpect(jsonPath("$.destinationStop").value("S4"))
                .andExpect(jsonPath("$.passengerCount").value(35))
                .andExpect(jsonPath("$.requestId").exists());

        // Verify state reflects the new demand batch
        mockMvc.perform(get("/api/simulation/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.waitingDemandBatches").value(23))
                .andExpect(jsonPath("$.summary.waitingPassengers").value(555));
    }

    @Test
    @DisplayName("12. Update road open status via PATCH /api/simulation/roads/status and verify route avoidance")
    void shouldUpdateRoadStatusAndVerifyRouteAvoidance() throws Exception {
        // Initially, route from DEPOT to S1 takes direct link (DEPOT -> S1, 8.0 min)
        mockMvc.perform(get("/api/simulation/routes")
                        .param("source", "DEPOT")
                        .param("destination", "S1")
                        .param("vehicleType", "GENERAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reachable").value(true))
                .andExpect(jsonPath("$.stopIds[0]").value("DEPOT"))
                .andExpect(jsonPath("$.stopIds[1]").value("S1"))
                .andExpect(jsonPath("$.travelTimeMinutes").value(8.0));

        // 1. Close road DEPOT -> S1 (and reverse)
        String closeJson = """
                {
                    "fromStopId": "DEPOT",
                    "toStopId": "S1",
                    "open": false,
                    "bidirectional": true
                }
                """;

        mockMvc.perform(patch("/api/simulation/roads/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(false))
                .andExpect(jsonPath("$.fromStopId").value("DEPOT"))
                .andExpect(jsonPath("$.toStopId").value("S1"));

        // Verify route calculation now detours around the closed road
        mockMvc.perform(get("/api/simulation/routes")
                        .param("source", "DEPOT")
                        .param("destination", "S1")
                        .param("vehicleType", "GENERAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reachable").value(true))
                .andExpect(jsonPath("$.stopIds[0]").value("DEPOT"))
                .andExpect(jsonPath("$.travelTimeMinutes").value(greaterThan(8.0)))
                .andExpect(jsonPath("$.stopIds.length()").value(greaterThan(2)));

        // 2. Reopen road DEPOT -> S1
        String reopenJson = """
                {
                    "fromStopId": "DEPOT",
                    "toStopId": "S1",
                    "open": true,
                    "bidirectional": true
                }
                """;

        mockMvc.perform(patch("/api/simulation/roads/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopenJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(true));

        // Verify route restored to direct DEPOT -> S1
        mockMvc.perform(get("/api/simulation/routes")
                        .param("source", "DEPOT")
                        .param("destination", "S1")
                        .param("vehicleType", "GENERAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.travelTimeMinutes").value(8.0))
                .andExpect(jsonPath("$.stopIds[1]").value("S1"));
    }

    @Test
    @DisplayName("13. Update road status via path parameters PATCH /api/simulation/roads/{fromId}/{toId}/status")
    void shouldUpdateRoadStatusViaPathParams() throws Exception {
        mockMvc.perform(patch("/api/simulation/roads/S3/S4/status")
                        .param("open", "false")
                        .param("bidirectional", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromStopId").value("S3"))
                .andExpect(jsonPath("$.toStopId").value("S4"))
                .andExpect(jsonPath("$.open").value(false));

        // Reopen via path
        mockMvc.perform(patch("/api/simulation/roads/S3/S4/status")
                        .param("open", "true")
                        .param("bidirectional", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(true));
    }

    @Test
    @DisplayName("14. Validate input constraints on dynamic demand creation")
    void shouldValidateDynamicDemandInputConstraints() throws Exception {
        // 1. Identical pickup and destination stops
        String identicalStopsJson = """
                {
                    "pickupStopId": "S1",
                    "destinationStopId": "S1",
                    "passengerCount": 20
                }
                """;
        mockMvc.perform(post("/api/simulation/demand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(identicalStopsJson))
                .andExpect(status().isBadRequest());

        // 2. Zero or negative passenger count
        String nonPositivePaxJson = """
                {
                    "pickupStopId": "S1",
                    "destinationStopId": "S2",
                    "passengerCount": 0
                }
                """;
        mockMvc.perform(post("/api/simulation/demand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nonPositivePaxJson))
                .andExpect(status().isBadRequest());

        // 3. Non-existent pickup stop ID
        String nonExistentStopJson = """
                {
                    "pickupStopId": "NON_EXISTENT",
                    "destinationStopId": "S2",
                    "passengerCount": 15
                }
                """;
        mockMvc.perform(post("/api/simulation/demand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nonExistentStopJson))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("15. Verify GET /api/simulation/analytics returns genuine passenger conservation and metrics")
    void shouldReturnComprehensiveAnalytics() throws Exception {
        mockMvc.perform(get("/api/simulation/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passengers.totalPassengers").value(520))
                .andExpect(jsonPath("$.passengers.waitingPassengers").value(520))
                .andExpect(jsonPath("$.passengers.inTransitPassengers").value(0))
                .andExpect(jsonPath("$.passengers.completedPassengers").value(0))
                .andExpect(jsonPath("$.passengers.conservationValid").value(true))
                .andExpect(jsonPath("$.fleet.totalBuses").value(4))
                .andExpect(jsonPath("$.fleet.availableBuses").value(4))
                .andExpect(jsonPath("$.fleet.totalFleetCapacity").value(120))
                .andExpect(jsonPath("$.demand.totalWaitingBatches").value(22))
                .andExpect(jsonPath("$.demand.topCorridors").isArray())
                .andExpect(jsonPath("$.telemetry").exists());
    }

    @Test
    @DisplayName("16. Bus breakdown workflow cancels active trip, de-boards passengers to emergency queue, and preserves conservation")
    void shouldHandleBusBreakdownAndRecoveryWorkflow() throws Exception {
        // 1. Run simulation to dispatch buses on active trips
        mockMvc.perform(post("/api/simulation/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTripCount").value(4));

        // 2. Trigger breakdown on BUS-01 (StandardBus with active trip)
        mockMvc.perform(post("/api/simulation/buses/BUS-01/breakdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busId").value("BUS-01"))
                .andExpect(jsonPath("$.currentStatus").value("MAINTENANCE"))
                .andExpect(jsonPath("$.hadActiveTrip").value(true))
                .andExpect(jsonPath("$.strandedPassengers").value(greaterThan(0)))
                .andExpect(jsonPath("$.reliefDemandRequestId").exists());

        // 3. Verify analytics confirms passenger conservation is strictly maintained
        mockMvc.perform(get("/api/simulation/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passengers.conservationValid").value(true))
                .andExpect(jsonPath("$.fleet.maintenanceBuses").value(1));

        // 4. Restore the bus
        mockMvc.perform(post("/api/simulation/buses/BUS-01/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        mockMvc.perform(get("/api/simulation/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fleet.maintenanceBuses").value(0))
                .andExpect(jsonPath("$.fleet.availableBuses").value(1));
    }
}
