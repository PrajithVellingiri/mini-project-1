package com.transport.backend.controller;

import com.transport.backend.dto.*;
import com.transport.backend.model.AllocationResult;
import com.transport.backend.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * REST API Controller for Smart Public Transportation Route Optimization simulation.
 */
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /**
     * GET /api/simulation/state
     * Returns full snapshot of stops, roads, buses, demand queue, trips, and summary metrics.
     */
    @GetMapping("/state")
    public ResponseEntity<SimulationStateDto> getState() {
        return ResponseEntity.ok(simulationService.getState());
    }

    /**
     * POST /api/simulation/run
     * Executes capacity-aware allocation pass on current waiting demand.
     */
    @PostMapping("/run")
    public ResponseEntity<AllocationResult> runSimulation() {
        AllocationResult result = simulationService.runSimulation();
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/simulation/reset
     * Restores fresh deterministic initial dataset and returns the fresh state.
     */
    @PostMapping("/reset")
    public ResponseEntity<SimulationStateDto> resetSimulation() {
        simulationService.resetSimulation();
        return ResponseEntity.ok(simulationService.getState());
    }

    /**
     * POST /api/simulation/complete-trips
     * Simulates in-progress trips completing and positioning buses at destinations.
     */
    @PostMapping("/complete-trips")
    public ResponseEntity<SimulationStateDto> completeActiveTrips() {
        simulationService.completeAllActiveTrips();
        return ResponseEntity.ok(simulationService.getState());
    }

    /**
     * POST /api/simulation/trips/{tripId}/complete
     * Completes an individual in-progress trip by ID.
     */
    @PostMapping("/trips/{tripId}/complete")
    public ResponseEntity<SimulationStateDto> completeSpecificTrip(@PathVariable("tripId") String tripId) {
        simulationService.completeTrip(tripId);
        return ResponseEntity.ok(simulationService.getState());
    }

    /**
     * GET /api/simulation/routes?source={id}&destination={id}&vehicleType={type}
     * Calculates minimum travel time route between two stops.
     */
    @GetMapping("/routes")
    public ResponseEntity<RouteResponseDto> getRoute(
            @RequestParam("source") String source,
            @RequestParam("destination") String destination,
            @RequestParam(value = "vehicleType", required = false) String vehicleType
    ) {
        RouteResponseDto route = simulationService.findRoute(source, destination, vehicleType);
        return ResponseEntity.ok(route);
    }

    /**
     * PATCH /api/simulation/roads/{fromId}/{toId}/travel-time
     * Updates road travel time via path parameters.
     */
    @PatchMapping("/roads/{fromId}/{toId}/travel-time")
    public ResponseEntity<Map<String, Object>> updateRoadTravelTimePath(
            @PathVariable("fromId") String fromId,
            @PathVariable("toId") String toId,
            @Valid @RequestBody UpdateTravelTimeRequest request
    ) {
        double travelTime = request.travelTimeMinutes();
        simulationService.updateRoadTravelTime(fromId, toId, travelTime);
        return ResponseEntity.ok(Map.of(
                "message", "Road travel time updated successfully",
                "fromStopId", fromId,
                "toStopId", toId,
                "travelTimeMinutes", travelTime
        ));
    }

    /**
     * PATCH /api/simulation/roads/travel-time
     * Updates road travel time via request body.
     */
    @PatchMapping("/roads/travel-time")
    public ResponseEntity<Map<String, Object>> updateRoadTravelTimeBody(
            @Valid @RequestBody UpdateTravelTimeRequest request
    ) {
        if (request.fromStopId() == null || request.toStopId() == null) {
            throw new IllegalArgumentException("fromStopId and toStopId must be provided in request body");
        }
        simulationService.updateRoadTravelTime(request.fromStopId(), request.toStopId(), request.travelTimeMinutes());
        return ResponseEntity.ok(Map.of(
                "message", "Road travel time updated successfully",
                "fromStopId", request.fromStopId(),
                "toStopId", request.toStopId(),
                "travelTimeMinutes", request.travelTimeMinutes()
        ));
    }

    /**
     * PATCH /api/simulation/roads/status
     * Updates road availability (open or closed to all transit).
     */
    @PatchMapping("/roads/status")
    public ResponseEntity<Map<String, Object>> updateRoadStatus(
            @Valid @RequestBody UpdateRoadStatusRequest request
    ) {
        simulationService.setRoadOpen(
                request.fromStopId(),
                request.toStopId(),
                request.open(),
                request.isBidirectional()
        );
        return ResponseEntity.ok(Map.of(
                "message", request.open() ? "Road opened successfully" : "Road closed successfully",
                "fromStopId", request.fromStopId(),
                "toStopId", request.toStopId(),
                "open", request.open(),
                "bidirectional", request.isBidirectional()
        ));
    }

    /**
     * PATCH /api/simulation/roads/{fromId}/{toId}/status
     * Updates road availability via path parameters.
     */
    @PatchMapping("/roads/{fromId}/{toId}/status")
    public ResponseEntity<Map<String, Object>> updateRoadStatusPath(
            @PathVariable("fromId") String fromId,
            @PathVariable("toId") String toId,
            @RequestParam(value = "open", defaultValue = "false") boolean open,
            @RequestParam(value = "bidirectional", defaultValue = "false") boolean bidirectional
    ) {
        simulationService.setRoadOpen(fromId, toId, open, bidirectional);
        return ResponseEntity.ok(Map.of(
                "message", open ? "Road opened successfully" : "Road closed successfully",
                "fromStopId", fromId,
                "toStopId", toId,
                "open", open,
                "bidirectional", bidirectional
        ));
    }

    /**
     * PATCH /api/simulation/buses/{id}/status
     * Updates bus operational status.
     */
    @PatchMapping("/buses/{id}/status")
    public ResponseEntity<Map<String, Object>> updateBusStatus(
            @PathVariable("id") String busId,
            @Valid @RequestBody BusStatusUpdateRequest request
    ) {
        simulationService.updateBusStatus(busId, request.status());
        return ResponseEntity.ok(Map.of(
                "message", "Bus status updated successfully",
                "busId", busId,
                "status", request.status().toUpperCase()
        ));
    }

    /**
     * POST /api/simulation/demand
     * Dynamically creates and enqueues a new passenger demand batch.
     */
    @PostMapping("/demand")
    public ResponseEntity<Map<String, Object>> createDemand(@Valid @RequestBody CreateDemandRequest request) {
        var created = simulationService.createDemand(
                request.pickupStopId(),
                request.destinationStopId(),
                request.passengerCount()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Passenger demand created and enqueued successfully",
                "requestId", created.getRequestId(),
                "pickupStop", created.getPickupStop().getId(),
                "destinationStop", created.getDestinationStop().getId(),
                "passengerCount", created.getPassengerCount()
        ));
    }

    /**
     * GET /api/simulation/analytics
     * Returns comprehensive operational, passenger conservation, fleet, and algorithmic telemetry analytics.
     */
    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponseDto> getAnalytics() {
        return ResponseEntity.ok(simulationService.getAnalytics());
    }

    /**
     * POST /api/simulation/buses/{id}/breakdown
     * Triggers a breakdown workflow for the specified bus, de-boarding passengers and re-queuing emergency relief.
     */
    @PostMapping("/buses/{id}/breakdown")
    public ResponseEntity<BreakdownResponseDto> triggerBusBreakdown(@PathVariable("id") String busId) {
        BreakdownResponseDto response = simulationService.triggerBusBreakdown(busId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/simulation/buses/{id}/restore
     * Restores a bus from maintenance back to available status.
     */
    @PostMapping("/buses/{id}/restore")
    public ResponseEntity<Map<String, Object>> restoreBus(@PathVariable("id") String busId) {
        Map<String, Object> response = simulationService.restoreBus(busId);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Bad Request",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Not Found",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Bad Request",
                "message", ex.getMessage()
        ));
    }
}
