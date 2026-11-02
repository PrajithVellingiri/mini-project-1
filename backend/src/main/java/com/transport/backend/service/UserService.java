package com.transport.backend.service;

import com.transport.backend.dto.user.CreateUserRequest;
import com.transport.backend.dto.user.UpdateUserRequest;
import com.transport.backend.dto.user.UserDto;
import com.transport.backend.entity.UserEntity;
import com.transport.backend.model.Role;
import com.transport.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers(String search, Role roleFilter) {
        List<UserEntity> users;
        if (roleFilter != null) {
            users = userRepository.findByRole(roleFilter);
        } else if (search != null && !search.isBlank()) {
            users = userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search.trim(), search.trim());
        } else {
            users = userRepository.findAllByOrderByCreatedAtDesc();
        }

        if (search != null && !search.isBlank() && roleFilter != null) {
            String lowerSearch = search.trim().toLowerCase();
            users = users.stream()
                    .filter(u -> u.getFullName().toLowerCase().contains(lowerSearch) || u.getEmail().toLowerCase().contains(lowerSearch))
                    .toList();
        }

        return users.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + id));
        return toDto(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new IllegalArgumentException("User with email '" + request.email().trim() + "' already exists");
        }

        UserEntity user = new UserEntity(
                request.fullName().trim(),
                request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.role(),
                request.enabled() != null ? request.enabled() : true
        );

        UserEntity saved = userRepository.save(user);
        return toDto(saved);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request, Long currentUserId) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + id));

        String newEmail = request.email().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new IllegalArgumentException("Email '" + newEmail + "' is already in use by another user");
        }

        // Protection: An admin cannot demote themselves from ADMIN
        if (currentUserId != null && currentUserId.equals(user.getId()) && user.getRole() == Role.ADMIN && request.role() != Role.ADMIN) {
            throw new IllegalStateException("You cannot remove your own administrator privileges");
        }

        // Protection: Cannot demote the last active administrator
        if (user.getRole() == Role.ADMIN && request.role() != Role.ADMIN) {
            long activeAdminCount = userRepository.countByRoleAndEnabledTrue(Role.ADMIN);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Cannot demote the last active administrator");
            }
        }

        // Protection: Cannot disable the last active administrator
        boolean requestedEnabled = request.enabled() != null ? request.enabled() : true;
        if (user.getRole() == Role.ADMIN && !requestedEnabled && user.isEnabled()) {
            long activeAdminCount = userRepository.countByRoleAndEnabledTrue(Role.ADMIN);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Cannot disable the last active administrator");
            }
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(newEmail);
        user.setRole(request.role());
        user.setEnabled(requestedEnabled);

        UserEntity updated = userRepository.save(user);
        return toDto(updated);
    }

    @Transactional
    public UserDto toggleUserStatus(Long id, boolean enabled, Long currentUserId) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + id));

        // Protection: An admin cannot disable their own account
        if (currentUserId != null && currentUserId.equals(user.getId()) && !enabled) {
            throw new IllegalStateException("You cannot disable your own administrator account");
        }

        // Protection: Cannot disable the last active administrator
        if (user.getRole() == Role.ADMIN && !enabled && user.isEnabled()) {
            long activeAdminCount = userRepository.countByRoleAndEnabledTrue(Role.ADMIN);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Cannot disable the last active administrator");
            }
        }

        user.setEnabled(enabled);
        UserEntity updated = userRepository.save(user);
        return toDto(updated);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + id));

        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public UserDto toDto(UserEntity user) {
        return new UserDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
