package com.tournament;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Local Tournament Management System
 * Entry point for the Spring Boot application.
 *
 * Designed for small-town cricket and sports tournament organizers.
 * Replaces manual/WhatsApp-based tournament management.
 */
@SpringBootApplication
@EnableJpaAuditing
public class TournamentApplication {

    public static void main(String[] args) {
        SpringApplication.run(TournamentApplication.class, args);
    }
}
