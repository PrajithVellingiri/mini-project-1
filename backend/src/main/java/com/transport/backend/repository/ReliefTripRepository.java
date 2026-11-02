package com.transport.backend.repository;

import com.transport.backend.entity.ReliefTripEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReliefTripRepository extends JpaRepository<ReliefTripEntity, String> {
    List<ReliefTripEntity> findByStatus(String status);
}
