package com.tournament.service;

import com.tournament.dto.PlayerDto;
import com.tournament.entity.Player;
import com.tournament.entity.Team;
import com.tournament.exception.ResourceNotFoundException;
import com.tournament.repository.PlayerRepository;
import com.tournament.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public PlayerDto.Response addPlayer(PlayerDto.CreateRequest request) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", request.getTeamId()));

        Player player = Player.builder()
                .name(request.getName())
                .jerseyNumber(request.getJerseyNumber())
                .role(request.getRole() != null ? request.getRole() : Player.PlayerRole.ALL_ROUNDER)
                .battingStyle(request.getBattingStyle() != null ? request.getBattingStyle() : Player.BattingStyle.RIGHT_HAND)
                .bowlingStyle(request.getBowlingStyle())
                .phone(request.getPhone())
                .age(request.getAge())
                .team(team)
                // Initialize stats to zero
                .matchesPlayed(0).totalRuns(0).totalWickets(0)
                .highestScore(0).catches(0).halfCenturies(0).centuries(0)
                .build();

        Player saved = playerRepository.save(player);
        log.info("Player '{}' added to team '{}'", saved.getName(), team.getName());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PlayerDto.Response> getByTeam(Long teamId) {
        return playerRepository.findByTeamId(teamId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlayerDto.Response getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public PlayerDto.Response update(Long id, PlayerDto.CreateRequest request) {
        Player player = findById(id);
        player.setName(request.getName());
        player.setJerseyNumber(request.getJerseyNumber());
        player.setRole(request.getRole());
        player.setBattingStyle(request.getBattingStyle());
        player.setBowlingStyle(request.getBowlingStyle());
        player.setPhone(request.getPhone());
        player.setAge(request.getAge());
        return toResponse(playerRepository.save(player));
    }

    @Transactional
    public void delete(Long id) {
        playerRepository.delete(findById(id));
    }

    @Transactional(readOnly = true)
    public List<PlayerDto.Response> getTopBatsmen(Long tournamentId) {
        return playerRepository.findTopBatsmenByTournament(tournamentId)
                .stream().limit(10).map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlayerDto.Response> getTopBowlers(Long tournamentId) {
        return playerRepository.findTopBowlersByTournament(tournamentId)
                .stream().limit(10).map(this::toResponse).collect(Collectors.toList());
    }

    public Player findById(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player", id));
    }

    public PlayerDto.Response toResponse(Player p) {
        return PlayerDto.Response.builder()
                .id(p.getId())
                .name(p.getName())
                .jerseyNumber(p.getJerseyNumber())
                .role(p.getRole())
                .battingStyle(p.getBattingStyle())
                .bowlingStyle(p.getBowlingStyle())
                .phone(p.getPhone())
                .age(p.getAge())
                .teamId(p.getTeam().getId())
                .teamName(p.getTeam().getName())
                .matchesPlayed(p.getMatchesPlayed())
                .totalRuns(p.getTotalRuns())
                .totalWickets(p.getTotalWickets())
                .highestScore(p.getHighestScore())
                .catches(p.getCatches())
                .halfCenturies(p.getHalfCenturies())
                .centuries(p.getCenturies())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
