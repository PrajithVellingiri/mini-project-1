# Smart Public Transportation Route Optimization
### Intelligent Urban Transit Management, Capacity-Aware Relief Dispatch & Algorithmic Telemetry

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT%20RBAC-blueviolet.svg)](https://spring.io/projects/spring-security)
[![React](https://img.shields.io/badge/React-19.2-blue.svg)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8.3-purple.svg)](https://vitejs.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0.46-blue.svg)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/Tests-112%20Passed-success.svg)]()
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

## 4. User Authentication & Role-Based Access Control (RBAC)

The application features an enterprise-grade, database-backed security architecture built with **Spring Security 6**, **JWT (HMAC-SHA256)**, and **BCrypt (12 rounds)** password hashing. All API endpoints enforce strict role boundaries with stateless session management.

### A. Pre-Seeded Review Demo Accounts
Four pre-configured review demo accounts are seeded into MySQL on first boot for one-click reviewer evaluation:

| Role | Demo Email | Password | Primary Interface / Permissions |
|:---|:---|:---|:---|
| **`ADMIN`** | `admin@smarttransport.demo` | `Review@2026` | Full system access, all simulation controls, plus **User Management Console** (`/users`). |
| **`TRANSPORT_MANAGER`** | `manager@smarttransport.demo` | `Review@2026` | Complete transit operations, road congestion/closures, fleet dispatch, simulation reset. |
| **`DRIVER`** | `driver@smarttransport.demo` | `Review@2026` | **Driver Cockpit**: Vehicle selector, live trip manifest, trip completion, maintenance status. |
| **`PASSENGER`** | `passenger@smarttransport.demo` | `Review@2026` | **Passenger Hub**: Network overview, station finder, Dijkstra route query, service alerts. |

### B. Role-Permission Enforcement Matrix

| Endpoint | HTTP Method | Public | `PASSENGER` | `DRIVER` | `TRANSPORT_MANAGER` | `ADMIN` |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|
| `/api/health` | `GET` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/api/auth/login` | `POST` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/api/auth/me` | `GET` | ❌ | ✅ | ✅ | ✅ | ✅ |
| `/api/auth/logout` | `POST` | ❌ | ✅ | ✅ | ✅ | ✅ |
| `/api/simulation/state` | `GET` | ❌ | ✅ | ✅ | ✅ | ✅ |
| `/api/simulation/routes` | `GET` | ❌ | ✅ | ✅ | ✅ | ✅ |
| `/api/simulation/analytics` | `GET` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `/api/simulation/run` | `POST` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `/api/simulation/reset` | `POST` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `/api/simulation/complete-trips` | `POST` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `/api/simulation/trips/{id}/complete` | `POST` | ❌ | ❌ | ✅ | ✅ | ✅ |
| `/api/simulation/roads/**` | `PATCH` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `/api/simulation/buses/{id}/status` | `PATCH` | ❌ | ❌ | ✅ | ✅ | ✅ |
| `/api/simulation/demand` | `POST` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `/api/simulation/buses/{id}/breakdown` | `POST` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `/api/simulation/buses/{id}/restore` | `POST` | ❌ | ❌ | ✅ | ✅ | ✅ |
| `/api/users/**` (CRUD, status, reset) | `*` | ❌ | ❌ | ❌ | ❌ | ✅ |

### C. Admin User Management Console
Administrators have access to a dedicated `/users` management interface featuring:
- **Search & Filter**: Real-time filtering by user name, email, role, or active status.
- **User Creation**: Dialog to create new accounts with email format validation and password complexity checks.
- **Role Editing**: Modify user roles (`PASSENGER`, `DRIVER`, `TRANSPORT_MANAGER`, `ADMIN`).
- **Status Toggling**: Instant activation/deactivation of accounts with immediate token invalidation.
- **Password Reset**: Direct administrator password override modal.
- **Safety Guards**: Self-protection (admins cannot disable or delete their own active account) and last-admin guard (system prevents demoting or disabling the sole active administrator).

---

## 5. REST API Documentation

The backend exposes **24 REST endpoints** supporting authentication, user administration, simulation, and incident management:

| Method | Endpoint | Access Role | Description |
|---|---|---|---|
| `GET` | `/api/health` | Public | Health status and application metadata. |
| `POST` | `/api/auth/login` | Public | Authenticates credentials and issues signed JWT Bearer token. |
| `GET` | `/api/auth/me` | Authenticated | Returns profile of currently authenticated user. |
| `POST` | `/api/auth/logout` | Authenticated | Stateless token logout hook. |
| `GET` | `/api/users` | `ADMIN` | Lists all users with optional role, enabled status, and search query filters. |
| `POST` | `/api/users` | `ADMIN` | Creates a new user with BCrypt hashed password. |
| `PUT` | `/api/users/{id}` | `ADMIN` | Updates user full name and role assignment. |
| `PATCH` | `/api/users/{id}/status` | `ADMIN` | Toggles user enabled status (`true`/`false`). |
| `POST` | `/api/users/{id}/reset-password` | `ADMIN` | Overrides password for a specific user. |
| `GET` | `/api/simulation/state` | All Roles | Complete state snapshot (stops, roads, buses, demand queue, trips). |
| `POST` | `/api/simulation/run` | Manager / Admin | Executes greedy capacity-aware allocation pass. |
| `POST` | `/api/simulation/reset` | Manager / Admin | Resets in-memory state and restores baseline deterministic dataset. |
| `POST` | `/api/simulation/complete-trips` | Manager / Admin | Completes all active trips and releases vehicles. |
| `POST` | `/api/simulation/trips/{id}/complete` | Driver / Mgr / Admin | Completes a specific trip and releases its assigned bus. |
| `GET` | `/api/simulation/routes` | All Roles | Computes Dijkstra shortest path with vehicle restrictions & telemetry ($\mu s$). |
| `PATCH` | `/api/simulation/roads/travel-time` | Manager / Admin | Updates road travel time via JSON body. |
| `PATCH` | `/api/simulation/roads/{from}/{to}/travel-time` | Manager / Admin | Updates road travel time via path parameters. |
| `PATCH` | `/api/simulation/roads/status` | Manager / Admin | Toggles road availability (`open: true/false`, `bidirectional`). |
| `PATCH` | `/api/simulation/roads/{from}/{to}/status` | Manager / Admin | Toggles road availability via path parameters. |
| `PATCH` | `/api/simulation/buses/{id}/status` | Driver / Mgr / Admin | Updates vehicle operational status (`AVAILABLE`, `MAINTENANCE`, `OUT_OF_SERVICE`). |
| `POST` | `/api/simulation/demand` | Manager / Admin | Ingests a dynamic passenger crowd batch into the PriorityQueue. |
| `GET` | `/api/simulation/analytics` | Manager / Admin | Returns real-time passenger conservation, fleet utilization, and Dijkstra profiler. |
| `POST` | `/api/simulation/buses/{id}/breakdown` | Manager / Admin | Triggers roadside breakdown, de-boards passengers, and re-queues emergency relief. |
| `POST` | `/api/simulation/buses/{id}/restore` | Driver / Mgr / Admin | Restores bus from maintenance back to available fleet. |

---

## 6. Local Development Setup & VS Code Port Forwarding Guide

### Prerequisites
- **JDK 21** installed (`java -version` returns 21+).
- **Node.js 18+** installed (`node -v`).
- **Database**: Local MySQL 8.0+ or an existing TiDB Cloud Serverless instance.

---

### Step 1: Configure Environment Variables (Without Exposing Credentials)

#### A. Database Connection (Local MySQL or TiDB Cloud)
Set your database credentials in your PowerShell session before running the backend:

**Option 1: Connecting to Local MySQL**
```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="smart_transport_db"
$env:DB_USER="root"
$env:DB_PASSWORD="your_mysql_password"
```

**Option 2: Connecting to TiDB Cloud Serverless**
```powershell
$env:DB_HOST="gateway01.us-east-1.prod.aws.tidbcloud.com" # (Your TiDB gateway)
$env:DB_PORT="4000"
$env:DB_NAME="smart_transport_db"
$env:DB_USER="your_cluster_user.root"
$env:DB_PASSWORD="your_tidb_password"
$env:DATABASE_URL="jdbc:mysql://${env:DB_HOST}:${env:DB_PORT}/${env:DB_NAME}?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true"
```

#### B. Frontend API Target
The frontend is pre-configured with `frontend/.env.local`:
```env
VITE_API_BASE_URL=http://localhost:8080
```
*(Leave blank to use the local Vite `/api` proxy, or set to forwarded port/URL if accessing remotely).*

---

### Step 2: Start the Backend (Spring Boot on Port 8080)
Open a new Windows PowerShell terminal in VS Code:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
> **Verification**: Test the health endpoint in your browser or a second terminal:
> ```powershell
> curl http://localhost:8080/api/health
> # Output: {"application":"Smart Public Transportation Route Optimization","status":"UP"}
> ```

---

### Step 3: Start the Frontend (Vite on Port 5173)
Open a separate Windows PowerShell terminal in VS Code:
```powershell
cd frontend
npm install
npm run dev
```
Vite will bind to `0.0.0.0:5173` with network access enabled.

---

### Step 4: VS Code Port Forwarding Setup

In VS Code, navigate to the **Ports** tab (in the bottom panel next to Terminal):

| Service | Local Port | Forwarded Port | Visibility | Purpose |
| :--- | :---: | :---: | :---: | :--- |
| **Spring Boot API** | `8080` | `8080` | Private / Forwarded | Core REST API, Dijkstra route planning, and simulation engine. |
| **React SPA (Vite)** | `5173` | `5173` | Private / Forwarded | Interactive transit dashboard and SVG map. |

1. Click **Add Port** $\to$ enter `8080` $\to$ verify it points to `localhost:8080`.
2. Click **Add Port** $\to$ enter `5173` $\to$ verify it points to `localhost:5173`.
3. Open the forwarded address for port `5173` in your browser.

> **Tip**: You can also launch both services using VS Code's Command Palette (`Ctrl+Shift+P` $\to$ `Tasks: Run Task` $\to$ select `Start Backend (Spring Boot)` or `Start Frontend (Vite)`).

---

### Step 5: Verify End-to-End Local Connectivity
1. Open `http://localhost:5173` (or the forwarded port 5173 URL).
2. The top navigation bar displays a green badge: **Backend Online (X ms)**.
3. Operations Overview shows live KPI cards (13 Stops, 38 Roads, 4 Fleet Buses, 22 Queued Demand Batches).
4. Run an allocation pass or test a route in the Route Optimization Lab to verify full stack execution.

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
- **112 automated unit, integration, persistence, and security tests passing (0 failures, 0 errors)**.
- Tested suites:
  - `AuthControllerTest` (10 tests: login for 4 roles, invalid password, unknown user, disabled account, token validation, me endpoint).
  - `UserSecurityRbacTest` (12 tests: unauthenticated 401s, passenger restrictions, driver capabilities, manager operations, admin full access).
  - `UserManagementTest` (8 tests: listing with filter/search, user creation, duplicate email rejection, role updating, status toggling, self-protection, password reset).
  - `RoadNetworkTest`, `RoutePlannerTest`, `DemandQueueServiceTest`, `AllocationServiceTest`, `SimulationControllerTest`, `DatabaseSeederTest`, `SimulationPersistenceHydrationTest`, `BackendApplicationTests`.

Build and lint the frontend bundle:
```powershell
cd frontend
npm run lint
npm run build
```
**Results**:
- Oxlint: 0 errors across all 19 frontend files.
- Vite: Production bundle compiled cleanly in 418ms with 0 errors.

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
│   ├── pom.xml                                # Spring Boot, Security, JJWT, JPA, MySQL, H2
│   └── src/
│       ├── main/java/com/transport/backend/
│       │   ├── config/                        # CORS, DatabaseSeeder, StartupCoordinator, UserSeeder
│       │   ├── controller/                    # AuthController, UserController, SimulationController, Health
│       │   ├── dto/                           # Auth DTOs, User DTOs, State, Route, Analytics, Telemetry
│       │   ├── entity/                        # UserEntity, Bus, Stop, Road, Trip, Demand, Relief
│       │   ├── model/                         # Pure domain models (Bus, Stop, Road, RouteResult, Role)
│       │   ├── network/                       # RoadNetwork directed graph model
│       │   ├── policy/                        # PriorityQueue ranking strategies
│       │   ├── repository/                    # UserRepository and 8 simulation repositories
│       │   ├── security/                      # JwtService, JwtAuthFilter, CustomUserDetailsService, SecurityConfig
│       │   └── service/                       # UserService, RoutePlanner, DemandQueue, Allocation, Persistence
│       └── test/java/com/transport/backend/   # 112 automated unit, integration & security test suites
│
├── frontend/                                  # React 19 + Vite 8 Single-Page Application
│   ├── vercel.json                            # Vercel SPA routing rewrite configuration
│   ├── .env.example                           # Frontend environment variables
│   ├── vite.config.js                         # Vite build & local dev proxy setup
│   └── src/
│       ├── api/client.js                      # Centralized API client with JWT interceptor & auth endpoints
│       ├── context/                           # AuthContext with role validation & token storage
│       ├── components/
│       │   ├── auth/                          # LoginPage with quick review login cards
│       │   ├── admin/                         # UserManagement console (CRUD, status, password reset)
│       │   ├── passenger/                     # PassengerHub transit view & route finder
│       │   ├── driver/                        # DriverCockpit vehicle selector, manifest & completion
│       │   ├── common/                        # Navbar with role badges & logout, StatCard, Badge, AccessDenied
│       │   ├── overview/                      # OperationsOverview KPI cards & dispatch
│       │   ├── network/                       # TransportationMap interactive SVG network
│       │   ├── routing/                       # RouteOptimizationLab Dijkstra solver & telemetry
│       │   ├── traffic/                       # TrafficConsole congestion & road closure toggles
│       │   ├── fleet/                         # FleetDemandManager table, injector & breakdown
│       │   └── analytics/                     # AnalyticsDashboard conservation equation & profiler
│       ├── App.jsx / App.css                  # Responsive layout & theme with role-tailored view routing
│       └── main.jsx                           # React root mount
│
└── database/
    ├── smart_transport_db.sql                 # Baseline MySQL 8.0.46 DDL schema and seed data
    └── migrations/
        └── V2__add_users_table.sql            # Additive migration for users table
```
