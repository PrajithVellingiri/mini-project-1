-- =============================================================================
--  Smart Public Transportation Route Optimization
--  Database Schema and Seed Data
--  Engine: MySQL 8.0.46 (InnoDB, utf8mb4)
--
--  Project:  College Mini Project – Smart Public Transportation Route Optimization
--  Backend:  Java 21 + Spring Boot 4.1.1 (Maven)
--  Frontend: React + Vite
--
--  COMPATIBILITY NOTES (MySQL 8.0.x / 8.0.46)
--  ─────────────────────────────────────────────────────────────────────────────
--  • MySQL 8.0 does NOT support the `CREATE INDEX IF NOT EXISTS` syntax
--    (which produces ERROR 1064 (42000): You have an error in your SQL syntax).
--  • In this script, secondary indexes are cleanly declared INLINE within each
--    `CREATE TABLE IF NOT EXISTS` statement using standard MySQL `INDEX` clauses.
--  • This ensures 100% idempotent, error-free execution on initial setup, and
--    prevents duplicate index creation or syntax errors when rerun.
--  • Tables follow the actual Java entity hierarchy:
--      Stop         → stops
--      Road         → roads
--      Bus (abstract) with MiniBus/StandardBus → buses
--      DemandRequest              → demand_requests
--      ReliefTrip                 → relief_trips
--      ReliefTrip route sequence  → trip_route_stops
--      Road travel-time changes   → road_travel_time_history
--      Simulation run header      → simulation_runs
--
--  • Seed data mirrors SimulationService.resetSimulation() exactly:
--      13 stops  (1 depot + 12 transit stops)
--      38 directed roads (19 bidirectional pairs)
--       4 buses  (2 StandardBus + 2 MiniBus)
--      22 demand batches (520 total passengers)
--
--  EXECUTION INSTRUCTIONS (MySQL Workbench 8.0+)
--  ─────────────────────────────────────────────────────────────────────────────
--  1. Open MySQL Workbench and connect to your local MySQL 8.0.46 server.
--  2. File → Open SQL Script → select this file.
--  3. Click the lightning bolt (⚡) "Execute Script" button  OR  press Ctrl+Shift+Enter.
--  4. Verify the Output panel shows no errors.
--  5. The database `smart_transport_db` is created and selected automatically.
--
--  SAFETY
--  • CREATE DATABASE ... IF NOT EXISTS  – does not drop or re-create an existing database.
--  • CREATE TABLE ... IF NOT EXISTS     – does not modify or drop existing tables.
--  • INSERT IGNORE                      – safe idempotency; existing seed rows are skipped.
-- =============================================================================

-- ── 0. Ensure we are talking to the right schema ─────────────────────────────

CREATE DATABASE IF NOT EXISTS smart_transport_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smart_transport_db;

-- ── 1. stops ──────────────────────────────────────────────────────────────────
-- Represents transit stops and the central depot (Stop.java).
-- stop_id is the application-level string key (e.g. 'DEPOT', 'S1'…'S12').
-- is_depot flags the unique central depot so queries can distinguish it easily.

