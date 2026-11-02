package com.transport.backend.repository;

import com.transport.backend.entity.TripRouteStopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRouteStopRepository extends JpaRepository<TripRouteStopEntity, Long> {
    List<TripRouteStopEntity> findByTripIdOrderByStopSequenceOrderAsc(String tripId);
}
