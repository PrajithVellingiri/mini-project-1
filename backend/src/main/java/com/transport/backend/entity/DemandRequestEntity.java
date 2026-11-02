package com.transport.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "demand_requests")
public class DemandRequestEntity {

    @Id
    @Column(name = "request_id", length = 40, nullable = false)
    private String requestId;

    @Column(name = "pickup_stop_id", length = 20, nullable = false)
    private String pickupStopId;

    @Column(name = "destination_stop_id", length = 20, nullable = false)
    private String destinationStopId;

    @Column(name = "passenger_count", nullable = false)
    private int passengerCount;

    @Column(name = "remaining_passenger_count", nullable = false)
    private int remainingPassengerCount;

    @Column(name = "original_arrival_order", nullable = false)
    private long originalArrivalOrder;

    @Column(name = "corridor_key", length = 50, nullable = false)
    private String corridorKey;

    @Column(name = "service_status", length = 20, nullable = false)
    private String serviceStatus;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public DemandRequestEntity() {}

    public DemandRequestEntity(String requestId, String pickupStopId, String destinationStopId,
                               int passengerCount, int remainingPassengerCount,
                               long originalArrivalOrder, String corridorKey, String serviceStatus) {
        this.requestId = requestId;
        this.pickupStopId = pickupStopId;
        this.destinationStopId = destinationStopId;
        this.passengerCount = passengerCount;
        this.remainingPassengerCount = remainingPassengerCount;
        this.originalArrivalOrder = originalArrivalOrder;
        this.corridorKey = corridorKey;
        this.serviceStatus = serviceStatus;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPickupStopId() {
        return pickupStopId;
    }

    public void setPickupStopId(String pickupStopId) {
        this.pickupStopId = pickupStopId;
    }

    public String getDestinationStopId() {
        return destinationStopId;
    }

    public void setDestinationStopId(String destinationStopId) {
        this.destinationStopId = destinationStopId;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(int passengerCount) {
        this.passengerCount = passengerCount;
    }

    public int getRemainingPassengerCount() {
        return remainingPassengerCount;
    }

    public void setRemainingPassengerCount(int remainingPassengerCount) {
        this.remainingPassengerCount = remainingPassengerCount;
    }

    public long getOriginalArrivalOrder() {
        return originalArrivalOrder;
    }

    public void setOriginalArrivalOrder(long originalArrivalOrder) {
        this.originalArrivalOrder = originalArrivalOrder;
    }

    public String getCorridorKey() {
        return corridorKey;
    }

    public void setCorridorKey(String corridorKey) {
        this.corridorKey = corridorKey;
    }

    public String getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(String serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
