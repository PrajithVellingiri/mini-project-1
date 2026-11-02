package com.transport.backend.repository;

import com.transport.backend.entity.RoadTravelTimeHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoadTravelTimeHistoryRepository extends JpaRepository<RoadTravelTimeHistoryEntity, Long> {
    List<RoadTravelTimeHistoryEntity> findByFromStopIdAndToStopIdOrderByChangedAtDesc(String fromStopId, String toStopId);
}
