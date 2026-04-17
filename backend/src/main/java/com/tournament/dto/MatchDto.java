package com.tournament.dto;

import com.tournament.entity.BallEvent;
import com.tournament.entity.Match;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class MatchDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String publicCode;
        private Long tournamentId;
        private String tournamentName;
        private Integer oversPerInnings;

        private Long teamAId;
        private String teamAName;
        private String teamAColor;

        private Long teamBId;
        private String teamBName;
        private String teamBColor;

        private LocalDate matchDate;
        private LocalTime matchTime;
        private String venue;
        private Integer roundNumber;
        private Integer matchNumber;

        private Match.MatchStatus status;

        private Long tossWinnerId;
        private String tossWinnerName;
        private Match.TossDecision tossDecision;

        private Long battingFirstTeamId;
        private String battingFirstTeamName;

        // Innings 1
        private Integer innings1Runs;
        private Integer innings1Wickets;
        private Double innings1Overs;
        private Double innings1RunRate;

        // Innings 2
        private Integer innings2Runs;
        private Integer innings2Wickets;
        private Double innings2Overs;
        private Double innings2RunRate;

        private Integer currentInnings;
        private Long currentBattingTeamId;
        private String currentBattingTeamName;
        private Integer target;

        private Long winnerId;
        private String winnerName;
        private String resultDescription;

        private String shareLink;
        private LocalDateTime createdAt;
    }

    // ---- Score Update DTOs ----

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TossUpdateRequest {
        @NotNull
        private Long tossWinnerId;
        @NotNull
        private Match.TossDecision decision;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ScoreUpdateRequest {
        @NotNull
        private Integer runsScored;
        private BallEvent.BallType ballType = BallEvent.BallType.NORMAL;
        private boolean isWicket;
        private BallEvent.WicketType wicketType;
        private Long batsmanId;
        private Long bowlerId;
        private Long fielderId;
        private Integer extraRuns;
        private String commentary;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InningsCompleteRequest {
        private String notes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MatchResultRequest {
        private Long winnerId;  // null = draw/no-result
        private String resultDescription;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BallEventResponse {
        private Long id;
        private Integer inningsNumber;
        private Integer overNumber;
        private Integer ballNumber;
        private String batsmanName;
        private String bowlerName;
        private Integer runsScored;
        private BallEvent.BallType ballType;
        private boolean isWicket;
        private BallEvent.WicketType wicketType;
        private Integer extraRuns;
        private String commentary;
        private LocalDateTime createdAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LiveMatchResponse {
        private Response match;
        private List<BallEventResponse> recentBalls;  // Last 6 balls
        private String currentPartnership;
        private Integer requiredRuns;
        private Double requiredRunRate;
        private Integer ballsRemaining;
    }
}
