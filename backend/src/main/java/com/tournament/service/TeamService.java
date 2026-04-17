package com.tournament.service;

import com.tournament.dto.TeamDto;
import com.tournament.entity.Team;
import com.tournament.entity.Tournament;
import com.tournament.exception.BadRequestException;
import com.tournament.exception.DuplicateResourceException;
import com.tournament.exception.ResourceNotFoundException;
import com.tournament.repository.LeaderboardRepository;
import com.tournament.repository.TeamRepository;
import com.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final TournamentRepository tournamentRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final LeaderboardService leaderboardService;

    @Transactional
    public TeamDto.Response addTeam(TeamDto.CreateRequest request) {
        Tournament tournament = tournamentRepository.findById(request.getTournamentId())
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", request.getTournamentId()));

        if (tournament.getStatus() == Tournament.TournamentStatus.COMPLETED) {
            throw new BadRequestException("Cannot add teams to a completed tournament");
        }

        long currentTeams = teamRepository.countByTournamentId(request.getTournamentId());
        if (currentTeams >= tournament.getMaxTeams()) {
            throw new BadRequestException("Tournament is full. Maximum " + tournament.getMaxTeams() + " teams allowed.");
        }

        if (teamRepository.existsByNameAndTournamentId(request.getName(), request.getTournamentId())) {
            throw new DuplicateResourceException("Team '" + request.getName() + "' already exists in this tournament");
        }

        Team team = Team.builder()
                .name(request.getName())
                .captainName(request.getCaptainName())
                .homeGround(request.getHomeGround())
                .colorCode(request.getColorCode())
                .logoUrl(request.getLogoUrl())
                .tournament(tournament)
                .build();

        Team saved = teamRepository.save(team);

        // Create leaderboard entry for this team
        leaderboardService.initializeTeam(tournament, saved);

        log.info("Team '{}' added to tournament '{}'", saved.getName(), tournament.getName());
        return toResponse(saved);
    }

    // ✅ NEW METHOD (THIS FIXES YOUR ERROR)
    @Transactional(readOnly = true)
    public List<TeamDto.Response> getAllTeams() {
        return teamRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeamDto.Response> getTeamsByTournament(Long tournamentId) {
        return teamRepository.findByTournamentId(tournamentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeamDto.Response getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public TeamDto.Response update(Long id, TeamDto.CreateRequest request) {
        Team team = findById(id);
        team.setName(request.getName());
        team.setCaptainName(request.getCaptainName());
        team.setHomeGround(request.getHomeGround());
        team.setColorCode(request.getColorCode());
        team.setLogoUrl(request.getLogoUrl());
        return toResponse(teamRepository.save(team));
    }

    @Transactional
    public void delete(Long id) {
        Team team = findById(id);
        teamRepository.delete(team);
        log.info("Team deleted: {}", id);
    }

    public Team findById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", id));
    }

    public TeamDto.Response toResponse(Team t) {
        return TeamDto.Response.builder()
                .id(t.getId())
                .name(t.getName())
                .captainName(t.getCaptainName())
                .homeGround(t.getHomeGround())
                .colorCode(t.getColorCode())
                .logoUrl(t.getLogoUrl())
                .tournamentId(t.getTournament().getId())
                .tournamentName(t.getTournament().getName())
                .playerCount(t.getPlayers() != null ? t.getPlayers().size() : 0)
                .createdAt(t.getCreatedAt())
                .build();
    }
}