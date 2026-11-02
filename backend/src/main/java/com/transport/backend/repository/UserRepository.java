package com.transport.backend.repository;

import com.transport.backend.entity.UserEntity;
import com.transport.backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRoleAndEnabledTrue(Role role);

    List<UserEntity> findByRole(Role role);

    List<UserEntity> findAllByOrderByCreatedAtDesc();

    List<UserEntity> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
}
