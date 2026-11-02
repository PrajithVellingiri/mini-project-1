package com.transport.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stops")
public class StopEntity {

    @Id
    @Column(name = "stop_id", length = 20, nullable = false)
    private String stopId;

    @Column(name = "stop_name", length = 120, nullable = false, unique = true)
    private String stopName;

    @Column(name = "is_depot", nullable = false)
    private boolean isDepot;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public StopEntity() {}

    public StopEntity(String stopId, String stopName, boolean isDepot) {
        this.stopId = stopId;
        this.stopName = stopName;
        this.isDepot = isDepot;
    }

    public String getStopId() {
        return stopId;
    }

    public void setStopId(String stopId) {
        this.stopId = stopId;
    }

    public String getStopName() {
        return stopName;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    public boolean isDepot() {
        return isDepot;
    }

    public void setDepot(boolean depot) {
        isDepot = depot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
