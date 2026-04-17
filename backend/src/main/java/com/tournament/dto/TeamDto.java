package com.tournament.dto;

import com.tournament.entity.Player;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class TeamDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        @NotBlank(message = "Team name is required")
        private String name;

        @NotNull(message = "Tournament ID is required")
        private Long tournamentId;

        private String captainName;
        private String homeGround;
        private String colorCode;
        private String logoUrl;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String name;
        private String captainName;
        private String homeGround;
        private String colorCode;
        private String logoUrl;
        private Long tournamentId;
        private String tournamentName;
        private Integer playerCount;
        private LocalDateTime createdAt;
    }
}
