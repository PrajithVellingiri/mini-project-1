package com.transport.backend.repository;

import com.transport.backend.entity.DemandRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandRequestRepository extends JpaRepository<DemandRequestEntity, String> {
    List<DemandRequestEntity> findAllByOrderByOriginalArrivalOrderAsc();
}
