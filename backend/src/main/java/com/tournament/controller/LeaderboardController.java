package com.tournament.controller;

import com.tournament.dto.ApiResponse;
import com.tournament.dto.LeaderboardDto;
import com.tournament.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Points table and tournament standings")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/{tournamentId}")
    @Operation(summary = "Get full leaderboard with standings and top players (public)")
    public ResponseEntity<ApiResponse<LeaderboardDto.Response>> getLeaderboard(
            @PathVariable Long tournamentId) {
        return ResponseEntity.ok(ApiResponse.success(leaderboardService.getLeaderboard(tournamentId)));
    }
}
