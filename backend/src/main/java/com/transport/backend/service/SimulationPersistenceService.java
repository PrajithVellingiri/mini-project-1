package com.transport.backend.service;

import com.transport.backend.entity.*;
import com.transport.backend.exception.InconsistentDatasetException;
import com.transport.backend.model.*;
import com.transport.backend.network.RoadNetwork;
import com.transport.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service orchestrating database persistence for the simulation.
 * Manages loading reference data from MySQL and persisting simulation runs,
 * trips, routes, demand status, bus state, and road updates.
 */
@Service
public class SimulationPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(SimulationPersistenceService.class);

    private final StopRepository stopRepository;
    private final RoadRepository roadRepository;
    private final BusRepository busRepository;
    private final DemandRequestRepository demandRequestRepository;
    private final SimulationRunRepository simulationRunRepository;
    private final ReliefTripRepository reliefTripRepository;
    private final TripRouteStopRepository tripRouteStopRepository;
    private final RoadTravelTimeHistoryRepository roadTravelTimeHistoryRepository;

    public SimulationPersistenceService(StopRepository stopRepository,
                                        RoadRepository roadRepository,
                                        BusRepository busRepository,
                                        DemandRequestRepository demandRequestRepository,
                                        SimulationRunRepository simulationRunRepository,
                                        ReliefTripRepository reliefTripRepository,
                                        TripRouteStopRepository tripRouteStopRepository,
                                        RoadTravelTimeHistoryRepository roadTravelTimeHistoryRepository) {
        this.stopRepository = stopRepository;
        this.roadRepository = roadRepository;
        this.busRepository = busRepository;
        this.demandRequestRepository = demandRequestRepository;
        this.simulationRunRepository = simulationRunRepository;
        this.reliefTripRepository = reliefTripRepository;
        this.tripRouteStopRepository = tripRouteStopRepository;
        this.roadTravelTimeHistoryRepository = roadTravelTimeHistoryRepository;
    }

    /**
     * Checks if MySQL database already has the seeded reference data across all four reference tables.
     */
    public boolean hasPersistedData() {
        try {
            return stopRepository.count() > 0
                    && roadRepository.count() > 0
                    && busRepository.count() > 0;
        } catch (Exception e) {
            log.warn("Could not check persisted data: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Loads stops, roads, buses, demand, and active trips from the database with deep integrity validation.
     */
    @Transactional(readOnly = true)
    public LoadedDataset loadDataset() {
        long stopCount = stopRepository.count();
        long roadCount = roadRepository.count();
        long busCount = busRepository.count();
        long demandCount = demandRequestRepository.count();

        if (stopCount == 0 || roadCount == 0 || busCount == 0) {
            throw new InconsistentDatasetException(String.format(
                    "Incomplete dataset in database: stops=%d, roads=%d, buses=%d, demands=%d. " +
                    "Core infrastructure reference tables (stops, roads, buses) must contain data for simulation hydration.",
                    stopCount, roadCount, busCount, demandCount
            ));
        }

        // 1. Reconstruct Stops and RoadNetwork
        Map<String, Stop> stopsMap = new LinkedHashMap<>();
        boolean hasDepot = false;
        for (StopEntity se : stopRepository.findAll()) {
            if (se.isDepot() || "DEPOT".equalsIgnoreCase(se.getStopId())) {
                hasDepot = true;
            }
            stopsMap.put(se.getStopId(), new Stop(se.getStopId(), se.getStopName()));
        }
        if (!hasDepot) {
            throw new InconsistentDatasetException("Database does not contain any transit depot stop (is_depot=true).");
        }

        RoadNetwork roadNetwork = new RoadNetwork();
        stopsMap.values().forEach(roadNetwork::addStop);

        List<Road> roadsList = new ArrayList<>();
        for (RoadEntity re : roadRepository.findAll()) {
            if (!stopsMap.containsKey(re.getFromStopId())) {
                throw new InconsistentDatasetException(String.format(
                        "Road ID %d references non-existent origin stop: %s", re.getRoadId(), re.getFromStopId()
                ));
            }
            if (!stopsMap.containsKey(re.getToStopId())) {
                throw new InconsistentDatasetException(String.format(
                        "Road ID %d references non-existent destination stop: %s", re.getRoadId(), re.getToStopId()
                ));
            }
            if (re.getTravelTimeMinutes() <= 0.0) {
                throw new InconsistentDatasetException(String.format(
                        "Road from %s to %s has non-positive travel time: %.2f",
                        re.getFromStopId(), re.getToStopId(), re.getTravelTimeMinutes()
                ));
            }
            Road road = roadNetwork.addRoad(re.getFromStopId(), re.getToStopId(), re.getTravelTimeMinutes(), re.isNarrowRoad());
            roadsList.add(road);
        }

        // 2. Reconstruct Fleet
        Map<String, Bus> busMap = new LinkedHashMap<>();
        List<Bus> busList = new ArrayList<>();
        for (BusEntity be : busRepository.findAll()) {
            if (!stopsMap.containsKey(be.getCurrentStopId())) {
                throw new InconsistentDatasetException(String.format(
                        "Bus %s references non-existent current stop: %s", be.getBusId(), be.getCurrentStopId()
                ));
            }
            if (be.getCapacity() <= 0) {
                throw new InconsistentDatasetException(String.format(
                        "Bus %s has non-positive capacity: %d", be.getBusId(), be.getCapacity()
                ));
            }
            if (be.getPassengerOccupancy() < 0 || be.getPassengerOccupancy() > be.getCapacity()) {
                throw new InconsistentDatasetException(String.format(
                        "Bus %s passenger occupancy (%d) violates capacity bounds [0, %d]",
                        be.getBusId(), be.getPassengerOccupancy(), be.getCapacity()
                ));
            }
            Stop curStop = stopsMap.get(be.getCurrentStopId());
            Bus bus;
            if ("MINIBUS".equalsIgnoreCase(be.getBusType())) {
                bus = new MiniBus(be.getBusId(), be.getCapacity(), curStop);
            } else {
                bus = new StandardBus(be.getBusId(), be.getCapacity(), curStop);
            }
            if (be.getStatus() != null) {
                try {
                    bus.setStatus(BusStatus.valueOf(be.getStatus().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new InconsistentDatasetException(String.format(
                            "Bus %s has invalid status: %s", be.getBusId(), be.getStatus()
                    ));
                }
            }
            if (be.getPassengerOccupancy() > 0) {
                bus.boardPassengers(be.getPassengerOccupancy());
            }
            busMap.put(bus.getId(), bus);
            busList.add(bus);
        }

        // 3. Reconstruct Demand Queues & Demands
        DemandQueueService demandQueueService = new DemandQueueService();
        Map<String, DemandRequestEntity> demandEntityMap = new HashMap<>();
        int totalDemandPax = 0;
        int waitingPassengers = 0;

        List<DemandRequestEntity> allDemandEntities = demandRequestRepository.findAllByOrderByOriginalArrivalOrderAsc();
        for (DemandRequestEntity de : allDemandEntities) {
            demandEntityMap.put(de.getRequestId(), de);
            if (!stopsMap.containsKey(de.getPickupStopId())) {
                throw new InconsistentDatasetException(String.format(
                        "Demand %s references non-existent pickup stop: %s", de.getRequestId(), de.getPickupStopId()
                ));
            }
            if (!stopsMap.containsKey(de.getDestinationStopId())) {
                throw new InconsistentDatasetException(String.format(
                        "Demand %s references non-existent destination stop: %s", de.getRequestId(), de.getDestinationStopId()
                ));
            }
            if (de.getPickupStopId().equalsIgnoreCase(de.getDestinationStopId())) {
                throw new InconsistentDatasetException(String.format(
                        "Demand %s has identical pickup and destination stop: %s", de.getRequestId(), de.getPickupStopId()
                ));
            }
            if (de.getPassengerCount() <= 0) {
                throw new InconsistentDatasetException(String.format(
                        "Demand %s has non-positive passenger count: %d", de.getRequestId(), de.getPassengerCount()
                ));
            }
            if (de.getRemainingPassengerCount() < 0 || de.getRemainingPassengerCount() > de.getPassengerCount()) {
                throw new InconsistentDatasetException(String.format(
                        "Demand %s remaining passenger count (%d) exceeds original count (%d) or is negative",
                        de.getRequestId(), de.getRemainingPassengerCount(), de.getPassengerCount()
                ));
            }

            totalDemandPax += de.getPassengerCount();

            if (!"FULLY_SERVED".equalsIgnoreCase(de.getServiceStatus()) && de.getRemainingPassengerCount() > 0) {
                waitingPassengers += de.getRemainingPassengerCount();
                Stop pickup = stopsMap.get(de.getPickupStopId());
                Stop dest = stopsMap.get(de.getDestinationStopId());
                DemandRequest req = new DemandRequest(
                        de.getRequestId(),
                        pickup,
                        dest,
                        de.getRemainingPassengerCount(),
                        de.getOriginalArrivalOrder(),
                        de.getCreatedAt()
                );
                demandQueueService.enqueue(req);
            }
        }

        // 4. Reconstruct Active Trips & Accounting
        List<ReliefTrip> activeTrips = new ArrayList<>();
        Map<String, List<ReliefTrip>> busToActiveTrips = new HashMap<>();
        Map<String, Integer> demandAllocatedPax = new HashMap<>();
        int inTransitPassengers = 0;
        int completedPassengers = 0;
        int cancelledPassengers = 0;
        long maxTripSeq = 0;

        List<ReliefTripEntity> allTrips = reliefTripRepository.findAll();
        for (ReliefTripEntity te : allTrips) {
            if (te.getTripId() != null && te.getTripId().startsWith("TRIP-")) {
                try {
                    long seq = Long.parseLong(te.getTripId().substring(5));
                    if (seq > maxTripSeq) {
                        maxTripSeq = seq;
                    }
                } catch (NumberFormatException ignored) {}
            }
            TripStatus tripStatus;
            try {
                tripStatus = TripStatus.valueOf(te.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InconsistentDatasetException(String.format(
                        "Trip %s has invalid status: %s", te.getTripId(), te.getStatus()
                ));
            }

            if (tripStatus == TripStatus.COMPLETED) {
                completedPassengers += te.getBoardedPassengerCount();
                demandAllocatedPax.merge(te.getDemandRequestId(), te.getBoardedPassengerCount(), Integer::sum);
                continue;
            }

            if (tripStatus == TripStatus.CANCELLED) {
                cancelledPassengers += te.getBoardedPassengerCount();
                demandAllocatedPax.merge(te.getDemandRequestId(), te.getBoardedPassengerCount(), Integer::sum);
                continue; // Cancelled trips do not count as in-transit or completed
            }

            // SCHEDULED or IN_PROGRESS trip:
            inTransitPassengers += te.getBoardedPassengerCount();
            demandAllocatedPax.merge(te.getDemandRequestId(), te.getBoardedPassengerCount(), Integer::sum);

            Bus assignedBus = busMap.get(te.getBusId());
            if (assignedBus == null) {
                throw new InconsistentDatasetException(String.format(
                        "Active trip %s references non-existent bus: %s", te.getTripId(), te.getBusId()
                ));
            }

            DemandRequestEntity sourceDemandEntity = demandEntityMap.get(te.getDemandRequestId());
            if (sourceDemandEntity == null) {
                throw new InconsistentDatasetException(String.format(
                        "Active trip %s references non-existent demand request: %s", te.getTripId(), te.getDemandRequestId()
                ));
            }

            List<TripRouteStopEntity> routeStopEntities = tripRouteStopRepository.findByTripIdOrderByStopSequenceOrderAsc(te.getTripId());
            if (routeStopEntities.isEmpty()) {
                throw new InconsistentDatasetException(String.format(
                        "Active trip %s has no route stops recorded in trip_route_stops", te.getTripId()
                ));
            }

            List<Stop> route = new ArrayList<>();
            int expectedSequence = 1;
            for (TripRouteStopEntity trse : routeStopEntities) {
                if (trse.getStopSequenceOrder() != expectedSequence) {
                    throw new InconsistentDatasetException(String.format(
                            "Trip %s route stops sequence order broken: expected %d, got %d",
                            te.getTripId(), expectedSequence, trse.getStopSequenceOrder()
                    ));
                }
                Stop stop = stopsMap.get(trse.getStopId());
                if (stop == null) {
                    throw new InconsistentDatasetException(String.format(
                            "Trip %s route references non-existent stop: %s", te.getTripId(), trse.getStopId()
                    ));
                }
                route.add(stop);
                expectedSequence++;
            }

            DemandRequest tripDemand = new DemandRequest(
                    te.getDemandRequestId() + "-active",
                    stopsMap.get(te.getOriginStopId()),
                    stopsMap.get(te.getDestinationStopId()),
                    te.getBoardedPassengerCount(),
                    sourceDemandEntity.getOriginalArrivalOrder(),
                    sourceDemandEntity.getCreatedAt()
            );

            ReliefTrip trip = new ReliefTrip(
                    te.getTripId(),
                    assignedBus,
                    tripDemand,
                    route,
                    te.getEstimatedTravelTimeMinutes(),
                    tripStatus
            );

            activeTrips.add(trip);
            busToActiveTrips.computeIfAbsent(assignedBus.getId(), k -> new ArrayList<>()).add(trip);
        }

        // 5. Cross-Entity Validation: Bus vs Active Trips
        for (Bus bus : busList) {
            List<ReliefTrip> tripsForBus = busToActiveTrips.getOrDefault(bus.getId(), Collections.emptyList());
            if (tripsForBus.size() > 1) {
                throw new InconsistentDatasetException(String.format(
                        "Bus %s is assigned to multiple active trips: %s",
                        bus.getId(), tripsForBus.stream().map(ReliefTrip::getTripId).toList()
                ));
            }

            if (bus.getStatus() == BusStatus.IN_TRANSIT) {
                if (tripsForBus.isEmpty()) {
                    throw new InconsistentDatasetException(String.format(
                            "Bus %s is marked IN_TRANSIT but has no corresponding active trip in relief_trips", bus.getId()
                    ));
                }
                ReliefTrip activeTrip = tripsForBus.get(0);
                if (activeTrip.getStatus() != TripStatus.IN_PROGRESS) {
                    throw new InconsistentDatasetException(String.format(
                            "Bus %s is IN_TRANSIT but its active trip %s has status %s (expected IN_PROGRESS)",
                            bus.getId(), activeTrip.getTripId(), activeTrip.getStatus()
                    ));
                }
                if (bus.getPassengerOccupancy() != activeTrip.getDemandRequest().getPassengerCount()) {
                    throw new InconsistentDatasetException(String.format(
                            "Bus %s occupancy (%d) does not match boarded passenger count (%d) of active trip %s",
                            bus.getId(), bus.getPassengerOccupancy(), activeTrip.getDemandRequest().getPassengerCount(), activeTrip.getTripId()
                    ));
                }
            } else {
                for (ReliefTrip t : tripsForBus) {
                    if (t.getStatus() == TripStatus.IN_PROGRESS) {
                        throw new InconsistentDatasetException(String.format(
                                "Bus %s is in status %s but has an active IN_PROGRESS trip %s",
                                bus.getId(), bus.getStatus(), t.getTripId()
                        ));
                    }
                }
            }
        }

        // 6. Passenger Conservation Validation Across All Demands
        for (DemandRequestEntity de : allDemandEntities) {
            int allocatedPax = demandAllocatedPax.getOrDefault(de.getRequestId(), 0);
            int accountedPax = de.getRemainingPassengerCount() + allocatedPax;
            if (accountedPax != de.getPassengerCount()) {
                throw new InconsistentDatasetException(String.format(
                        "Passenger conservation violated for demand %s: original=%d, remaining=%d, boarded=%d (sum=%d)",
                        de.getRequestId(), de.getPassengerCount(), de.getRemainingPassengerCount(), allocatedPax, accountedPax
                ));
            }
        }

        // Net unique passenger intake: total demands minus recycled stranded passengers from cancelled trips
        int totalInitialPassengers = totalDemandPax - cancelledPassengers;

        int systemAccounted = waitingPassengers + inTransitPassengers + completedPassengers;
        if (systemAccounted != totalInitialPassengers) {
            throw new InconsistentDatasetException(String.format(
                    "System-wide passenger conservation violated: totalInitial=%d, waiting=%d, inTransit=%d, completed=%d, cancelled=%d (sum=%d)",
                    totalInitialPassengers, waitingPassengers, inTransitPassengers, completedPassengers, cancelledPassengers, systemAccounted
            ));
        }

        return new LoadedDataset(
                roadNetwork,
                new ArrayList<>(stopsMap.values()),
                roadsList,
                busList,
                demandQueueService,
                activeTrips,
                totalInitialPassengers,
                (inTransitPassengers + completedPassengers),
                maxTripSeq + 1
        );
    }

    /**
     * Persists an initial dataset to MySQL if database is empty.
     */
    @Transactional
    public void seedInitialData(List<Stop> stops, List<Road> roads, List<Bus> buses, List<DemandRequest> demands) {
        if (stopRepository.count() == 0) {
            for (Stop s : stops) {
                stopRepository.save(new StopEntity(s.getId(), s.getName(), "DEPOT".equalsIgnoreCase(s.getId())));
            }
        }
        if (roadRepository.count() == 0) {
            for (Road r : roads) {
                roadRepository.save(new RoadEntity(r.getFromStopId(), r.getToStopId(), r.getTravelTimeMinutes(), r.isNarrowRoad()));
            }
        }
        if (busRepository.count() == 0) {
            for (Bus b : buses) {
                String type = (b instanceof MiniBus) ? "MINIBUS" : "STANDARD_BUS";
                boolean canNarrow = (b instanceof MiniBus);
                busRepository.save(new BusEntity(b.getId(), type, b.getCapacity(), canNarrow,
                        b.getCurrentStop().getId(), b.getStatus().name(), b.getPassengerOccupancy()));
            }
        }
        if (demandRequestRepository.count() == 0) {
            for (DemandRequest d : demands) {
                String corridorKey = d.getPickupStop().getId() + "->" + d.getDestinationStop().getId();
                demandRequestRepository.save(new DemandRequestEntity(d.getRequestId(),
                        d.getPickupStop().getId(), d.getDestinationStop().getId(),
                        d.getPassengerCount(), d.getPassengerCount(), d.getOriginalArrivalOrder(),
                        corridorKey, "WAITING"));
            }
        }
    }

    /**
     * Persists a simulation run, its assigned trips, ordered route stops, and updates bus/demand state.
     */
    @Transactional
    public void persistAllocationRun(AllocationResult result, List<Bus> allBuses, int pendingPax) {
        SimulationRunEntity runEntity = new SimulationRunEntity(
                LocalDateTime.now(),
                result.getAllocatedPassengerCount(),
                pendingPax,
                result.getAssignedTrips().size()
        );
        runEntity = simulationRunRepository.save(runEntity);

        for (ReliefTrip trip : result.getAssignedTrips()) {
            Stop originStop = trip.getRoute().get(0);
            Stop destStop = trip.getRoute().get(trip.getRoute().size() - 1);
            String baseReqId = trip.getDemandRequest().getRequestId().replace("-boarded", "");

            ReliefTripEntity tripEntity = new ReliefTripEntity(
                    trip.getTripId(),
                    trip.getAssignedBus().getId(),
                    baseReqId,
                    runEntity.getRunId(),
                    originStop.getId(),
                    destStop.getId(),
                    trip.getDemandRequest().getPassengerCount(),
                    trip.getEstimatedTravelTimeMinutes(),
                    trip.getStatus().name(),
                    LocalDateTime.now(),
                    null
            );
            reliefTripRepository.save(tripEntity);

            List<Stop> routeStops = trip.getRoute();
            for (int i = 0; i < routeStops.size(); i++) {
                tripRouteStopRepository.save(new TripRouteStopEntity(trip.getTripId(), routeStops.get(i).getId(), i + 1));
            }

            // Update source demand request status in DB
            demandRequestRepository.findById(baseReqId).ifPresent(de -> {
                int remaining = Math.max(0, de.getRemainingPassengerCount() - trip.getDemandRequest().getPassengerCount());
                de.setRemainingPassengerCount(remaining);
                de.setServiceStatus(remaining == 0 ? "FULLY_SERVED" : "PARTIALLY_SERVED");
                demandRequestRepository.save(de);
            });
        }

        // Persist bus statuses
        for (Bus b : allBuses) {
            busRepository.findById(b.getId()).ifPresent(be -> {
                be.setStatus(b.getStatus().name());
                be.setCurrentStopId(b.getCurrentStop().getId());
                be.setPassengerOccupancy(b.getPassengerOccupancy());
                busRepository.save(be);
            });
        }
    }

    /**
     * Persists trip completion (moving bus to terminal stop and freeing it).
     */
    @Transactional
    public void persistTripCompletion(ReliefTrip trip) {
        reliefTripRepository.findById(trip.getTripId()).ifPresent(te -> {
            te.setStatus(TripStatus.COMPLETED.name());
            te.setCompletedAt(LocalDateTime.now());
            reliefTripRepository.save(te);
        });

        Bus bus = trip.getAssignedBus();
        busRepository.findById(bus.getId()).ifPresent(be -> {
            be.setStatus(bus.getStatus().name());
            be.setCurrentStopId(bus.getCurrentStop().getId());
            be.setPassengerOccupancy(bus.getPassengerOccupancy());
            busRepository.save(be);
        });
    }

    /**
     * Persists road travel time change and writes an audit log to road_travel_time_history.
     */
    @Transactional
    public void persistRoadTravelTimeUpdate(String fromId, String toId, double oldTime, double newTime) {
        roadRepository.findByFromStopIdAndToStopId(fromId, toId).ifPresent(re -> {
            re.setTravelTimeMinutes(newTime);
            roadRepository.save(re);
        });

        roadTravelTimeHistoryRepository.save(new RoadTravelTimeHistoryEntity(
                fromId, toId, oldTime, newTime, LocalDateTime.now()
        ));
    }

    /**
     * Persists bus status update.
     */
    @Transactional
    public void persistBusStatus(String busId, String status) {
        busRepository.findById(busId).ifPresent(be -> {
            be.setStatus(status.toUpperCase());
            busRepository.save(be);
        });
    }

    /**
     * Persists trip cancellation and updates bus state during breakdown.
     */
    @Transactional
    public void persistTripCancellation(ReliefTrip trip) {
        if (trip == null) return;
        reliefTripRepository.findById(trip.getTripId()).ifPresent(te -> {
            te.setStatus(TripStatus.CANCELLED.name());
            reliefTripRepository.save(te);
        });

        Bus bus = trip.getAssignedBus();
        if (bus != null) {
            busRepository.findById(bus.getId()).ifPresent(be -> {
                be.setStatus(bus.getStatus().name());
                be.setCurrentStopId(bus.getCurrentStop().getId());
                be.setPassengerOccupancy(bus.getPassengerOccupancy());
                busRepository.save(be);
            });
        }
    }

    /**
     * Persists a dynamically generated passenger demand batch.
     */
    @Transactional
    public void persistDemandRequest(DemandRequest demand) {
        if (demand == null) return;
        String corridorKey = demand.getPickupStop().getId() + "->" + demand.getDestinationStop().getId();
        DemandRequestEntity entity = new DemandRequestEntity(
                demand.getRequestId(),
                demand.getPickupStop().getId(),
                demand.getDestinationStop().getId(),
                demand.getPassengerCount(),
                demand.getPassengerCount(),
                demand.getOriginalArrivalOrder(),
                corridorKey,
                "WAITING"
        );
        demandRequestRepository.save(entity);
    }

    public record LoadedDataset(
            RoadNetwork roadNetwork,
            List<Stop> stops,
            List<Road> roads,
            List<Bus> buses,
            DemandQueueService demandQueueService,
            List<ReliefTrip> activeTrips,
            int totalInitialPassengers,
            int totalAllocatedPassengers,
            long nextTripSequence
    ) {
        public LoadedDataset(
                RoadNetwork roadNetwork,
                List<Stop> stops,
                List<Road> roads,
                List<Bus> buses,
                DemandQueueService demandQueueService,
                List<ReliefTrip> activeTrips,
                int totalInitialPassengers,
                int totalAllocatedPassengers
        ) {
            this(roadNetwork, stops, roads, buses, demandQueueService, activeTrips, totalInitialPassengers, totalAllocatedPassengers, 1L);
        }
    }
}
