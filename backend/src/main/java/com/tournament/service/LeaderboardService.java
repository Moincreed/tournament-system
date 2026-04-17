package com.tournament.service;

import com.tournament.dto.LeaderboardDto;
import com.tournament.dto.PlayerDto;
import com.tournament.entity.*;
import com.tournament.exception.ResourceNotFoundException;
import com.tournament.repository.LeaderboardRepository;
import com.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;
    private final TournamentRepository tournamentRepository;
    private final PlayerService playerService;

    /**
     * Initialize a leaderboard row when a team is added to a tournament.
     */
    @Transactional
    public void initializeTeam(Tournament tournament, Team team) {
        boolean exists = leaderboardRepository
                .findByTournamentIdAndTeamId(tournament.getId(), team.getId()).isPresent();
        if (!exists) {
            Leaderboard entry = Leaderboard.builder()
                    .tournament(tournament).team(team)
                    .matchesPlayed(0).wins(0).losses(0).draws(0)
                    .points(0).netRunRate(0.0)
                    .totalRunsScored(0).totalOversBatted(0.0)
                    .totalRunsConceded(0).totalOversBowled(0.0)
                    .build();
            leaderboardRepository.save(entry);
        }
    }

    /**
     * Called automatically when a match completes. Updates both teams' rows.
     */
    @Transactional
    public void updateAfterMatch(Match match) {
        if (match.getStatus() != Match.MatchStatus.COMPLETED) return;

        Team teamA = match.getTeamA();
        Team teamB = match.getTeamB();
        Tournament tournament = match.getTournament();

        Leaderboard entryA = getOrCreate(tournament, teamA);
        Leaderboard entryB = getOrCreate(tournament, teamB);

        // Determine batting/bowling stats per team
        Team battingFirst = match.getBattingFirstTeam();
        boolean aIsBattingFirst = battingFirst != null && battingFirst.getId().equals(teamA.getId());

        int runsA, runsB, wicketsA, wicketsB;
        double oversA, oversB;

        if (aIsBattingFirst) {
            runsA = match.getInnings1Runs(); oversA = match.getInnings1Overs();
            runsB = match.getInnings2Runs(); oversB = match.getInnings2Overs();
        } else {
            runsA = match.getInnings2Runs(); oversA = match.getInnings2Overs();
            runsB = match.getInnings1Runs(); oversB = match.getInnings1Overs();
        }

        // Update match stats
        entryA.setMatchesPlayed(entryA.getMatchesPlayed() + 1);
        entryB.setMatchesPlayed(entryB.getMatchesPlayed() + 1);

        // Update running totals for NRR
        entryA.setTotalRunsScored(entryA.getTotalRunsScored() + runsA);
        entryA.setTotalOversBatted(entryA.getTotalOversBatted() + oversA);
        entryA.setTotalRunsConceded(entryA.getTotalRunsConceded() + runsB);
        entryA.setTotalOversBowled(entryA.getTotalOversBowled() + oversB);

        entryB.setTotalRunsScored(entryB.getTotalRunsScored() + runsB);
        entryB.setTotalOversBatted(entryB.getTotalOversBatted() + oversB);
        entryB.setTotalRunsConceded(entryB.getTotalRunsConceded() + runsA);
        entryB.setTotalOversBowled(entryB.getTotalOversBowled() + oversA);

        // Win/Loss/Draw assignment
        if (match.getWinner() == null) {
            // Tie or no result
            entryA.setDraws(entryA.getDraws() + 1);
            entryB.setDraws(entryB.getDraws() + 1);
            entryA.setPoints(entryA.getPoints() + 1);
            entryB.setPoints(entryB.getPoints() + 1);
        } else if (match.getWinner().getId().equals(teamA.getId())) {
            entryA.setWins(entryA.getWins() + 1);
            entryA.setPoints(entryA.getPoints() + 2);
            entryB.setLosses(entryB.getLosses() + 1);
        } else {
            entryB.setWins(entryB.getWins() + 1);
            entryB.setPoints(entryB.getPoints() + 2);
            entryA.setLosses(entryA.getLosses() + 1);
        }

        entryA.recalculateNRR();
        entryB.recalculateNRR();

        leaderboardRepository.save(entryA);
        leaderboardRepository.save(entryB);

        log.info("Leaderboard updated after match {} completion", match.getId());
    }

    @Transactional(readOnly = true)
    public LeaderboardDto.Response getLeaderboard(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));

        List<Leaderboard> standings = leaderboardRepository
                .findByTournamentIdOrderByPointsDescNetRunRateDesc(tournamentId);

        AtomicInteger rank = new AtomicInteger(1);
        List<LeaderboardDto.Entry> entries = standings.stream().map(lb -> {
            LeaderboardDto.Entry entry = LeaderboardDto.Entry.builder()
                    .rank(rank.getAndIncrement())
                    .teamId(lb.getTeam().getId())
                    .teamName(lb.getTeam().getName())
                    .teamColor(lb.getTeam().getColorCode())
                    .matchesPlayed(lb.getMatchesPlayed())
                    .wins(lb.getWins())
                    .losses(lb.getLosses())
                    .draws(lb.getDraws())
                    .noResults(lb.getNoResults())
                    .points(lb.getPoints())
                    .netRunRate(lb.getNetRunRate())
                    .totalRunsScored(lb.getTotalRunsScored())
                    .build();
            return entry;
        }).collect(Collectors.toList());

        List<PlayerDto.Response> topBatsmen = playerService.getTopBatsmen(tournamentId);
        List<PlayerDto.Response> topBowlers = playerService.getTopBowlers(tournamentId);

        return LeaderboardDto.Response.builder()
                .tournamentId(tournamentId)
                .tournamentName(tournament.getName())
                .standings(entries)
                .topBatsmen(topBatsmen.stream().limit(5).collect(Collectors.toList()))
                .topBowlers(topBowlers.stream().limit(5).collect(Collectors.toList()))
                .build();
    }

    private Leaderboard getOrCreate(Tournament tournament, Team team) {
        return leaderboardRepository.findByTournamentIdAndTeamId(tournament.getId(), team.getId())
                .orElseGet(() -> {
                    Leaderboard lb = Leaderboard.builder()
                            .tournament(tournament).team(team)
                            .matchesPlayed(0).wins(0).losses(0).draws(0)
                            .points(0).netRunRate(0.0)
                            .totalRunsScored(0).totalOversBatted(0.0)
                            .totalRunsConceded(0).totalOversBowled(0.0)
                            .build();
                    return leaderboardRepository.save(lb);
                });
    }
}
