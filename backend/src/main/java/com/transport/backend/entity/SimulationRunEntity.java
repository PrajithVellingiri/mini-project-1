package com.transport.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_runs")
public class SimulationRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "run_at", nullable = false)
    private LocalDateTime runAt;

    @Column(name = "allocated_passenger_count", nullable = false)
    private int allocatedPassengerCount;

    @Column(name = "pending_passenger_count", nullable = false)
    private int pendingPassengerCount;

    @Column(name = "assigned_trip_count", nullable = false)
    private int assignedTripCount;

    public SimulationRunEntity() {}

    public SimulationRunEntity(LocalDateTime runAt, int allocatedPassengerCount, int pendingPassengerCount, int assignedTripCount) {
        this.runAt = runAt;
        this.allocatedPassengerCount = allocatedPassengerCount;
        this.pendingPassengerCount = pendingPassengerCount;
        this.assignedTripCount = assignedTripCount;
    }

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public LocalDateTime getRunAt() {
        return runAt;
    }

    public void setRunAt(LocalDateTime runAt) {
        this.runAt = runAt;
    }

    public int getAllocatedPassengerCount() {
        return allocatedPassengerCount;
    }

    public void setAllocatedPassengerCount(int allocatedPassengerCount) {
        this.allocatedPassengerCount = allocatedPassengerCount;
    }

    public int getPendingPassengerCount() {
        return pendingPassengerCount;
    }

    public void setPendingPassengerCount(int pendingPassengerCount) {
        this.pendingPassengerCount = pendingPassengerCount;
    }

    public int getAssignedTripCount() {
        return assignedTripCount;
    }

    public void setAssignedTripCount(int assignedTripCount) {
        this.assignedTripCount = assignedTripCount;
    }
}
