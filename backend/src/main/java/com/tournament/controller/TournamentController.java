package com.tournament.controller;

import com.tournament.dto.ApiResponse;
import com.tournament.dto.TournamentDto;
import com.tournament.entity.Tournament;
import com.tournament.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tournaments")
@RequiredArgsConstructor
@Tag(name = "Tournaments", description = "Tournament management")
public class TournamentController {

    private final TournamentService tournamentService;

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new tournament")
    public ResponseEntity<ApiResponse<TournamentDto.Response>> create(
            @Valid @RequestBody TournamentDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tournamentService.create(request, user.getUsername()), "Tournament created"));
    }

    @GetMapping
    @Operation(summary = "Get all tournaments (public)")
    public ResponseEntity<ApiResponse<Page<TournamentDto.Response>>> getAll(
            @PageableDefault(size = 10, sort = "startDate") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(tournamentService.getAll(pageable)));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent tournaments (public)")
    public ResponseEntity<ApiResponse<List<TournamentDto.Response>>> getRecent() {
        return ResponseEntity.ok(ApiResponse.success(tournamentService.getRecent()));
    }

    @GetMapping("/my")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get organizer's own tournaments")
    public ResponseEntity<ApiResponse<Page<TournamentDto.Response>>> getMine(
            @AuthenticationPrincipal UserDetails user,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                tournamentService.getByOrganizer(user.getUsername(), pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tournament by ID (public)")
    public ResponseEntity<ApiResponse<TournamentDto.Response>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(tournamentService.getById(id)));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update tournament")
    public ResponseEntity<ApiResponse<TournamentDto.Response>> update(
            @PathVariable Long id,
            @Valid @RequestBody TournamentDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(
                tournamentService.update(id, request, user.getUsername()), "Tournament updated"));
    }

    @PatchMapping("/{id}/status")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update tournament status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long id,
            @RequestParam Tournament.TournamentStatus status,
            @AuthenticationPrincipal UserDetails user) {
        tournamentService.updateStatus(id, status, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Status updated to " + status));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete tournament")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        tournamentService.delete(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Tournament deleted"));
    }
}
