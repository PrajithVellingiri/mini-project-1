package com.transport.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "buses")
public class BusEntity {

    @Id
    @Column(name = "bus_id", length = 20, nullable = false)
    private String busId;

    @Column(name = "bus_type", length = 20, nullable = false)
    private String busType;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "can_use_narrow_road", nullable = false)
    private boolean canUseNarrowRoad;

    @Column(name = "current_stop_id", length = 20, nullable = false)
    private String currentStopId;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "passenger_occupancy", nullable = false)
    private int passengerOccupancy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public BusEntity() {}

    public BusEntity(String busId, String busType, int capacity, boolean canUseNarrowRoad, String currentStopId, String status, int passengerOccupancy) {
        this.busId = busId;
        this.busType = busType;
        this.capacity = capacity;
        this.canUseNarrowRoad = canUseNarrowRoad;
        this.currentStopId = currentStopId;
        this.status = status;
        this.passengerOccupancy = passengerOccupancy;
    }

    public String getBusId() {
        return busId;
    }

    public void setBusId(String busId) {
        this.busId = busId;
    }

    public String getBusType() {
        return busType;
    }

    public void setBusType(String busType) {
        this.busType = busType;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isCanUseNarrowRoad() {
        return canUseNarrowRoad;
    }

    public void setCanUseNarrowRoad(boolean canUseNarrowRoad) {
        this.canUseNarrowRoad = canUseNarrowRoad;
    }

    public String getCurrentStopId() {
        return currentStopId;
    }

    public void setCurrentStopId(String currentStopId) {
        this.currentStopId = currentStopId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPassengerOccupancy() {
        return passengerOccupancy;
    }

    public void setPassengerOccupancy(int passengerOccupancy) {
        this.passengerOccupancy = passengerOccupancy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
