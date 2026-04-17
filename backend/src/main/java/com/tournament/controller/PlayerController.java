package com.tournament.controller;

import com.tournament.dto.ApiResponse;
import com.tournament.dto.PlayerDto;
import com.tournament.service.PlayerService;
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
@RequestMapping("/players")
@RequiredArgsConstructor
@Tag(name = "Players", description = "Player management and stats")
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add a player to a team")
    public ResponseEntity<ApiResponse<PlayerDto.Response>> addPlayer(
            @Valid @RequestBody PlayerDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(playerService.addPlayer(request), "Player added"));
    }

    @GetMapping("/team/{teamId}")
    @Operation(summary = "Get all players in a team (public)")
    public ResponseEntity<ApiResponse<List<PlayerDto.Response>>> getByTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(ApiResponse.success(playerService.getByTeam(teamId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get player by ID (public)")
    public ResponseEntity<ApiResponse<PlayerDto.Response>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(playerService.getById(id)));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update player details")
    public ResponseEntity<ApiResponse<PlayerDto.Response>> update(
            @PathVariable Long id,
            @Valid @RequestBody PlayerDto.CreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(playerService.update(id, request), "Player updated"));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remove a player")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        playerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Player deleted"));
    }

    @GetMapping("/tournament/{tournamentId}/top-batsmen")
    @Operation(summary = "Top 10 batsmen in a tournament (public)")
    public ResponseEntity<ApiResponse<List<PlayerDto.Response>>> topBatsmen(
            @PathVariable Long tournamentId) {
        return ResponseEntity.ok(ApiResponse.success(playerService.getTopBatsmen(tournamentId)));
    }

    @GetMapping("/tournament/{tournamentId}/top-bowlers")
    @Operation(summary = "Top 10 bowlers in a tournament (public)")
    public ResponseEntity<ApiResponse<List<PlayerDto.Response>>> topBowlers(
            @PathVariable Long tournamentId) {
        return ResponseEntity.ok(ApiResponse.success(playerService.getTopBowlers(tournamentId)));
    }
}
