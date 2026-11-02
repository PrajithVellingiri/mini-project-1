package com.transport.backend.repository;

import com.transport.backend.entity.SimulationRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulationRunRepository extends JpaRepository<SimulationRunEntity, Long> {
}
