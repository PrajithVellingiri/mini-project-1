# Smart Public Transportation Route Optimization
### Intelligent Urban Transit Management, Capacity-Aware Relief Dispatch & Algorithmic Telemetry

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.2-blue.svg)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8.3-purple.svg)](https://vitejs.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0.46-blue.svg)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/Tests-77%20Passed-success.svg)]()
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)]()

An interactive, production-ready urban transit management and route optimization system built for **college evaluation, algorithm demonstration, and public deployment**. 

The system models a complex 13-node, 38-edge directed transportation network with vehicle-restricted corridors, dynamic traffic congestion, greedy capacity-aware fleet dispatching, roadside breakdown workflows, and high-resolution Dijkstra algorithmic telemetry.

---

## 1. Architectural Overview & Tech Stack

```text
                                  ┌────────────────────────────────────────┐
                                  │      React 19 + Vite Frontend SPA      │
                                  │   (Dark Charcoal & Orange Dashboard)   │
                                  └───────────────────┬────────────────────┘
                                                      │  REST API (JSON)
                                                      │  VITE_API_BASE_URL
                                                      ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 Spring Boot 4.1.1 Backend Service (Java 21)                            │
├──────────────────────────────┬──────────────────────────────┬──────────────────────────────────────────┤
│        Domain Models         │      Core Algorithms         │          Persistence & Telemetry         │
│  - Bus (MiniBus, Standard)   │  - Min-Heap Dijkstra Planner │  - SimulationPersistenceService          │
│  - Stop (Depot, Terminals)   │  - High-Res Monotonic Timer  │  - SimulationInitializationCoordinator   │
│  - Road (Directed Graph)     │  - PriorityQueue Demand FIFO │  - 8 Relational JPA Repositories         │
│  - DemandRequest, ReliefTrip │  - Greedy Allocation Engine  │  - Real-time Analytics & Conservation    │
└──────────────────────────────┴──────────────┬───────────────┴──────────────────────────────────────────┘
                                              │  JDBC (HikariCP Pool)
                                              ▼
                                  ┌────────────────────────────────────────┐
                                  │   MySQL 8.0.46 / TiDB Cloud Serverless │
                                  │       8 Relational Schema Tables       │
                                  └────────────────────────────────────────┘
```

### Full-Stack Technologies:
- **Backend**: Java 21 (LTS), Spring Boot 4.1.1 (Spring WebMVC, Spring Validation, Spring Data JPA, HikariCP).
- **Core Algorithms**: Pure Java domain components (zero framework dependencies in routing and graph data structures).
- **Frontend**: React 19, Vite 8, Modular Functional Components, Responsive CSS Grid/Flexbox, Native SVG Rendering.
- **Database**: MySQL 8.0.46 locally, TiDB Cloud Serverless (MySQL 8.0 protocol compatible) for cloud hosting.
- **Testing**: JUnit 5, MockMvc, In-Memory MySQL-mode H2 for isolated automated testing (77 automated tests).

---

## 2. Core Data Structures & Algorithms

### A. Dijkstra Shortest-Travel-Time Routing
- **Graph Topology**: Represented as a directed weighted adjacency list (`Map<String, Map<String, Road>>`) in `RoadNetwork.java`.
- **Cost Metric**: Optimizes strictly for **travel time in minutes** (not spatial distance) using a priority queue min-heap.
- **Vehicle Accessibility Constraints**: Evaluates road compatibility polymorphically (`bus.canUseRoad(road)`). Standard buses (50 capacity) are restricted from narrow corridors (`S6↔S7`, `S7↔S1`), forcing arterial detours; MiniBuses (25 capacity) have universal road access.
- **Dynamic Road Closures**: Emergency closures (`isOpen = false`) remove directed edges from path calculation without modifying underlying road metrics. Reopening immediately restores connectivity.
- **Algorithmic Telemetry**: Every Dijkstra traversal is wrapped with a high-resolution monotonic profiler (`System.nanoTime()`), returning microsecond-level latency ($\mu s$) per calculation and maintaining rolling benchmarks (`totalCalculations`, `averageLatencyMicros`, `minLatencyMicros`, `maxLatencyMicros`).

### B. Dual-Tier PriorityQueue & Corridor FIFO Demand Scheduling
- **Corridor Deque (`LinkedHashMap<String, ArrayDeque<DemandRequest>>`)**: Groups passengers by transit corridor (`pickupStopId -> destinationStopId`), guaranteeing strict First-In-First-Out processing.
- **Candidate PriorityQueue**: Only the head of each corridor deque enters the global priority queue min/max heap.
- **Priority Policy**: Orders demand by passenger batch size descending (largest crowd served first); ties are broken deterministically by ordinal arrival order ascending (earliest arrival first).

