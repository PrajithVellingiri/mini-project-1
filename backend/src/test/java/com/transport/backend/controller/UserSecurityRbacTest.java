package com.transport.backend.controller;

import com.transport.backend.config.UserSeederService;
import com.transport.backend.entity.UserEntity;
import com.transport.backend.repository.UserRepository;
import com.transport.backend.security.CustomUserDetails;
import com.transport.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class UserSecurityRbacTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSeederService userSeederService;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private String adminToken;
    private String passengerToken;
    private String driverToken;
    private String managerToken;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userSeederService.seedDemoUsersIfEnabled();

        UserEntity admin = userRepository.findByEmailIgnoreCase("admin@smarttransport.demo").orElseThrow();
        UserEntity passenger = userRepository.findByEmailIgnoreCase("passenger@smarttransport.demo").orElseThrow();
        UserEntity driver = userRepository.findByEmailIgnoreCase("driver@smarttransport.demo").orElseThrow();
        UserEntity manager = userRepository.findByEmailIgnoreCase("manager@smarttransport.demo").orElseThrow();

        adminToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(admin));
        passengerToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(passenger));
        driverToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(driver));
        managerToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(manager));
    }

    @Test
    @DisplayName("1. Direct unauthenticated API request is rejected with HTTP 401")
    void testUnauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/api/simulation/state"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("2. PASSENGER can view simulation state and calculate routes")
    void testPassengerPermittedEndpoints() throws Exception {
        mockMvc.perform(get("/api/simulation/state")
                        .header("Authorization", passengerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/simulation/routes?source=S1&destination=S2")
                        .header("Authorization", passengerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("3. PASSENGER cannot trigger simulation allocation (HTTP 403 Forbidden)")
    void testPassengerCannotRunSimulation() throws Exception {
        mockMvc.perform(post("/api/simulation/run")
                        .header("Authorization", passengerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("4. PASSENGER cannot trigger destructive simulation reset (HTTP 403 Forbidden)")
    void testPassengerCannotResetSimulation() throws Exception {
        mockMvc.perform(post("/api/simulation/reset")
                        .header("Authorization", passengerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("5. PASSENGER cannot access User Management (HTTP 403 Forbidden)")
    void testPassengerCannotAccessUserManagement() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", passengerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("6. DRIVER cannot access User Management (HTTP 403 Forbidden)")
    void testDriverCannotAccessUserManagement() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", driverToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. DRIVER cannot reset simulation (HTTP 403 Forbidden)")
    void testDriverCannotResetSimulation() throws Exception {
        mockMvc.perform(post("/api/simulation/reset")
                        .header("Authorization", driverToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("8. DRIVER can update bus operational status (Permitted)")
    void testDriverCanUpdateBusStatus() throws Exception {
        String json = "{\"status\":\"MAINTENANCE\"}";
        mockMvc.perform(patch("/api/simulation/buses/BUS-01/status")
                        .header("Authorization", driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busId").value("BUS-01"));
    }

    @Test
    @DisplayName("9. TRANSPORT_MANAGER can run allocation, monitor demand, and view analytics")
    void testManagerPermittedOperations() throws Exception {
        mockMvc.perform(post("/api/simulation/run")
                        .header("Authorization", managerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/simulation/analytics")
                        .header("Authorization", managerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("10. TRANSPORT_MANAGER cannot reset simulation (HTTP 403 Forbidden)")
    void testManagerCannotResetSimulation() throws Exception {
        mockMvc.perform(post("/api/simulation/reset")
                        .header("Authorization", managerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("11. TRANSPORT_MANAGER cannot access User Management (HTTP 403 Forbidden)")
    void testManagerCannotAccessUserManagement() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12. ADMIN has full access to User Management, Simulation Reset, and Operations")
    void testAdminFullAccess() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/simulation/reset")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }
}
