package com.transport.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "roads")
public class RoadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "road_id", nullable = false)
    private Long roadId;

    @Column(name = "from_stop_id", length = 20, nullable = false)
    private String fromStopId;

    @Column(name = "to_stop_id", length = 20, nullable = false)
    private String toStopId;

    @Column(name = "travel_time_minutes", columnDefinition = "DECIMAL(8,2)", nullable = false)
    private double travelTimeMinutes;

    @Column(name = "is_narrow_road", nullable = false)
    private boolean isNarrowRoad;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public RoadEntity() {}

    public RoadEntity(String fromStopId, String toStopId, double travelTimeMinutes, boolean isNarrowRoad) {
        this.fromStopId = fromStopId;
        this.toStopId = toStopId;
        this.travelTimeMinutes = travelTimeMinutes;
        this.isNarrowRoad = isNarrowRoad;
    }

    public Long getRoadId() {
        return roadId;
    }

    public void setRoadId(Long roadId) {
        this.roadId = roadId;
    }

    public String getFromStopId() {
        return fromStopId;
    }

    public void setFromStopId(String fromStopId) {
        this.fromStopId = fromStopId;
    }

    public String getToStopId() {
        return toStopId;
    }

    public void setToStopId(String toStopId) {
        this.toStopId = toStopId;
    }

    public double getTravelTimeMinutes() {
        return travelTimeMinutes;
    }

    public void setTravelTimeMinutes(double travelTimeMinutes) {
        this.travelTimeMinutes = travelTimeMinutes;
    }

    public boolean isNarrowRoad() {
        return isNarrowRoad;
    }

    public void setNarrowRoad(boolean narrowRoad) {
        isNarrowRoad = narrowRoad;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