### C. Greedy Capacity-Aware Fleet Allocation
- **Dispatch Formulation**: Couples available vehicles (`AVAILABLE` status, available seats $> 0$) with prioritized demand.
- **Composite Routing**: Formulates two-leg composite paths: **Dispatch Leg** (current bus location $\to$ pickup stop) + **Transit Leg** (pickup stop $\to$ destination stop), seamlessly eliminating duplicate junction stops.
- **Partial Boarding Preservation**: If a batch size exceeds available vehicle capacity, the boarded passengers form an active trip, while the remaining passengers (`${requestId}-rem`) retain their exact original arrival order and timestamp at the head of their corridor.

### D. Roadside Breakdown & Emergency Relief Recovery
- **Incident Workflow**: Operator action `POST /api/simulation/buses/{id}/breakdown` transitions vehicle to `MAINTENANCE` and marks active trip `CANCELLED`.
- **Passenger Conservation**: Passengers on board are de-boarded at the bus's current station stop and immediately re-enqueued as an emergency relief demand batch (`DEM-EMG-{tripId}`) with priority arrival order `0L` (top priority in heap).
- **Vehicle Restoration**: `POST /api/simulation/buses/{id}/restore` returns the repaired vehicle back to `AVAILABLE` status.

---

## 3. Mathematical System Invariants

The system rigorously proves and enforces four mathematical invariants across all runs:

1. **Passenger Conservation Invariant**:
   $$\text{Total Recorded Demand} = \text{Waiting Passengers} + \text{In-Transit Passengers} + \text{Completed Passengers} + \text{Cancelled Passengers}$$
   Verified continuously by `SimulationPersistenceService.java` and visually verified on the Analytics Dashboard.
2. **Corridor FIFO Invariant**:
   $$\forall D_1, D_2 \in \text{Corridor}(A \to B) : \text{arrivalOrder}(D_1) < \text{arrivalOrder}(D_2) \implies D_1 \text{ dispatched before or with } D_2$$
3. **Vehicle Capacity Invariant**:
   $$\forall B \in \text{Fleet}, \forall t \ge 0 : \text{passengerOccupancy}(B, t) \le \text{seatCapacity}(B)$$
4. **Road Exclusion Invariant**:
   $$\text{roadType}(R) == \text{NARROW} \implies \text{StandardBus.canUseRoad}(R) == \text{false}$$

---

## 4. REST API Documentation

The backend exposes **16 REST endpoints** supporting full simulation, interactive queries, and incident management:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | Health status and application metadata. |
| `GET` | `/api/simulation/state` | Complete state snapshot (stops, roads, buses, demand queue, trips). |
| `POST` | `/api/simulation/run` | Executes greedy capacity-aware allocation pass. |
| `POST` | `/api/simulation/reset` | Resets in-memory state and restores baseline deterministic dataset. |
| `POST` | `/api/simulation/complete-trips` | Completes all active trips and releases vehicles. |
| `POST` | `/api/simulation/trips/{id}/complete` | Completes a specific trip and releases its assigned bus. |
| `GET` | `/api/simulation/routes` | Computes Dijkstra shortest path with vehicle restrictions & telemetry ($\mu s$). |
| `PATCH` | `/api/simulation/roads/travel-time` | Updates road travel time via JSON body. |
| `PATCH` | `/api/simulation/roads/{from}/{to}/travel-time` | Updates road travel time via path parameters. |
| `PATCH` | `/api/simulation/roads/status` | Toggles road availability (`open: true/false`, `bidirectional`). |
| `PATCH` | `/api/simulation/roads/{from}/{to}/status` | Toggles road availability via path parameters. |
| `PATCH` | `/api/simulation/buses/{id}/status` | Updates vehicle operational status (`AVAILABLE`, `MAINTENANCE`, `OUT_OF_SERVICE`). |
| `POST` | `/api/simulation/demand` | Ingests a dynamic passenger crowd batch into the PriorityQueue. |
| `GET` | `/api/simulation/analytics` | Returns real-time passenger conservation, fleet utilization, and Dijkstra profiler. |
| `POST` | `/api/simulation/buses/{id}/breakdown` | Triggers roadside breakdown, de-boards passengers, and re-queues emergency relief. |
| `POST` | `/api/simulation/buses/{id}/restore` | Restores bus from maintenance back to available fleet. |

---

## 5. Local Setup & Quickstart Guide

### Prerequisites
- **JDK 21** installed (`java -version` returns 21+).
- **Node.js 18+** installed (`node -v`).
- **MySQL 8.0+** running locally (or automated tests using in-memory H2).

### Step 1: Clone and Configure Database
1. Open MySQL Workbench or MySQL CLI:
   ```cmd
   mysql -u root -p < database/smart_transport_db.sql
   ```
