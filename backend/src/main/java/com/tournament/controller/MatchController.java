package com.tournament.controller;

import com.tournament.dto.ApiResponse;
import com.tournament.dto.MatchDto;
import com.tournament.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
@Tag(name = "Matches & Scoring", description = "Match management, fixture generation and live scoring")
public class MatchController {

    private final MatchService matchService;

    // ---- Fixture Generation ----
    @PostMapping("/generate/{tournamentId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Generate round-robin fixtures for a tournament")
    public ResponseEntity<ApiResponse<List<MatchDto.Response>>> generateFixtures(
            @PathVariable Long tournamentId) {
        return ResponseEntity.ok(ApiResponse.success(
                matchService.generateFixtures(tournamentId), "Fixtures generated successfully"));
    }

    // ---- Read ----
    @GetMapping("/tournament/{tournamentId}")
    @Operation(summary = "Get all matches for a tournament (public)")
    public ResponseEntity<ApiResponse<List<MatchDto.Response>>> getByTournament(
            @PathVariable Long tournamentId) {
        return ResponseEntity.ok(ApiResponse.success(matchService.getByTournament(tournamentId)));
    }

    @GetMapping("/live")
    @Operation(summary = "Get all currently live matches (public)")
    public ResponseEntity<ApiResponse<List<MatchDto.Response>>> getLive() {
        return ResponseEntity.ok(ApiResponse.success(matchService.getLiveMatches()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get match details by ID (public)")
    public ResponseEntity<ApiResponse<MatchDto.Response>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(matchService.toResponse(matchService.findById(id))));
    }

    @GetMapping("/{id}/live")
    @Operation(summary = "Get live match with recent balls and run chase stats (public)")
    public ResponseEntity<ApiResponse<MatchDto.LiveMatchResponse>> getLiveDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(matchService.getLiveMatch(id)));
    }

    @GetMapping("/public/{code}")
    @Operation(summary = "Get match by shareable public code (public)")
    public ResponseEntity<ApiResponse<MatchDto.Response>> getByPublicCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(matchService.getByPublicCode(code)));
    }

    // ---- Toss ----
    @PatchMapping("/{id}/toss")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Record toss result and start match")
    public ResponseEntity<ApiResponse<MatchDto.Response>> updateToss(
            @PathVariable Long id,
            @Valid @RequestBody MatchDto.TossUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(matchService.updateToss(id, request), "Toss recorded, match is now LIVE"));
    }

    // ---- Live Scoring ----
    @PostMapping("/{id}/score")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Record a ball event (runs, wicket, wide, etc.)")
    public ResponseEntity<ApiResponse<MatchDto.Response>> updateScore(
            @PathVariable Long id,
            @Valid @RequestBody MatchDto.ScoreUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(matchService.updateScore(id, request), "Score updated"));
    }

    @PatchMapping("/{id}/innings2/start")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Start second innings (after innings break)")
    public ResponseEntity<ApiResponse<MatchDto.Response>> startInnings2(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(matchService.startInnings2(id), "2nd innings started"));
    }

    // ---- Result ----
    @PatchMapping("/{id}/result")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Declare match result (manual override)")
    public ResponseEntity<ApiResponse<MatchDto.Response>> declareResult(
            @PathVariable Long id,
            @RequestBody MatchDto.MatchResultRequest request) {
        return ResponseEntity.ok(ApiResponse.success(matchService.declareResult(id, request), "Result declared"));
    }
}
