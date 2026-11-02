package com.transport.backend.service;

import com.transport.backend.config.DatabaseSeeder;
import com.transport.backend.entity.BusEntity;
import com.transport.backend.entity.DemandRequestEntity;
import com.transport.backend.entity.RoadEntity;
import com.transport.backend.entity.StopEntity;
import com.transport.backend.exception.InconsistentDatasetException;
import com.transport.backend.repository.BusRepository;
import com.transport.backend.repository.DemandRequestRepository;
import com.transport.backend.repository.RoadRepository;
import com.transport.backend.repository.StopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DatabaseSeederTest {

    @Autowired
    private DatabaseSeeder databaseSeeder;

    @Autowired
    private StopRepository stopRepository;

    @Autowired
    private RoadRepository roadRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private DemandRequestRepository demandRequestRepository;

    @BeforeEach
    void cleanRepositories() {
        demandRequestRepository.deleteAll();
        busRepository.deleteAll();
        roadRepository.deleteAll();
        stopRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Empty database triggers complete baseline seeding")
    void testEmptyDatabaseTriggersSeeding() {
        assertEquals(DatabaseSeeder.DatabaseState.EMPTY, databaseSeeder.evaluateDatabaseState());

        databaseSeeder.seedIfEmpty();

        assertEquals(13, stopRepository.count(), "Should seed exactly 13 stops");
        assertEquals(38, roadRepository.count(), "Should seed exactly 38 directed roads");
        assertEquals(4, busRepository.count(), "Should seed exactly 4 buses");
        assertEquals(22, demandRequestRepository.count(), "Should seed exactly 22 demands");

        assertEquals(DatabaseSeeder.DatabaseState.POPULATED, databaseSeeder.evaluateDatabaseState());
    }

    @Test
    @DisplayName("2. Repeated execution is idempotent and does not duplicate records")
    void testRepeatedStartupIsIdempotent() {
        databaseSeeder.seedIfEmpty();
        assertEquals(13, stopRepository.count());

        // Run a second time
        assertDoesNotThrow(() -> databaseSeeder.seedIfEmpty());

        assertEquals(13, stopRepository.count());
        assertEquals(38, roadRepository.count());
        assertEquals(4, busRepository.count());
        assertEquals(22, demandRequestRepository.count());
    }

    @Test
    @DisplayName("3. Partially populated database is detected as inconsistent and throws exception")
    void testInconsistentDatabaseDetected() {
        stopRepository.save(new StopEntity("DEPOT", "Central Depot", true));
        // roads, buses, demands remain empty

        assertEquals(DatabaseSeeder.DatabaseState.INCONSISTENT, databaseSeeder.evaluateDatabaseState());
        assertThrows(InconsistentDatasetException.class, () -> databaseSeeder.seedIfEmpty());
    }

    @Test
    @DisplayName("4. Custom/operational populated dataset is preserved and never overwritten")
    void testModifiedOperationalDataNotOverwritten() {
        StopEntity s1 = stopRepository.save(new StopEntity("DEPOT", "Central Depot", true));
        StopEntity s2 = stopRepository.save(new StopEntity("S1", "Stop 1", false));
        roadRepository.save(new RoadEntity("DEPOT", "S1", 5.0, false));
        busRepository.save(new BusEntity("BUS-CUSTOM", "STANDARD_BUS", 60, false, "DEPOT", "AVAILABLE", 0));
        demandRequestRepository.save(new DemandRequestEntity("DEM-CUSTOM", "DEPOT", "S1", 10, 10, 1L, "DEPOT->S1", "WAITING"));

        assertEquals(DatabaseSeeder.DatabaseState.POPULATED, databaseSeeder.evaluateDatabaseState());

        databaseSeeder.seedIfEmpty();

        // Must still have only 1 custom bus, not overwritten with baseline 4 buses
        assertEquals(1, busRepository.count());
        assertTrue(busRepository.existsById("BUS-CUSTOM"));
    }

    @Test
    @DisplayName("5. BUG-DB-02: Valid infrastructure with zero pending demands evaluates as POPULATED and preserves state")
    void testZeroPendingDemandsEvaluatesAsPopulated() {
        // Stops, roads, and fleet exist, but demand queue is empty (all demands completed or none injected)
        stopRepository.save(new StopEntity("DEPOT", "Central Depot", true));
        stopRepository.save(new StopEntity("S1", "Stop 1", false));
        roadRepository.save(new RoadEntity("DEPOT", "S1", 5.0, false));
        busRepository.save(new BusEntity("BUS-01", "STANDARD_BUS", 40, false, "DEPOT", "AVAILABLE", 0));
        // demandRequestRepository has 0 rows

        assertEquals(0, demandRequestRepository.count());
        assertEquals(DatabaseSeeder.DatabaseState.POPULATED, databaseSeeder.evaluateDatabaseState(),
                "Infrastructure presence with 0 demands must be considered a valid POPULATED database");

        assertDoesNotThrow(() -> databaseSeeder.seedIfEmpty(),
                "Zero demands must not trigger InconsistentDatasetException or wipe the database");
        assertEquals(1, busRepository.count(), "Custom fleet must be preserved");
    }
}
