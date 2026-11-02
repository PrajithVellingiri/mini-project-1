package com.transport.backend.controller;

import com.transport.backend.config.UserSeederService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class UserManagementTest {

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
    private UserEntity adminUser;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userSeederService.seedDemoUsersIfEnabled();
        adminUser = userRepository.findByEmailIgnoreCase("admin@smarttransport.demo").orElseThrow();
        adminToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(adminUser));
    }

    @Test
    @DisplayName("1. Admin lists all users with role filtering and search")
    void testListUsersWithFilters() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))));

        mockMvc.perform(get("/api/users?role=DRIVER")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("DRIVER"));

        mockMvc.perform(get("/api/users?search=Priya")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName", containsString("Priya")));
    }

    @Test
    @DisplayName("2. Admin creates a new user successfully")
    void testCreateUser() throws Exception {
        String testEmail = "new.operator@smarttransport.demo";
        userRepository.findByEmailIgnoreCase(testEmail).ifPresent(userRepository::delete);

        String json = "{\"fullName\":\"New Operator\",\"email\":\"" + testEmail + "\",\"password\":\"Secure@2026\",\"role\":\"TRANSPORT_MANAGER\",\"enabled\":true}";

        mockMvc.perform(post("/api/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(testEmail))
                .andExpect(jsonPath("$.role").value("TRANSPORT_MANAGER"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("3. Creating a user with duplicate email returns HTTP 400 Bad Request")
    void testCreateDuplicateUserFails() throws Exception {
        String json = "{\"fullName\":\"Duplicate Admin\",\"email\":\"admin@smarttransport.demo\",\"password\":\"Password123\",\"role\":\"ADMIN\",\"enabled\":true}";

        mockMvc.perform(post("/api/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("4. Admin updates user details and role")
    void testUpdateUser() throws Exception {
        UserEntity driver = userRepository.findByEmailIgnoreCase("driver@smarttransport.demo").orElseThrow();

        String json = "{\"fullName\":\"Karthik Raja Updated\",\"email\":\"driver@smarttransport.demo\",\"role\":\"DRIVER\",\"enabled\":true}";

        mockMvc.perform(put("/api/users/" + driver.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Karthik Raja Updated"));
    }

    @Test
    @DisplayName("5. Admin toggles user active status (disable/enable)")
    void testToggleUserStatus() throws Exception {
        UserEntity passenger = userRepository.findByEmailIgnoreCase("passenger@smarttransport.demo").orElseThrow();

        // Disable
        mockMvc.perform(patch("/api/users/" + passenger.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        // Re-enable
        mockMvc.perform(patch("/api/users/" + passenger.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("6. Protection: Admin cannot disable their own account")
    void testAdminCannotDisableSelf() throws Exception {
        mockMvc.perform(patch("/api/users/" + adminUser.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot disable your own administrator account")));
    }

    @Test
    @DisplayName("7. Protection: Admin cannot remove their own admin privileges")
    void testAdminCannotDemoteSelf() throws Exception {
        String json = "{\"fullName\":\"System Administrator\",\"email\":\"admin@smarttransport.demo\",\"role\":\"PASSENGER\",\"enabled\":true}";

        mockMvc.perform(put("/api/users/" + adminUser.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot remove your own administrator privileges")));
    }

    @Test
    @DisplayName("8. Admin resets user password securely")
    void testResetPassword() throws Exception {
        UserEntity driver = userRepository.findByEmailIgnoreCase("driver@smarttransport.demo").orElseThrow();

        String json = "{\"newPassword\":\"BrandNewPassword@2026\"}";

        mockMvc.perform(post("/api/users/" + driver.getId() + "/reset-password")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("reset successfully")));
    }
}
