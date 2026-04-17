package com.tournament.config;

import com.tournament.entity.User;
import com.tournament.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once at startup to seed the admin user.
 * Safe to run multiple times — checks before inserting.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedSampleOrganizer();
    }

    private void seedAdminUser() {
        if (!userRepository.existsByEmail("admin@tournament.local")) {
            User admin = User.builder()
                    .name("System Admin")
                    .email("admin@tournament.local")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(User.Role.ADMIN)
                    .active(true)
                    .build();
            userRepository.save(admin);
            log.info("✅ Admin user created: admin@tournament.local / Admin@123");
        }
    }

    private void seedSampleOrganizer() {
        if (!userRepository.existsByEmail("demo@tournament.local")) {
            User demo = User.builder()
                    .name("Demo Organizer")
                    .email("demo@tournament.local")
                    .password(passwordEncoder.encode("Demo@123"))
                    .phone("9876543210")
                    .role(User.Role.ORGANIZER)
                    .active(true)
                    .build();
            userRepository.save(demo);
            log.info("✅ Demo organizer created: demo@tournament.local / Demo@123");
        }
    }
}
