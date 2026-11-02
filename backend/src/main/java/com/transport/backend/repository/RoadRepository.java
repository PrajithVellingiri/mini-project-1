package com.transport.backend.repository;

import com.transport.backend.entity.RoadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoadRepository extends JpaRepository<RoadEntity, Long> {
    Optional<RoadEntity> findByFromStopIdAndToStopId(String fromStopId, String toStopId);
}
