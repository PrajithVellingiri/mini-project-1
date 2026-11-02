package com.transport.backend.config;

import com.transport.backend.entity.BusEntity;
import com.transport.backend.entity.DemandRequestEntity;
import com.transport.backend.entity.RoadEntity;
import com.transport.backend.entity.StopEntity;
import com.transport.backend.exception.InconsistentDatasetException;
import com.transport.backend.repository.BusRepository;
import com.transport.backend.repository.DemandRequestRepository;
import com.transport.backend.repository.RoadRepository;
import com.transport.backend.repository.StopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Evaluates the persistent state across core reference tables and performs
 * idempotent initial database seeding when the database is completely empty.
 * Never overwrites populated or modified operational data.
 */
@Component
public class DatabaseSeeder {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    public enum DatabaseState {
        EMPTY,
        POPULATED,
        INCONSISTENT
    }

    private final StopRepository stopRepository;
    private final RoadRepository roadRepository;
    private final BusRepository busRepository;
    private final DemandRequestRepository demandRequestRepository;

    public DatabaseSeeder(StopRepository stopRepository,
                          RoadRepository roadRepository,
                          BusRepository busRepository,
                          DemandRequestRepository demandRequestRepository) {
        this.stopRepository = stopRepository;
        this.roadRepository = roadRepository;
        this.busRepository = busRepository;
        this.demandRequestRepository = demandRequestRepository;
    }

    /**
     * Inspects row counts across all four core reference tables to classify the database state.
     */
    public DatabaseState evaluateDatabaseState() {
        long stopCount = stopRepository.count();
        long roadCount = roadRepository.count();
        long busCount = busRepository.count();
        long demandCount = demandRequestRepository.count();

        if (stopCount == 0 && roadCount == 0 && busCount == 0 && demandCount == 0) {
            return DatabaseState.EMPTY;
        }

        if (stopCount > 0 && roadCount > 0 && busCount > 0 && demandCount > 0) {
            return DatabaseState.POPULATED;
        }

        return DatabaseState.INCONSISTENT;
    }

    /**
     * Seeds the baseline dataset if the database is completely empty.
     * Skips execution if already populated, and throws an exception if inconsistent.
     */
    @Transactional
    public void seedIfEmpty() {
        DatabaseState state = evaluateDatabaseState();
        long stopCount = stopRepository.count();
        long roadCount = roadRepository.count();
        long busCount = busRepository.count();
        long demandCount = demandRequestRepository.count();

        switch (state) {
            case EMPTY -> {
                log.info("Database is empty. Executing baseline reference data seeding...");
                seedBaselineDataset();
                log.info("Baseline reference dataset seeded successfully: 13 stops, 38 roads, 4 buses, 22 demands.");
            }
            case POPULATED -> log.info(
                    "Existing database records detected (stops: {}, roads: {}, buses: {}, demands: {}). Preserving operational dataset.",
                    stopCount, roadCount, busCount, demandCount
            );
            case INCONSISTENT -> {
                String diagnostic = String.format(
                        "Inconsistent database state detected: stops=%d, roads=%d, buses=%d, demands=%d. " +
                        "Core reference tables are partially populated. Aborting startup to prevent state corruption.",
                        stopCount, roadCount, busCount, demandCount
                );
                log.error(diagnostic);
                throw new InconsistentDatasetException(diagnostic);
            }
        }
    }

