package com.transport.backend.config;

import com.transport.backend.entity.UserEntity;
import com.transport.backend.model.Role;
import com.transport.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserSeederService {

    private static final Logger log = LoggerFactory.getLogger(UserSeederService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedDemoUsers;

    public UserSeederService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.seed-demo-users:true}") boolean seedDemoUsers
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedDemoUsers = seedDemoUsers;
    }

    @Transactional
    public void seedDemoUsersIfEnabled() {
        if (!seedDemoUsers) {
            log.info("Demo user seeding is disabled (app.security.seed-demo-users=false). Skipping.");
            return;
        }

        record DemoUser(String fullName, String email, Role role, String rawPassword) {}

        List<DemoUser> demoUsers = List.of(
                new DemoUser("System Administrator", "admin@smarttransport.demo", Role.ADMIN, "Review@2026"),
                new DemoUser("Priya Sharma", "passenger@smarttransport.demo", Role.PASSENGER, "Review@2026"),
                new DemoUser("Karthik Raja", "driver@smarttransport.demo", Role.DRIVER, "Review@2026"),
                new DemoUser("Ananya Iyer", "manager@smarttransport.demo", Role.TRANSPORT_MANAGER, "Review@2026")
        );

        int seededCount = 0;
        for (DemoUser demo : demoUsers) {
            if (userRepository.findByEmailIgnoreCase(demo.email()).isEmpty()) {
                UserEntity entity = new UserEntity(
                        demo.fullName(),
                        demo.email().toLowerCase(),
                        passwordEncoder.encode(demo.rawPassword()),
                        demo.role(),
                        true
                );
                userRepository.save(entity);
                seededCount++;
            }
        }

        if (seededCount > 0) {
            log.info("Successfully seeded {} review demo user accounts with BCrypt-hashed credentials.", seededCount);
        } else {
            log.info("Review demo user accounts already present in database. Preserving existing accounts.");
        }
    }
}
