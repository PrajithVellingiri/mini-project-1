package com.transport.backend.config;

import com.transport.backend.service.SimulationPersistenceService;
import com.transport.backend.service.SimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Coordinates the deterministic bootstrap lifecycle:
 * 1. Database state inspection and seeding (DatabaseSeeder)
 * 2. Database structural and relational integrity validation (SimulationPersistenceService)
 * 3. Simulation state hydration from database (SimulationService)
 * 4. Application readiness
 */
@Component
@Order(1)
public class SimulationInitializationCoordinator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SimulationInitializationCoordinator.class);

    private final DatabaseSeeder databaseSeeder;
    private final UserSeederService userSeederService;
    private final SimulationPersistenceService persistenceService;
    private final SimulationService simulationService;

    public SimulationInitializationCoordinator(DatabaseSeeder databaseSeeder,
                                               UserSeederService userSeederService,
                                               SimulationPersistenceService persistenceService,
                                               SimulationService simulationService) {
        this.databaseSeeder = databaseSeeder;
        this.userSeederService = userSeederService;
        this.persistenceService = persistenceService;
        this.simulationService = simulationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Starting Smart Transport Bootstrap Lifecycle ===");

        // Step 1: Evaluate database and seed if empty
        log.info("[Bootstrap 1/3] Inspecting database state and seeding baseline if empty...");
        databaseSeeder.seedIfEmpty();
        userSeederService.seedDemoUsersIfEnabled();

        // Step 2: Validate database integrity and load dataset
        log.info("[Bootstrap 2/3] Validating structural/relational database integrity and loading dataset...");
        SimulationPersistenceService.LoadedDataset dataset = persistenceService.loadDataset();

        // Step 3: Hydrate simulation service
        log.info("[Bootstrap 3/3] Hydrating in-memory simulation state from persistent records...");
        simulationService.initializeFromDataset(dataset);

        log.info("=== Smart Transport Bootstrap Lifecycle Complete. System Ready ===");
    }
}
