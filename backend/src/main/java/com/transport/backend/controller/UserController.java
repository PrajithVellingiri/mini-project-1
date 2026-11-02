package com.transport.backend.controller;

import com.transport.backend.dto.user.CreateUserRequest;
import com.transport.backend.dto.user.ResetPasswordRequest;
import com.transport.backend.dto.user.UpdateUserRequest;
import com.transport.backend.dto.user.UserDto;
import com.transport.backend.model.Role;
import com.transport.backend.security.CustomUserDetails;
import com.transport.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/users
     * Returns list of all registered users with optional search and role filtering.
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> listUsers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "role", required = false) Role role
    ) {
        return ResponseEntity.ok(userService.getAllUsers(search, role));
    }

    /**
     * GET /api/users/{id}
     * Returns details of a specific user by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * POST /api/users
     * Creates a new database-backed user account with role.
     */
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/users/{id}
     * Updates user personal details and role assignment.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        UserDto updated = userService.updateUser(id, request, currentUserId);
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /api/users/{id}/status
     * Enables or disables a user account.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDto> toggleStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        boolean enabled = body.getOrDefault("enabled", true);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        UserDto updated = userService.toggleUserStatus(id, enabled, currentUserId);
        return ResponseEntity.ok(updated);
    }

    /**
     * POST /api/users/{id}/reset-password
     * Resets a user's password securely with BCrypt hashing.
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable("id") Long id,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        userService.resetPassword(id, request.newPassword());
        return ResponseEntity.ok(Map.of("message", "User password reset successfully"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Bad Request",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Bad Request",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", HttpStatus.NOT_FOUND.value(),
                "error", "Not Found",
                "message", ex.getMessage()
        ));
    }
}
