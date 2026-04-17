package com.tournament.dto;

import com.tournament.entity.Player;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class PlayerDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        @NotBlank(message = "Player name is required")
        private String name;

        @NotNull(message = "Team ID is required")
        private Long teamId;

        private Integer jerseyNumber;
        private Player.PlayerRole role;
        private Player.BattingStyle battingStyle;
        private Player.BowlingStyle bowlingStyle;
        private String phone;
        private Integer age;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String name;
        private Integer jerseyNumber;
        private Player.PlayerRole role;
        private Player.BattingStyle battingStyle;
        private Player.BowlingStyle bowlingStyle;
        private String phone;
        private Integer age;
        private Long teamId;
        private String teamName;
        // Stats
        private Integer matchesPlayed;
        private Integer totalRuns;
        private Integer totalWickets;
        private Integer highestScore;
        private Integer catches;
        private Integer halfCenturies;
        private Integer centuries;
        private LocalDateTime createdAt;
    }
}