2. Verify tables were created:
   ```sql
   USE smart_transport_db;
   SHOW TABLES; -- 8 tables: stops, roads, buses, demand_requests, relief_trips, etc.
   ```

### Step 2: Start the Backend (Spring Boot)
1. Open terminal in `backend/`:
   ```cmd
   cd backend
   set DB_PASSWORD=your_mysql_password
   mvnw.cmd spring-boot:run
   ```
   *(On PowerShell)*:
   ```powershell
   cd backend
   $env:DB_PASSWORD="your_mysql_password"
   .\mvnw.cmd spring-boot:run
   ```
2. Verify backend is running:
   ```text
   curl http://localhost:8080/api/health
   # Response: {"status":"UP","application":"Smart Public Transportation Route Optimization"}
   ```

### Step 3: Start the Frontend (React + Vite)
1. Open a second terminal in `frontend/`:
   ```cmd
   cd frontend
   npm install
   npm run dev
   ```
2. Open your browser at **`http://localhost:5173`**.

---

## 6. Free-Tier Cloud Deployment Guide

The application is structured for zero-cost cloud hosting using:
- **Database**: [TiDB Cloud Serverless](https://tidbcloud.com/) (Permanent free 5GB MySQL 8.0 compatible database, no credit card required).
- **Backend**: [Render.com](https://render.com/) (Free Web Service Docker container, 512MB RAM, dynamic port binding).
- **Frontend**: [Vercel.com](https://vercel.com/) (Free global CDN static hosting, SPA rewrites configured in `vercel.json`).

### Step 1: Deploy Cloud MySQL Database (TiDB Cloud)
1. Sign up for free at [tidbcloud.com](https://tidbcloud.com/) and create a **Serverless Cluster**.
2. Under cluster security, generate your database password and copy the connection string.
3. Open the **SQL Editor** tab in TiDB Cloud and run the contents of [`database/smart_transport_db.sql`](database/smart_transport_db.sql).

### Step 2: Deploy Backend Web Service (Render)
1. Sign up at [render.com](https://render.com/) and connect your GitHub repository.
2. Select **New Web Service** $\to$ connect repository $\to$ select **Docker** environment.
   - **Docker Context**: `backend`
   - **Dockerfile Path**: `backend/Dockerfile`
   - **Plan**: Free
3. Add the following **Environment Variables**:
   - `PORT`: `8080`
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DB_HOST`: `gateway01.us-east-1.prod.aws.tidbcloud.com` *(from TiDB Cloud)*
   - `DB_PORT`: `4000`
   - `DB_NAME`: `smart_transport_db`
   - `DB_USER`: `your_cluster_user.root`
   - `DB_PASSWORD`: `your_cluster_password`
   - `FRONTEND_ORIGIN`: `https://your-frontend-app.vercel.app`
4. Click **Create Web Service**. Once deployed, copy your backend URL (e.g. `https://smart-transport-backend.onrender.com`).

### Step 3: Deploy Frontend SPA (Vercel)
1. Sign up at [vercel.com](https://vercel.com/) and click **Add New Project**.
2. Import your GitHub repository and set **Root Directory** to `frontend`.
   - **Framework Preset**: Vite
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
3. Add Environment Variable:
   - `VITE_API_BASE_URL`: `https://smart-transport-backend.onrender.com` *(your Render backend URL)*
4. Click **Deploy**. Vercel will build and publish your frontend on a public `https://...` URL.

---

## 7. College Evaluation Demonstration Script (5-Minute Workflow)

When presenting this project to evaluators, follow this recommended walkthrough:

1. **Dashboard & Operations Center (1 min)**:
   - Show the Operations Center KPI cards: 13 Transit Stops, 38 Roads, 4 Buses, Queued Batches.
   - Explain the system-wide Passenger Conservation invariant displayed on the top banner.
2. **Interactive Transit Network Map (1 min)**:
   - Navigate to the **Transit Network Map** tab.
   - Show the 13 stations and 38 directed road links.
   - Point out the dashed narrow historic corridors (`S6↔S7` Market Square $\to$ Old Town, and `S7↔S1` Old Town $\to$ Downtown).
   - Point out the live bus markers with their current station locations and available capacity gauges.
3. **Route Optimization Lab & Telemetry (1 min)**:
   - Navigate to **Route Optimization Lab**. Select Origin: `DEPOT`, Destination: `S4`.
   - Demonstrate the high-resolution Dijkstra execution latency badge (e.g. `⚡ 133.1 µs`).
   - Switch Vehicle Mode to `StandardBus` vs `MiniBus` on a narrow road origin (e.g. `S6` to `S1`). Show how the StandardBus is automatically detoured across arterial roads while the MiniBus takes the narrow shortcut.
4. **Traffic Simulation & Road Closures (1 min)**:
   - Navigate to **Traffic Simulation**. Select road `S1 → S2`.
   - Increase travel time by 2.5x congestion; show edge turning red.
   - Toggle **Road Closed**; return to Route Lab and demonstrate how Dijkstra instantly finds an alternative bypass route.
5. **Fleet Dispatch, Breakdown & Recovery (1 min)**:
   - Navigate to **Fleet & Demand Console**. Click **Dispatch Simulation**.
   - Show buses transitioning to `IN_TRANSIT` with boarded passenger badges and illuminated routes on the network map.
   - Simulate a breakdown on active vehicle `BUS-04` by clicking **💥 Breakdown**.
   - Show that the active trip is cancelled, stranded passengers are de-boarded at the current stop and automatically re-enqueued as an emergency relief batch (`DEM-EMG-TRIP-XXXX`) with priority 0.
   - Navigate to the **Analytics Dashboard** to show that Passenger Conservation ($Total = Waiting + In-Transit + Completed + Cancelled$) remains 100% verified without losing a single passenger.

---

## 8. Verification & Test Suite

Run the full automated test suite locally:
```powershell
cd backend
.\mvnw.cmd test
```
**Results**:
- 77 unit, persistence, and controller tests passing (0 failures, 0 errors).
- Tested suites: `RoadNetworkTest`, `RoutePlannerTest`, `DemandQueueServiceTest`, `AllocationServiceTest`, `SimulationControllerTest`, `DatabaseSeederTest`, `SimulationPersistenceHydrationTest`, `BackendApplicationTests`.

Build the frontend bundle:
```powershell
cd frontend
npm run build
```
**Results**:
- 27 modules compiled in 220ms with 0 errors.

---

## 9. Project Directory Structure

```text
smart-transport/
├── .env.example                               # Production environment variable template
├── README.md                                  # Comprehensive documentation & evaluation guide
├── MASTER_PROJECT_BLUEPRINT.md                # System invariants & architectural specifications
├── PHASE_TRACKER.md                           # Milestone tracker across all development phases
├── PROJECT_STATUS.md                          # Live operational audit & version status
├── render.yaml                                # 1-click cloud deployment blueprint for Render.com
│
├── backend/                                   # Spring Boot 4.1.1 Service (Java 21)
│   ├── Dockerfile                             # Multi-stage optimized JRE container
│   ├── .env.example                           # Backend environment variables
│   ├── mvnw / mvnw.cmd                        # Maven wrapper binaries
│   ├── pom.xml                                # Dependencies: Spring Web, JPA, MySQL, H2
│   └── src/
│       ├── main/java/com/transport/backend/
│       │   ├── config/                        # CORS, DatabaseSeeder, StartupCoordinator
│       │   ├── controller/                    # SimulationController (15 REST APIs), Health
│       │   ├── dto/                           # State, Route, Analytics, Breakdown, Telemetry DTOs
│       │   ├── entity/                        # 8 JPA entities mapping to MySQL schema
│       │   ├── model/                         # Pure domain models (Bus, Stop, Road, RouteResult)
│       │   ├── network/                       # RoadNetwork directed graph model
│       │   ├── policy/                        # PriorityQueue ranking strategies
│       │   ├── repository/                    # Spring Data JPA repositories
│       │   └── service/                       # RoutePlanner, DemandQueue, Allocation, Persistence
│       └── test/java/com/transport/backend/   # 77 automated unit & integration test suites
│
├── frontend/                                  # React 19 + Vite 8 Single-Page Application
│   ├── vercel.json                            # Vercel SPA routing rewrite configuration
│   ├── .env.example                           # Frontend environment variables
│   ├── vite.config.js                         # Vite build & local dev proxy setup
│   └── src/
│       ├── api/client.js                      # Centralized API client with VITE_API_BASE_URL
│       ├── components/
│       │   ├── common/                        # Navbar, Badge, StatCard
│       │   ├── overview/                      # OperationsOverview KPI cards & dispatch
│       │   ├── network/                       # TransportationMap interactive SVG network
│       │   ├── routing/                       # RouteOptimizationLab Dijkstra solver & telemetry
│       │   ├── traffic/                       # TrafficConsole congestion & road closure toggles
│       │   ├── fleet/                         # FleetDemandManager table, injector & breakdown
│       │   └── analytics/                     # AnalyticsDashboard conservation equation & profiler
│       ├── App.jsx / App.css                  # Responsive layout & charcoal/orange theme
│       └── main.jsx                           # React root mount
│
└── database/
    └── smart_transport_db.sql                 # Complete MySQL 8.0.46 DDL schema and seed data
```