CREATE TABLE IF NOT EXISTS stops (
    stop_id     VARCHAR(20)  NOT NULL COMMENT 'Application-level stop identifier (e.g. DEPOT, S1)',
    stop_name   VARCHAR(120) NOT NULL COMMENT 'Human-readable stop name',
    is_depot    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1 = central transit depot, 0 = regular stop',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_stops PRIMARY KEY (stop_id),
    CONSTRAINT uq_stop_name UNIQUE (stop_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Transit stops and the central depot (maps to Stop.java)';


-- ── 2. roads ──────────────────────────────────────────────────────────────────
-- Directed weighted edge of the road network (Road.java).
-- Each bidirectional road in Java is stored as two directed rows here.
-- is_narrow_road enforces MiniBus-vs-StandardBus access restrictions.
-- travel_time_minutes carries the CURRENT weight; history is in road_travel_time_history.

CREATE TABLE IF NOT EXISTS roads (
    road_id             BIGINT       NOT NULL AUTO_INCREMENT,
    from_stop_id        VARCHAR(20)  NOT NULL COMMENT 'Source stop (FK → stops)',
    to_stop_id          VARCHAR(20)  NOT NULL COMMENT 'Destination stop (FK → stops)',
    travel_time_minutes DECIMAL(8,2) NOT NULL COMMENT 'Current travel time in minutes (> 0)',
    is_narrow_road      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1 = narrow; StandardBus cannot use this road',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_roads PRIMARY KEY (road_id),
    CONSTRAINT uq_road_direction UNIQUE (from_stop_id, to_stop_id),
    CONSTRAINT fk_roads_from FOREIGN KEY (from_stop_id) REFERENCES stops (stop_id) ON UPDATE CASCADE,
    CONSTRAINT fk_roads_to   FOREIGN KEY (to_stop_id)   REFERENCES stops (stop_id) ON UPDATE CASCADE,
    CONSTRAINT chk_road_travel_time CHECK (travel_time_minutes > 0),
    INDEX idx_roads_from (from_stop_id),
    INDEX idx_roads_to (to_stop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Directed weighted road graph edges (maps to Road.java + RoadNetwork.java)';


-- ── 3. buses ──────────────────────────────────────────────────────────────────
-- Fleet of transit vehicles (Bus.java / MiniBus.java / StandardBus.java).
-- bus_type stores the concrete subclass: 'MINIBUS' or 'STANDARD_BUS'.
-- can_use_narrow_road is derived from the subclass but stored for query convenience.
-- current_stop_id tracks the vehicle's latest known position.

CREATE TABLE IF NOT EXISTS buses (
    bus_id              VARCHAR(20)  NOT NULL COMMENT 'Application-level bus identifier (e.g. BUS-01)',
    bus_type            ENUM('STANDARD_BUS','MINIBUS') NOT NULL COMMENT 'Concrete subclass',
    capacity            INT          NOT NULL COMMENT 'Fixed maximum passenger capacity',
    can_use_narrow_road TINYINT(1)   NOT NULL COMMENT '1 = MiniBus (any road); 0 = StandardBus (no narrow roads)',
    current_stop_id     VARCHAR(20)  NOT NULL COMMENT 'Current or last-known position (FK → stops)',
    status              ENUM('AVAILABLE','IN_TRANSIT','MAINTENANCE','OUT_OF_SERVICE')
                                     NOT NULL DEFAULT 'AVAILABLE' COMMENT 'Operational lifecycle status',
    passenger_occupancy INT          NOT NULL DEFAULT 0 COMMENT 'Passengers currently on board',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_buses PRIMARY KEY (bus_id),
    CONSTRAINT fk_buses_stop FOREIGN KEY (current_stop_id) REFERENCES stops (stop_id) ON UPDATE CASCADE,
    CONSTRAINT chk_bus_capacity      CHECK (capacity > 0),
    CONSTRAINT chk_bus_occupancy_min CHECK (passenger_occupancy >= 0),
    CONSTRAINT chk_bus_occupancy_max CHECK (passenger_occupancy <= capacity),
    INDEX idx_buses_current_stop (current_stop_id),
    INDEX idx_buses_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Transit vehicle fleet (maps to Bus / MiniBus / StandardBus)';


-- ── 4. simulation_runs ────────────────────────────────────────────────────────
-- Header record for each call to POST /api/simulation/run.
-- Allows reporting of per-run passenger accounting (allocated, pending).

CREATE TABLE IF NOT EXISTS simulation_runs (
    run_id                    BIGINT    NOT NULL AUTO_INCREMENT,
    run_at                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the allocation pass was executed',
    allocated_passenger_count INT       NOT NULL DEFAULT 0 COMMENT 'Passengers boarded across all trips in this run',
    pending_passenger_count   INT       NOT NULL DEFAULT 0 COMMENT 'Passengers still waiting after this run',
    assigned_trip_count       INT       NOT NULL DEFAULT 0 COMMENT 'Number of relief trips created in this run',

    CONSTRAINT pk_simulation_runs PRIMARY KEY (run_id),
    CONSTRAINT chk_run_allocated CHECK (allocated_passenger_count >= 0),
    CONSTRAINT chk_run_pending   CHECK (pending_passenger_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Header record per POST /api/simulation/run call';


-- ── 5. demand_requests ────────────────────────────────────────────────────────
-- Passenger batch travelling from a pickup stop to a destination (DemandRequest.java).
--
-- Key design decisions:
-- • original_arrival_order is the application's FIFO tiebreaker – stored faithfully.
-- • remaining_passenger_count tracks how many passengers still need service after
--   partial boarding (≤ original passenger_count).
-- • service_status tracks batch lifecycle: WAITING → PARTIALLY_SERVED → FULLY_SERVED.
-- • corridor_key is a denormalised 'FROM->TO' string matching DemandQueueService
--   grouping logic, making corridor-FIFO queries straightforward.

CREATE TABLE IF NOT EXISTS demand_requests (
    request_id                VARCHAR(40)  NOT NULL COMMENT 'Application-level request ID (e.g. DEM-01)',
    pickup_stop_id            VARCHAR(20)  NOT NULL COMMENT 'Passenger pickup stop (FK → stops)',
    destination_stop_id       VARCHAR(20)  NOT NULL COMMENT 'Passenger destination stop (FK → stops)',
    passenger_count           INT          NOT NULL COMMENT 'Original batch passenger count',
    remaining_passenger_count INT          NOT NULL COMMENT 'Passengers still waiting (= passenger_count when first created)',
    original_arrival_order    BIGINT       NOT NULL COMMENT 'Immutable FIFO rank; lower = arrived earlier',
    corridor_key              VARCHAR(50)  NOT NULL COMMENT 'Denormalised "PICKUP_ID->DEST_ID" for corridor FIFO queries',
    service_status            ENUM('WAITING','PARTIALLY_SERVED','FULLY_SERVED') NOT NULL DEFAULT 'WAITING',
    created_at                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_demand_requests PRIMARY KEY (request_id),
    CONSTRAINT fk_demand_pickup FOREIGN KEY (pickup_stop_id)      REFERENCES stops (stop_id) ON UPDATE CASCADE,
    CONSTRAINT fk_demand_dest   FOREIGN KEY (destination_stop_id) REFERENCES stops (stop_id) ON UPDATE CASCADE,
    CONSTRAINT chk_demand_pax_count       CHECK (passenger_count > 0),
    CONSTRAINT chk_demand_remaining_min   CHECK (remaining_passenger_count >= 0),
    CONSTRAINT chk_demand_remaining_max   CHECK (remaining_passenger_count <= passenger_count),
    CONSTRAINT chk_demand_arrival_order   CHECK (original_arrival_order >= 0),
    INDEX idx_demand_corridor (corridor_key, original_arrival_order),
    INDEX idx_demand_status (service_status),
    INDEX idx_demand_pickup (pickup_stop_id),
    INDEX idx_demand_dest (destination_stop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Passenger demand batches with corridor-FIFO metadata (DemandRequest.java)';


-- ── 6. relief_trips ───────────────────────────────────────────────────────────
-- A relief trip is one bus dispatched to serve one demand batch (ReliefTrip.java).
-- • The route is stored as an ordered sequence in trip_route_stops (child table).
-- • demand_request_id references the original (or remainder) DEM-xx batch.
-- • boarded_passenger_count may be < demand_requests.passenger_count for partial boarding.
-- • simulation_run_id links back to the allocation pass that created this trip.

CREATE TABLE IF NOT EXISTS relief_trips (
    trip_id                       VARCHAR(20)  NOT NULL COMMENT 'Application-level trip ID (e.g. TRIP-0001)',
    bus_id                        VARCHAR(20)  NOT NULL COMMENT 'Assigned bus (FK → buses)',
    demand_request_id             VARCHAR(40)  NOT NULL COMMENT 'Source demand batch (FK → demand_requests)',
    simulation_run_id             BIGINT       NULL     COMMENT 'Allocation pass that created this trip (FK → simulation_runs)',
    origin_stop_id                VARCHAR(20)  NOT NULL COMMENT 'Trip start stop (first stop in route)',
    destination_stop_id           VARCHAR(20)  NOT NULL COMMENT 'Trip terminal stop (last stop in route)',
    boarded_passenger_count       INT          NOT NULL COMMENT 'Passengers who boarded this trip (≤ bus capacity)',
    estimated_travel_time_minutes DECIMAL(8,2) NOT NULL COMMENT 'Total route travel time in minutes',
    status                        ENUM('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')
                                               NOT NULL DEFAULT 'SCHEDULED',
    started_at                    TIMESTAMP    NULL COMMENT 'Timestamp when trip transitioned to IN_PROGRESS',
    completed_at                  TIMESTAMP    NULL COMMENT 'Timestamp when trip was COMPLETED or CANCELLED',
    created_at                    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_relief_trips PRIMARY KEY (trip_id),
    CONSTRAINT fk_trip_bus        FOREIGN KEY (bus_id)             REFERENCES buses (bus_id)             ON UPDATE CASCADE,
    CONSTRAINT fk_trip_demand     FOREIGN KEY (demand_request_id)  REFERENCES demand_requests (request_id) ON UPDATE CASCADE,
    CONSTRAINT fk_trip_run        FOREIGN KEY (simulation_run_id)  REFERENCES simulation_runs (run_id),
    CONSTRAINT fk_trip_origin     FOREIGN KEY (origin_stop_id)     REFERENCES stops (stop_id)            ON UPDATE CASCADE,
    CONSTRAINT fk_trip_dest       FOREIGN KEY (destination_stop_id)REFERENCES stops (stop_id)            ON UPDATE CASCADE,
    CONSTRAINT chk_trip_pax       CHECK (boarded_passenger_count > 0),
    CONSTRAINT chk_trip_time      CHECK (estimated_travel_time_minutes >= 0),
    INDEX idx_trip_bus (bus_id),
    INDEX idx_trip_status (status),
    INDEX idx_trip_run (simulation_run_id),
    INDEX idx_trip_demand (demand_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Relief trip assignments (ReliefTrip.java)';


-- ── 7. trip_route_stops ───────────────────────────────────────────────────────
-- Stores the ordered stop sequence for each trip's combined route
-- (bus dispatch leg + passenger transit leg, with pickup not duplicated).
-- stop_sequence_order starts at 1 for the origin and increments for each subsequent stop.

CREATE TABLE IF NOT EXISTS trip_route_stops (
    id                    BIGINT      NOT NULL AUTO_INCREMENT,
    trip_id               VARCHAR(20) NOT NULL COMMENT 'Owning trip (FK → relief_trips)',
    stop_id               VARCHAR(20) NOT NULL COMMENT 'Stop in the route (FK → stops)',
    stop_sequence_order   INT         NOT NULL COMMENT 'Position in route: 1 = origin, N = terminal destination',

    CONSTRAINT pk_trip_route_stops PRIMARY KEY (id),
    CONSTRAINT uq_trip_stop_order  UNIQUE (trip_id, stop_sequence_order),
    CONSTRAINT fk_trs_trip FOREIGN KEY (trip_id)  REFERENCES relief_trips (trip_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_trs_stop FOREIGN KEY (stop_id)  REFERENCES stops (stop_id)        ON UPDATE CASCADE,
    CONSTRAINT chk_trs_order CHECK (stop_sequence_order >= 1),
    INDEX idx_trs_trip (trip_id),
    INDEX idx_trs_stop (stop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Ordered stop sequence for each relief trip route (ReliefTrip.route list)';


-- ── 8. road_travel_time_history ───────────────────────────────────────────────
-- Audit trail whenever PATCH /api/simulation/roads/{from}/{to}/travel-time is called.
-- The current live value is on roads.travel_time_minutes; this table keeps history.

CREATE TABLE IF NOT EXISTS road_travel_time_history (
    history_id          BIGINT       NOT NULL AUTO_INCREMENT,
    from_stop_id        VARCHAR(20)  NOT NULL COMMENT 'Source stop of the updated road',
    to_stop_id          VARCHAR(20)  NOT NULL COMMENT 'Destination stop of the updated road',
    old_travel_time     DECIMAL(8,2) NOT NULL COMMENT 'Value before the update',
    new_travel_time     DECIMAL(8,2) NOT NULL COMMENT 'Value after the update (> 0)',
    changed_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_road_history PRIMARY KEY (history_id),
    CONSTRAINT fk_rth_from FOREIGN KEY (from_stop_id) REFERENCES stops (stop_id) ON UPDATE CASCADE,
    CONSTRAINT fk_rth_to   FOREIGN KEY (to_stop_id)   REFERENCES stops (stop_id) ON UPDATE CASCADE,
    CONSTRAINT chk_rth_new_time CHECK (new_travel_time > 0),
    INDEX idx_rth_road (from_stop_id, to_stop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit trail for dynamic road travel-time updates';


-- =============================================================================
--  SEED DATA
--  Mirrors SimulationService.resetSimulation() exactly.
--  Uses INSERT IGNORE so re-running the script skips existing rows safely.
-- =============================================================================

-- ── Seed: stops ──────────────────────────────────────────────────────────────
-- 1 depot + 12 transit stops = 13 total (matches summary.totalStops = 13)

INSERT IGNORE INTO stops (stop_id, stop_name, is_depot) VALUES
    ('DEPOT', 'Central Transit Depot', 1),
    ('S1',    'Downtown Hub',          0),
    ('S2',    'Central Station',       0),
    ('S3',    'University Campus',     0),
    ('S4',    'Tech Park North',       0),
    ('S5',    'City Hospital',         0),
    ('S6',    'Market Square',         0),
    ('S7',    'Old Town Historic',     0),
    ('S8',    'South Harbor',          0),
    ('S9',    'Airport Terminal',      0),
    ('S10',   'West End Mall',         0),
    ('S11',   'Industrial Zone',       0),
    ('S12',   'Riverside Park',        0);


-- ── Seed: roads ───────────────────────────────────────────────────────────────
-- 19 bidirectional pairs = 38 directed rows (matches summary.totalRoads = 38).
-- Narrow roads: S6↔S7 and S7↔S1 (is_narrow_road = 1).

-- Depot arterial links
INSERT IGNORE INTO roads (from_stop_id, to_stop_id, travel_time_minutes, is_narrow_road) VALUES
    ('DEPOT', 'S1',  8.0,  0),
    ('S1',  'DEPOT', 8.0,  0),
    ('DEPOT', 'S2',  10.0, 0),
    ('S2',  'DEPOT', 10.0, 0),
    ('DEPOT', 'S11', 12.0, 0),
    ('S11', 'DEPOT', 12.0, 0);

-- Transit corridors
INSERT IGNORE INTO roads (from_stop_id, to_stop_id, travel_time_minutes, is_narrow_road) VALUES
    ('S1',  'S2',   6.0,  0),
    ('S2',  'S1',   6.0,  0),
    ('S1',  'S3',   9.0,  0),
    ('S3',  'S1',   9.0,  0),
    ('S2',  'S4',   11.0, 0),
    ('S4',  'S2',   11.0, 0),
    ('S3',  'S4',   7.0,  0),
    ('S4',  'S3',   7.0,  0),
    ('S3',  'S5',   8.0,  0),
    ('S5',  'S3',   8.0,  0),
    ('S4',  'S9',   14.0, 0),
    ('S9',  'S4',   14.0, 0),
    ('S5',  'S6',   5.0,  0),
    ('S6',  'S5',   5.0,  0);

-- Narrow road corridor (MiniBus only)
INSERT IGNORE INTO roads (from_stop_id, to_stop_id, travel_time_minutes, is_narrow_road) VALUES
    ('S6',  'S7',  4.0, 1),
    ('S7',  'S6',  4.0, 1),
    ('S7',  'S1',  5.0, 1),
    ('S1',  'S7',  5.0, 1);

-- Southern & outer links
INSERT IGNORE INTO roads (from_stop_id, to_stop_id, travel_time_minutes, is_narrow_road) VALUES
    ('S6',  'S8',   10.0, 0),
    ('S8',  'S6',   10.0, 0),
    ('S8',  'S9',   12.0, 0),
    ('S9',  'S8',   12.0, 0),
    ('S2',  'S10',  8.0,  0),
    ('S10', 'S2',   8.0,  0),
    ('S10', 'S11',  9.0,  0),
    ('S11', 'S10',  9.0,  0),
    ('S11', 'S12',  7.0,  0),
    ('S12', 'S11',  7.0,  0),
    ('S12', 'S8',   11.0, 0),
    ('S8',  'S12',  11.0, 0),
    ('S10', 'S12',  6.0,  0),
    ('S12', 'S10',  6.0,  0);


-- ── Seed: buses ───────────────────────────────────────────────────────────────
-- 4 buses: BUS-01 & BUS-02 = StandardBus (wide, no narrow roads)
--          BUS-03 & BUS-04 = MiniBus (compact, all roads)
-- Initial positions match SimulationService:
--   BUS-01,02,03 → DEPOT | BUS-04 → S1 (Downtown Hub)

INSERT IGNORE INTO buses (bus_id, bus_type, capacity, can_use_narrow_road, current_stop_id, status, passenger_occupancy) VALUES
    ('BUS-01', 'STANDARD_BUS', 45, 0, 'DEPOT', 'AVAILABLE', 0),
    ('BUS-02', 'STANDARD_BUS', 40, 0, 'DEPOT', 'AVAILABLE', 0),
    ('BUS-03', 'MINIBUS',      20, 1, 'DEPOT', 'AVAILABLE', 0),
    ('BUS-04', 'MINIBUS',      15, 1, 'S1',    'AVAILABLE', 0);


-- ── Seed: demand_requests ─────────────────────────────────────────────────────
-- 22 demand batches totalling 520 passengers.
-- original_arrival_order = 1..22 (matches DemandRequest constructors in SimulationService).
-- corridor_key mirrors DemandQueueService.getGroupKey() → 'PICKUP_ID->DEST_ID'.
-- All records start as WAITING with remaining_passenger_count = passenger_count.

INSERT IGNORE INTO demand_requests
    (request_id, pickup_stop_id, destination_stop_id, passenger_count, remaining_passenger_count,
     original_arrival_order, corridor_key, service_status)
VALUES
    -- DEM-01: S1→S2, 25 pax, arr=1
    ('DEM-01', 'S1',  'S2',  25, 25,  1,  'S1->S2',  'WAITING'),
    -- DEM-02: S1→S2, 40 pax, arr=2  (same corridor → FIFO behind DEM-01)
    ('DEM-02', 'S1',  'S2',  40, 40,  2,  'S1->S2',  'WAITING'),
    -- DEM-03: S2→S4, 30 pax, arr=3
    ('DEM-03', 'S2',  'S4',  30, 30,  3,  'S2->S4',  'WAITING'),
    -- DEM-04: S3→S9, 35 pax, arr=4
    ('DEM-04', 'S3',  'S9',  35, 35,  4,  'S3->S9',  'WAITING'),
    -- DEM-05: S5→S7, 14 pax, arr=5  (destination via narrow road – MiniBus only)
    ('DEM-05', 'S5',  'S7',  14, 14,  5,  'S5->S7',  'WAITING'),
    -- DEM-06: S4→S2, 20 pax, arr=6
    ('DEM-06', 'S4',  'S2',  20, 20,  6,  'S4->S2',  'WAITING'),
    -- DEM-07: S10→S1, 28 pax, arr=7
    ('DEM-07', 'S10', 'S1',  28, 28,  7,  'S10->S1', 'WAITING'),
    -- DEM-08: S1→S3, 15 pax, arr=8
    ('DEM-08', 'S1',  'S3',  15, 15,  8,  'S1->S3',  'WAITING'),
    -- DEM-09: S8→S9, 22 pax, arr=9
    ('DEM-09', 'S8',  'S9',  22, 22,  9,  'S8->S9',  'WAITING'),
    -- DEM-10: S11→S2, 32 pax, arr=10
    ('DEM-10', 'S11', 'S2',  32, 32, 10,  'S11->S2', 'WAITING'),
    -- DEM-11: S1→S2, 18 pax, arr=11  (third batch on S1→S2 corridor)
    ('DEM-11', 'S1',  'S2',  18, 18, 11,  'S1->S2',  'WAITING'),
    -- DEM-12: S6→S7, 10 pax, arr=12  (narrow historic sector)
    ('DEM-12', 'S6',  'S7',  10, 10, 12,  'S6->S7',  'WAITING'),
    -- DEM-13: S9→S1, 45 pax, arr=13  (high-volume airport demand)
    ('DEM-13', 'S9',  'S1',  45, 45, 13,  'S9->S1',  'WAITING'),
    -- DEM-14: S12→S10, 16 pax, arr=14
    ('DEM-14', 'S12', 'S10', 16, 16, 14,  'S12->S10','WAITING'),
    -- DEM-15: S3→S5, 24 pax, arr=15
    ('DEM-15', 'S3',  'S5',  24, 24, 15,  'S3->S5',  'WAITING'),
    -- DEM-16: S2→S11, 20 pax, arr=16
    ('DEM-16', 'S2',  'S11', 20, 20, 16,  'S2->S11', 'WAITING'),
    -- DEM-17: S10→S8, 18 pax, arr=17
    ('DEM-17', 'S10', 'S8',  18, 18, 17,  'S10->S8', 'WAITING'),
    -- DEM-18: S4→S1, 26 pax, arr=18
    ('DEM-18', 'S4',  'S1',  26, 26, 18,  'S4->S1',  'WAITING'),
    -- DEM-19: S7→S3, 12 pax, arr=19
    ('DEM-19', 'S7',  'S3',  12, 12, 19,  'S7->S3',  'WAITING'),
    -- DEM-20: S5→S9, 30 pax, arr=20
    ('DEM-20', 'S5',  'S9',  30, 30, 20,  'S5->S9',  'WAITING'),
    -- DEM-21: S8→S1, 25 pax, arr=21
    ('DEM-21', 'S8',  'S1',  25, 25, 21,  'S8->S1',  'WAITING'),
    -- DEM-22: S11→S12, 15 pax, arr=22
    ('DEM-22', 'S11', 'S12', 15, 15, 22,  'S11->S12','WAITING');

-- Quick passenger total verification (should equal 520):
-- SELECT SUM(passenger_count) AS total_passengers FROM demand_requests;


-- =============================================================================
--  MIGRATION: ROAD AVAILABILITY PERSISTENCE (PHASE 1 REFINEMENT)
-- =============================================================================
-- The simulation engine explicitly supports dynamic road closures and reopenings
-- (is_open = false removes edges from Dijkstra; is_open = true restores them).
-- To persist road availability states in MySQL across application restarts:
--
-- ALTER TABLE roads 
--   ADD COLUMN is_open TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 = open to transit, 0 = emergency road closure';
--
-- When applied, update RoadEntity.java with @Column(name = "is_open") boolean isOpen.
-- =============================================================================

-- =============================================================================
--  END OF SCHEMA & SEED DATA
-- =============================================================================
