package com.transport.backend.controller;

import com.transport.backend.dto.auth.AuthResponse;
import com.transport.backend.dto.auth.LoginRequest;
import com.transport.backend.dto.auth.UserProfileDto;
import com.transport.backend.security.CustomUserDetails;
import com.transport.backend.security.JwtService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * POST /api/auth/login
     * Authenticates user credentials, generates a signed JWT, and returns user identity.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(userDetails);

            UserProfileDto profile = new UserProfileDto(
                    userDetails.getId(),
                    userDetails.getFullName(),
                    userDetails.getUsername(),
                    userDetails.getRole(),
                    userDetails.isEnabled()
            );

            AuthResponse response = new AuthResponse(token, jwtService.getExpirationMs(), profile);
            log.info("User {} successfully authenticated with role {}", normalizedEmail, userDetails.getRole());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for {}: Invalid credentials", normalizedEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "message", "Invalid email or password"
            ));
        } catch (DisabledException e) {
            log.warn("Failed login attempt for {}: Account disabled", normalizedEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "message", "Account has been disabled. Please contact an administrator."
            ));
        } catch (Exception e) {
            log.error("Authentication error for {}: {}", normalizedEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "message", "Authentication failed: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/auth/me
     * Returns profile and role information for currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "message", "No active authentication session found"
            ));
        }

        UserProfileDto profile = new UserProfileDto(
                userDetails.getId(),
                userDetails.getFullName(),
                userDetails.getUsername(),
                userDetails.getRole(),
                userDetails.isEnabled()
        );
        return ResponseEntity.ok(profile);
    }

    /**
     * POST /api/auth/logout
     * Signifies client logout. Since JWT is stateless, the client clears local storage.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
