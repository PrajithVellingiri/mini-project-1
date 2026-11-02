package com.transport.backend.controller;

import com.transport.backend.config.UserSeederService;
import com.transport.backend.dto.auth.LoginRequest;
import com.transport.backend.entity.UserEntity;
import com.transport.backend.model.Role;
import com.transport.backend.repository.UserRepository;
import com.transport.backend.security.CustomUserDetails;
import com.transport.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSeederService userSeederService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        userSeederService.seedDemoUsersIfEnabled();
    }

    @Test
    @DisplayName("1. Successful login for seeded Admin returns JWT token and UserProfile")
    void testSuccessfulAdminLogin() throws Exception {
        String json = "{\"email\":\"admin@smarttransport.demo\",\"password\":\"Review@2026\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInMs").value(greaterThan(0)))
                .andExpect(jsonPath("$.user.email").value("admin@smarttransport.demo"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.enabled").value(true));
    }

    @Test
    @DisplayName("2. Successful login for seeded Passenger returns passenger role")
    void testSuccessfulPassengerLogin() throws Exception {
        String json = "{\"email\":\"passenger@smarttransport.demo\",\"password\":\"Review@2026\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("PASSENGER"));
    }

    @Test
    @DisplayName("3. Successful login for seeded Driver returns driver role")
    void testSuccessfulDriverLogin() throws Exception {
        String json = "{\"email\":\"driver@smarttransport.demo\",\"password\":\"Review@2026\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("DRIVER"));
    }

    @Test
    @DisplayName("4. Successful login for seeded Transport Manager returns manager role")
    void testSuccessfulManagerLogin() throws Exception {
        String json = "{\"email\":\"manager@smarttransport.demo\",\"password\":\"Review@2026\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("TRANSPORT_MANAGER"));
    }

    @Test
    @DisplayName("5. Login with invalid password returns HTTP 401 Unauthorized")
    void testLoginWithWrongPassword() throws Exception {
        String json = "{\"email\":\"admin@smarttransport.demo\",\"password\":\"WrongPassword123\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("6. Login with unknown email returns HTTP 401 Unauthorized")
    void testLoginWithUnknownEmail() throws Exception {
        String json = "{\"email\":\"unknown.ghost@smarttransport.demo\",\"password\":\"Review@2026\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("7. Login for disabled account returns HTTP 401 Account Disabled")
    void testLoginForDisabledAccount() throws Exception {
        // Create a disabled test user
        String email = "disabled.user@smarttransport.demo";
        userRepository.findByEmailIgnoreCase(email).ifPresent(userRepository::delete);
        UserEntity disabledUser = new UserEntity(
                "Disabled Tester",
                email,
                passwordEncoder.encode("Password@123"),
                Role.PASSENGER,
                false // disabled
        );
        userRepository.save(disabledUser);

        String json = "{\"email\":\"" + email + "\",\"password\":\"Password@123\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Account has been disabled")));
    }

    @Test
    @DisplayName("8. GET /api/auth/me returns current user profile when valid JWT provided")
    void testGetCurrentUserWithValidToken() throws Exception {
        UserEntity admin = userRepository.findByEmailIgnoreCase("admin@smarttransport.demo").orElseThrow();
        String token = "Bearer " + jwtService.generateToken(new CustomUserDetails(admin));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@smarttransport.demo"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("9. GET /api/auth/me returns HTTP 401 when no token provided")
    void testGetCurrentUserUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("10. Request with expired or invalid token returns HTTP 401")
    void testRequestWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.malformed.token"))
                .andExpect(status().isUnauthorized());
    }
}
