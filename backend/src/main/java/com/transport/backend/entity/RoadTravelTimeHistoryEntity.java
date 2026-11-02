package com.transport.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "road_travel_time_history")
public class RoadTravelTimeHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id", nullable = false)
    private Long historyId;

    @Column(name = "from_stop_id", length = 20, nullable = false)
    private String fromStopId;

    @Column(name = "to_stop_id", length = 20, nullable = false)
    private String toStopId;

    @Column(name = "old_travel_time", columnDefinition = "DECIMAL(8,2)", nullable = false)
    private double oldTravelTime;

    @Column(name = "new_travel_time", columnDefinition = "DECIMAL(8,2)", nullable = false)
    private double newTravelTime;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public RoadTravelTimeHistoryEntity() {}

    public RoadTravelTimeHistoryEntity(String fromStopId, String toStopId, double oldTravelTime, double newTravelTime, LocalDateTime changedAt) {
        this.fromStopId = fromStopId;
        this.toStopId = toStopId;
        this.oldTravelTime = oldTravelTime;
        this.newTravelTime = newTravelTime;
        this.changedAt = changedAt;
    }

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
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

    public double getOldTravelTime() {
        return oldTravelTime;
    }

    public void setOldTravelTime(double oldTravelTime) {
        this.oldTravelTime = oldTravelTime;
    }

    public double getNewTravelTime() {
        return newTravelTime;
    }

    public void setNewTravelTime(double newTravelTime) {
        this.newTravelTime = newTravelTime;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
