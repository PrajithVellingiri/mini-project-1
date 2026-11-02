package com.transport.backend.service;

import com.transport.backend.config.DatabaseSeeder;
import com.transport.backend.entity.*;
import com.transport.backend.exception.InconsistentDatasetException;
import com.transport.backend.model.*;
import com.transport.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SimulationPersistenceHydrationTest {

    @Autowired
    private DatabaseSeeder databaseSeeder;

    @Autowired
    private SimulationPersistenceService persistenceService;

    @Autowired
    private StopRepository stopRepository;

    @Autowired
    private RoadRepository roadRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private DemandRequestRepository demandRequestRepository;

    @Autowired
    private ReliefTripRepository reliefTripRepository;

    @Autowired
    private TripRouteStopRepository tripRouteStopRepository;

    @BeforeEach
    void resetState() {
        tripRouteStopRepository.deleteAll();
        reliefTripRepository.deleteAll();
        demandRequestRepository.deleteAll();
        busRepository.deleteAll();
        roadRepository.deleteAll();
        stopRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Complete baseline dataset is faithfully hydrated into domain models")
    void testSuccessfulDatasetHydration() {
        databaseSeeder.seedIfEmpty();

        SimulationPersistenceService.LoadedDataset dataset = persistenceService.loadDataset();

        assertNotNull(dataset);
        assertNotNull(dataset.roadNetwork());
        assertEquals(13, dataset.roadNetwork().getStopCount());
        assertEquals(38, dataset.roadNetwork().getAllRoads().size());

        // Verify narrow road flags
        Road narrowRoad1 = dataset.roadNetwork().getRoad("S6", "S7");
        assertNotNull(narrowRoad1);
        assertTrue(narrowRoad1.isNarrowRoad());

        Road normalRoad = dataset.roadNetwork().getRoad("S1", "S2");
        assertNotNull(normalRoad);
        assertFalse(normalRoad.isNarrowRoad());

        // Verify fleet composition
        assertEquals(4, dataset.buses().size());
        Bus bus1 = dataset.buses().stream().filter(b -> b.getId().equals("BUS-01")).findFirst().orElseThrow();
        assertInstanceOf(StandardBus.class, bus1);
        assertEquals(45, bus1.getCapacity());
        assertEquals(BusStatus.AVAILABLE, bus1.getStatus());
        assertEquals("DEPOT", bus1.getCurrentStop().getId());

        Bus bus4 = dataset.buses().stream().filter(b -> b.getId().equals("BUS-04")).findFirst().orElseThrow();
        assertInstanceOf(MiniBus.class, bus4);
        assertEquals(15, bus4.getCapacity());
        assertEquals("S1", bus4.getCurrentStop().getId());

        // Verify passenger accounting
        assertEquals(520, dataset.totalInitialPassengers());
        assertEquals(0, dataset.totalAllocatedPassengers());
        assertEquals(520, dataset.demandQueueService().getTotalWaitingPassengers());
        assertEquals(0, dataset.activeTrips().size());
    }

    @Test
    @DisplayName("2. Demand queue preserves strict corridor FIFO and global priority on hydration")
    void testCorridorFifoAndPriorityPreservedOnHydration() {
        databaseSeeder.seedIfEmpty();

        SimulationPersistenceService.LoadedDataset dataset = persistenceService.loadDataset();
        DemandQueueService queue = dataset.demandQueueService();

        // On corridor S1->S2, we seeded:
        // DEM-01 (order 1, 25 pax), DEM-02 (order 2, 40 pax), DEM-11 (order 11, 18 pax)
        // Strict corridor FIFO dictates that DEM-01 must be the candidate head for S1->S2,
        // even though DEM-02 has 40 passengers.
        List<DemandRequest> eligible = queue.getEligibleRequestsInPriorityOrder();
        boolean foundDem01 = eligible.stream().anyMatch(d -> d.getRequestId().equals("DEM-01"));
        boolean foundDem02 = eligible.stream().anyMatch(d -> d.getRequestId().equals("DEM-02"));

        assertTrue(foundDem01, "DEM-01 (earlier arrival) must be eligible at the head of corridor S1->S2");
        assertFalse(foundDem02, "DEM-02 must be queued behind DEM-01 in the corridor queue");
    }

    @Test
    @DisplayName("3. Active IN_PROGRESS trip is faithfully reconstructed with route and passenger accounting")
    void testActiveTripReconstruction() {
        databaseSeeder.seedIfEmpty();

        // Simulate an in-progress trip on BUS-01 boarding DEM-01 (25 passengers)
        BusEntity bus1 = busRepository.findById("BUS-01").orElseThrow();
        bus1.setStatus("IN_TRANSIT");
        bus1.setPassengerOccupancy(25);
        busRepository.save(bus1);

        DemandRequestEntity dem01 = demandRequestRepository.findById("DEM-01").orElseThrow();
        dem01.setRemainingPassengerCount(0);
        dem01.setServiceStatus("FULLY_SERVED");
        demandRequestRepository.save(dem01);

        ReliefTripEntity tripEntity = new ReliefTripEntity(
                "TRIP-0001", "BUS-01", "DEM-01", null, "DEPOT", "S2", 25, 16.0, "IN_PROGRESS", LocalDateTime.now(), null
        );
        reliefTripRepository.save(tripEntity);

        tripRouteStopRepository.save(new TripRouteStopEntity("TRIP-0001", "DEPOT", 1));
        tripRouteStopRepository.save(new TripRouteStopEntity("TRIP-0001", "S1", 2));
        tripRouteStopRepository.save(new TripRouteStopEntity("TRIP-0001", "S2", 3));

        SimulationPersistenceService.LoadedDataset dataset = persistenceService.loadDataset();

        assertEquals(1, dataset.activeTrips().size());
        ReliefTrip restoredTrip = dataset.activeTrips().get(0);
        assertEquals("TRIP-0001", restoredTrip.getTripId());
        assertEquals("BUS-01", restoredTrip.getAssignedBus().getId());
        assertEquals(TripStatus.IN_PROGRESS, restoredTrip.getStatus());
        assertEquals(3, restoredTrip.getRoute().size());
        assertEquals("DEPOT", restoredTrip.getRoute().get(0).getId());
        assertEquals("S2", restoredTrip.getRoute().get(2).getId());

        // Verify passenger conservation: Total(520) = Waiting(495) + In-Transit(25) + Completed(0)
        assertEquals(520, dataset.totalInitialPassengers());
        assertEquals(25, dataset.totalAllocatedPassengers());
        assertEquals(495, dataset.demandQueueService().getTotalWaitingPassengers());
    }

    @Test
    @DisplayName("4. SCHEDULED trip semantics: bus is AVAILABLE and passengers are reserved")
    void testScheduledTripSemantics() {
        databaseSeeder.seedIfEmpty();

        // Create a SCHEDULED trip: bus is still AVAILABLE at DEPOT with 0 occupancy
        DemandRequestEntity dem01 = demandRequestRepository.findById("DEM-01").orElseThrow();
        dem01.setRemainingPassengerCount(0);
        dem01.setServiceStatus("FULLY_SERVED");
        demandRequestRepository.save(dem01);

        ReliefTripEntity scheduledTrip = new ReliefTripEntity(
                "TRIP-SCHED", "BUS-01", "DEM-01", null, "DEPOT", "S2", 25, 16.0, "SCHEDULED", null, null
        );
        reliefTripRepository.save(scheduledTrip);

        tripRouteStopRepository.save(new TripRouteStopEntity("TRIP-SCHED", "DEPOT", 1));
        tripRouteStopRepository.save(new TripRouteStopEntity("TRIP-SCHED", "S2", 2));

        SimulationPersistenceService.LoadedDataset dataset = persistenceService.loadDataset();

        assertEquals(1, dataset.activeTrips().size());
        assertEquals(TripStatus.SCHEDULED, dataset.activeTrips().get(0).getStatus());
        Bus assignedBus = dataset.activeTrips().get(0).getAssignedBus();
        assertEquals(BusStatus.AVAILABLE, assignedBus.getStatus());
    }

    @Test
    @DisplayName("5. Integrity validation: Bus occupancy exceeding capacity throws InconsistentDatasetException")
    void testIntegrityValidation_BusOccupancyExceedsCapacity() {
        databaseSeeder.seedIfEmpty();

        BusEntity bus = busRepository.findById("BUS-03").orElseThrow(); // Capacity 20
        bus.setPassengerOccupancy(30);
        busRepository.save(bus);

        InconsistentDatasetException ex = assertThrows(InconsistentDatasetException.class, () -> persistenceService.loadDataset());
        assertTrue(ex.getMessage().contains("violates capacity bounds"));
    }

    @Test
    @DisplayName("6. Integrity validation: IN_TRANSIT bus without active trip throws InconsistentDatasetException")
    void testIntegrityValidation_InTransitBusWithoutActiveTrip() {
        databaseSeeder.seedIfEmpty();

        BusEntity bus = busRepository.findById("BUS-01").orElseThrow();
        bus.setStatus("IN_TRANSIT");
        bus.setPassengerOccupancy(25);
        busRepository.save(bus);

        InconsistentDatasetException ex = assertThrows(InconsistentDatasetException.class, () -> persistenceService.loadDataset());
        assertTrue(ex.getMessage().contains("marked IN_TRANSIT but has no corresponding active trip"));
    }

    @Test
    @DisplayName("7. Integrity validation: AVAILABLE bus assigned to IN_PROGRESS trip throws InconsistentDatasetException")
    void testIntegrityValidation_AvailableBusWithActiveTrip() {
        databaseSeeder.seedIfEmpty();

        DemandRequestEntity dem01 = demandRequestRepository.findById("DEM-01").orElseThrow();
        dem01.setRemainingPassengerCount(0);
        demandRequestRepository.save(dem01);

        // Bus-01 left as AVAILABLE, but trip is IN_PROGRESS
        ReliefTripEntity trip = new ReliefTripEntity(
                "TRIP-BAD", "BUS-01", "DEM-01", null, "DEPOT", "S2", 25, 16.0, "IN_PROGRESS", LocalDateTime.now(), null
        );
        reliefTripRepository.save(trip);
        tripRouteStopRepository.save(new TripRouteStopEntity("TRIP-BAD", "DEPOT", 1));
        tripRouteStopRepository.save(new TripRouteStopEntity("TRIP-BAD", "S2", 2));

        InconsistentDatasetException ex = assertThrows(InconsistentDatasetException.class, () -> persistenceService.loadDataset());
        assertTrue(ex.getMessage().contains("has an active IN_PROGRESS trip"));
    }

    @Test
    @DisplayName("8. Integrity validation: Passenger conservation violation throws InconsistentDatasetException")
    void testIntegrityValidation_PassengerConservationViolated() {
        databaseSeeder.seedIfEmpty();

        // Corrupt DEM-01: original 25, remaining 10, but 0 trips exist (15 passengers disappeared)
        DemandRequestEntity dem01 = demandRequestRepository.findById("DEM-01").orElseThrow();
        dem01.setRemainingPassengerCount(10);
        demandRequestRepository.save(dem01);

        InconsistentDatasetException ex = assertThrows(InconsistentDatasetException.class, () -> persistenceService.loadDataset());
        assertTrue(ex.getMessage().contains("Passenger conservation violated"));
    }

    @Test
    @DisplayName("9. Dynamically persisted demand request survives reload and preserves passenger conservation")
    void testDynamicDemandPersistenceAndConservationOnReload() {
        databaseSeeder.seedIfEmpty();

        Stop s1 = new Stop("S1", "Main Terminal");
        Stop s4 = new Stop("S4", "North District");
        DemandRequest dynamicDemand = new DemandRequest("DEM-DYN-01", s1, s4, 35, System.currentTimeMillis());

        // Persist dynamic demand
        persistenceService.persistDemandRequest(dynamicDemand);

        // Reload dataset from persistence (simulating backend reboot)
        SimulationPersistenceService.LoadedDataset reloaded = persistenceService.loadDataset();

        assertNotNull(reloaded);
        assertEquals(520 + 35, reloaded.totalInitialPassengers(), "Total passengers must include dynamically added demand");
        assertEquals(520 + 35, reloaded.demandQueueService().getTotalWaitingPassengers());
        assertEquals(0, reloaded.totalAllocatedPassengers());
        assertEquals(23, reloaded.demandQueueService().size());
    }
}
