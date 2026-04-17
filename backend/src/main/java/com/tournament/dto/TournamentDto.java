package com.tournament.dto;

import com.tournament.entity.Tournament;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TournamentDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        @NotBlank(message = "Tournament name is required")
        private String name;

        @NotBlank(message = "Location is required")
        private String location;

        private String description;

        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        private LocalDate endDate;

        private Tournament.SportType sportType = Tournament.SportType.CRICKET;
        private Tournament.TournamentFormat format = Tournament.TournamentFormat.ROUND_ROBIN;
        private Integer oversPerInnings = 20;
        private Integer maxTeams = 8;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String name;
        private String location;
        private String description;
        private LocalDate startDate;
        private LocalDate endDate;
        private Tournament.SportType sportType;
        private Tournament.TournamentStatus status;
        private Tournament.TournamentFormat format;
        private Integer oversPerInnings;
        private Integer maxTeams;
        private Integer teamsRegistered;
        private Integer matchesPlayed;
        private String organizerName;
        private Long organizerId;
        private LocalDateTime createdAt;
    }
}
