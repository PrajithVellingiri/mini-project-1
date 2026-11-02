package com.transport.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "relief_trips")
public class ReliefTripEntity {

    @Id
    @Column(name = "trip_id", length = 20, nullable = false)
    private String tripId;

    @Column(name = "bus_id", length = 20, nullable = false)
    private String busId;

    @Column(name = "demand_request_id", length = 40, nullable = false)
    private String demandRequestId;

    @Column(name = "simulation_run_id")
    private Long simulationRunId;

    @Column(name = "origin_stop_id", length = 20, nullable = false)
    private String originStopId;

    @Column(name = "destination_stop_id", length = 20, nullable = false)
    private String destinationStopId;

    @Column(name = "boarded_passenger_count", nullable = false)
    private int boardedPassengerCount;

    @Column(name = "estimated_travel_time_minutes", columnDefinition = "DECIMAL(8,2)", nullable = false)
    private double estimatedTravelTimeMinutes;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public ReliefTripEntity() {}

    public ReliefTripEntity(String tripId, String busId, String demandRequestId, Long simulationRunId,
                            String originStopId, String destinationStopId, int boardedPassengerCount,
                            double estimatedTravelTimeMinutes, String status,
                            LocalDateTime startedAt, LocalDateTime completedAt) {
        this.tripId = tripId;
        this.busId = busId;
        this.demandRequestId = demandRequestId;
        this.simulationRunId = simulationRunId;
        this.originStopId = originStopId;
        this.destinationStopId = destinationStopId;
        this.boardedPassengerCount = boardedPassengerCount;
        this.estimatedTravelTimeMinutes = estimatedTravelTimeMinutes;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getBusId() {
        return busId;
    }

    public void setBusId(String busId) {
        this.busId = busId;
    }

    public String getDemandRequestId() {
        return demandRequestId;
    }

    public void setDemandRequestId(String demandRequestId) {
        this.demandRequestId = demandRequestId;
    }

    public Long getSimulationRunId() {
        return simulationRunId;
    }

    public void setSimulationRunId(Long simulationRunId) {
        this.simulationRunId = simulationRunId;
    }

    public String getOriginStopId() {
        return originStopId;
    }

    public void setOriginStopId(String originStopId) {
        this.originStopId = originStopId;
    }

    public String getDestinationStopId() {
        return destinationStopId;
    }

    public void setDestinationStopId(String destinationStopId) {
        this.destinationStopId = destinationStopId;
    }

    public int getBoardedPassengerCount() {
        return boardedPassengerCount;
    }

    public void setBoardedPassengerCount(int boardedPassengerCount) {
        this.boardedPassengerCount = boardedPassengerCount;
    }

    public double getEstimatedTravelTimeMinutes() {
        return estimatedTravelTimeMinutes;
    }

    public void setEstimatedTravelTimeMinutes(double estimatedTravelTimeMinutes) {
        this.estimatedTravelTimeMinutes = estimatedTravelTimeMinutes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
