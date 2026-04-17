package com.tournament.controller;

import com.tournament.dto.ApiResponse;
import com.tournament.dto.TeamDto;
import com.tournament.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Team management")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add a team to a tournament")
    public ResponseEntity<ApiResponse<TeamDto.Response>> addTeam(
            @Valid @RequestBody TeamDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(teamService.addTeam(request), "Team added"));
    }

    @GetMapping("/tournament/{tournamentId}")
    @Operation(summary = "Get all teams in a tournament (public)")
    public ResponseEntity<ApiResponse<List<TeamDto.Response>>> getByTournament(
            @PathVariable Long tournamentId) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamsByTournament(tournamentId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get team by ID (public)")
    public ResponseEntity<ApiResponse<TeamDto.Response>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getById(id)));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update team details")
    public ResponseEntity<ApiResponse<TeamDto.Response>> update(
            @PathVariable Long id,
            @Valid @RequestBody TeamDto.CreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(teamService.update(id, request), "Team updated"));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remove a team")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        teamService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Team deleted"));
    }
}
