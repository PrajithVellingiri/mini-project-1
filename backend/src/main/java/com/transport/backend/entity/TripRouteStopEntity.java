package com.transport.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "trip_route_stops")
public class TripRouteStopEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "trip_id", length = 20, nullable = false)
    private String tripId;

    @Column(name = "stop_id", length = 20, nullable = false)
    private String stopId;

    @Column(name = "stop_sequence_order", nullable = false)
    private int stopSequenceOrder;

    public TripRouteStopEntity() {}

    public TripRouteStopEntity(String tripId, String stopId, int stopSequenceOrder) {
        this.tripId = tripId;
        this.stopId = stopId;
        this.stopSequenceOrder = stopSequenceOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getStopId() {
        return stopId;
    }

    public void setStopId(String stopId) {
        this.stopId = stopId;
    }

    public int getStopSequenceOrder() {
        return stopSequenceOrder;
    }

    public void setStopSequenceOrder(int stopSequenceOrder) {
        this.stopSequenceOrder = stopSequenceOrder;
    }
}