    private void seedBaselineDataset() {
        // 1. Stops (1 Depot + 12 Stops = 13 Stops)
        List<StopEntity> stops = List.of(
                new StopEntity("DEPOT", "Central Transit Depot", true),
                new StopEntity("S1", "Downtown Hub", false),
                new StopEntity("S2", "Central Station", false),
                new StopEntity("S3", "University Campus", false),
                new StopEntity("S4", "Tech Park North", false),
                new StopEntity("S5", "City Hospital", false),
                new StopEntity("S6", "Market Square", false),
                new StopEntity("S7", "Old Town Historic", false),
                new StopEntity("S8", "South Harbor", false),
                new StopEntity("S9", "Airport Terminal", false),
                new StopEntity("S10", "West End Mall", false),
                new StopEntity("S11", "Industrial Zone", false),
                new StopEntity("S12", "Riverside Park", false)
        );
        stopRepository.saveAll(stops);

        // 2. Roads (19 bidirectional corridors = 38 directed roads)
        List<RoadEntity> roads = List.of(
                // Depot links
                new RoadEntity("DEPOT", "S1", 8.0, false),
                new RoadEntity("S1", "DEPOT", 8.0, false),
                new RoadEntity("DEPOT", "S2", 10.0, false),
                new RoadEntity("S2", "DEPOT", 10.0, false),
                new RoadEntity("DEPOT", "S11", 12.0, false),
                new RoadEntity("S11", "DEPOT", 12.0, false),

                // Primary corridors
                new RoadEntity("S1", "S2", 6.0, false),
                new RoadEntity("S2", "S1", 6.0, false),
                new RoadEntity("S1", "S3", 9.0, false),
                new RoadEntity("S3", "S1", 9.0, false),
                new RoadEntity("S2", "S4", 11.0, false),
                new RoadEntity("S4", "S2", 11.0, false),
                new RoadEntity("S3", "S4", 7.0, false),
                new RoadEntity("S4", "S3", 7.0, false),
                new RoadEntity("S3", "S5", 8.0, false),
                new RoadEntity("S5", "S3", 8.0, false),
                new RoadEntity("S4", "S9", 14.0, false),
                new RoadEntity("S9", "S4", 14.0, false),
                new RoadEntity("S5", "S6", 5.0, false),
                new RoadEntity("S6", "S5", 5.0, false),

                // Narrow historic road segments
                new RoadEntity("S6", "S7", 4.0, true),
                new RoadEntity("S7", "S6", 4.0, true),
                new RoadEntity("S7", "S1", 5.0, true),
                new RoadEntity("S1", "S7", 5.0, true),

                // Southern and peripheral links
                new RoadEntity("S6", "S8", 10.0, false),
                new RoadEntity("S8", "S6", 10.0, false),
                new RoadEntity("S8", "S9", 12.0, false),
                new RoadEntity("S9", "S8", 12.0, false),
                new RoadEntity("S2", "S10", 8.0, false),
                new RoadEntity("S10", "S2", 8.0, false),
                new RoadEntity("S10", "S11", 9.0, false),
                new RoadEntity("S11", "S10", 9.0, false),
                new RoadEntity("S11", "S12", 7.0, false),
                new RoadEntity("S12", "S11", 7.0, false),
                new RoadEntity("S12", "S8", 11.0, false),
                new RoadEntity("S8", "S12", 11.0, false),
                new RoadEntity("S10", "S12", 6.0, false),
                new RoadEntity("S12", "S10", 6.0, false)
        );
        roadRepository.saveAll(roads);

        // 3. Buses (2 StandardBus, 2 MiniBus)
        List<BusEntity> buses = List.of(
                new BusEntity("BUS-01", "STANDARD_BUS", 45, false, "DEPOT", "AVAILABLE", 0),
                new BusEntity("BUS-02", "STANDARD_BUS", 40, false, "DEPOT", "AVAILABLE", 0),
                new BusEntity("BUS-03", "MINIBUS", 20, true, "DEPOT", "AVAILABLE", 0),
                new BusEntity("BUS-04", "MINIBUS", 15, true, "S1", "AVAILABLE", 0)
        );
        busRepository.saveAll(buses);

        // 4. Demand Requests (22 batches = 520 passengers total)
        List<DemandRequestEntity> demands = List.of(
                new DemandRequestEntity("DEM-01", "S1", "S2", 25, 25, 1L, "S1->S2", "WAITING"),
                new DemandRequestEntity("DEM-02", "S1", "S2", 40, 40, 2L, "S1->S2", "WAITING"),
                new DemandRequestEntity("DEM-03", "S2", "S4", 30, 30, 3L, "S2->S4", "WAITING"),
                new DemandRequestEntity("DEM-04", "S3", "S9", 35, 35, 4L, "S3->S9", "WAITING"),
                new DemandRequestEntity("DEM-05", "S5", "S7", 14, 14, 5L, "S5->S7", "WAITING"),
                new DemandRequestEntity("DEM-06", "S4", "S2", 20, 20, 6L, "S4->S2", "WAITING"),
                new DemandRequestEntity("DEM-07", "S10", "S1", 28, 28, 7L, "S10->S1", "WAITING"),
                new DemandRequestEntity("DEM-08", "S1", "S3", 15, 15, 8L, "S1->S3", "WAITING"),
                new DemandRequestEntity("DEM-09", "S8", "S9", 22, 22, 9L, "S8->S9", "WAITING"),
                new DemandRequestEntity("DEM-10", "S11", "S2", 32, 32, 10L, "S11->S2", "WAITING"),
                new DemandRequestEntity("DEM-11", "S1", "S2", 18, 18, 11L, "S1->S2", "WAITING"),
                new DemandRequestEntity("DEM-12", "S6", "S7", 10, 10, 12L, "S6->S7", "WAITING"),
                new DemandRequestEntity("DEM-13", "S9", "S1", 45, 45, 13L, "S9->S1", "WAITING"),
                new DemandRequestEntity("DEM-14", "S12", "S10", 16, 16, 14L, "S12->S10", "WAITING"),
                new DemandRequestEntity("DEM-15", "S3", "S5", 24, 24, 15L, "S3->S5", "WAITING"),
                new DemandRequestEntity("DEM-16", "S2", "S11", 20, 20, 16L, "S2->S11", "WAITING"),
                new DemandRequestEntity("DEM-17", "S10", "S8", 18, 18, 17L, "S10->S8", "WAITING"),
                new DemandRequestEntity("DEM-18", "S4", "S1", 26, 26, 18L, "S4->S1", "WAITING"),
                new DemandRequestEntity("DEM-19", "S7", "S3", 12, 12, 19L, "S7->S3", "WAITING"),
                new DemandRequestEntity("DEM-20", "S5", "S9", 30, 30, 20L, "S5->S9", "WAITING"),
                new DemandRequestEntity("DEM-21", "S8", "S1", 25, 25, 21L, "S8->S1", "WAITING"),
                new DemandRequestEntity("DEM-22", "S11", "S12", 15, 15, 22L, "S11->S12", "WAITING")
        );
        demandRequestRepository.saveAll(demands);
    }
}
