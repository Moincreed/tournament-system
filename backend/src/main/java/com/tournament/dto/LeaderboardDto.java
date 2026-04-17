package com.tournament.dto;

import lombok.*;

import java.util.List;

public class LeaderboardDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Entry {
        private Integer rank;
        private Long teamId;
        private String teamName;
        private String teamColor;
        private Integer matchesPlayed;
        private Integer wins;
        private Integer losses;
        private Integer draws;
        private Integer noResults;
        private Integer points;
        private Double netRunRate;
        private Integer totalRunsScored;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long tournamentId;
        private String tournamentName;
        private List<Entry> standings;
        private List<PlayerDto.Response> topBatsmen;
        private List<PlayerDto.Response> topBowlers;
    }
}
