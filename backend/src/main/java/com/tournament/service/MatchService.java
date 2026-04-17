package com.tournament.service;

import com.tournament.dto.MatchDto;
import com.tournament.entity.*;
import com.tournament.exception.BadRequestException;
import com.tournament.exception.ResourceNotFoundException;
import com.tournament.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final BallEventRepository ballEventRepository;
    private final LeaderboardService leaderboardService;
    private final SimpMessagingTemplate messagingTemplate;

    // ================================================================
    // FIXTURE GENERATION — Round Robin
    // ================================================================
    @Transactional
    public List<MatchDto.Response> generateFixtures(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));

        List<Team> teams = teamRepository.findByTournamentId(tournamentId);
        if (teams.size() < 2) {
            throw new BadRequestException("Need at least 2 teams to generate fixtures");
        }

        // Check if fixtures already exist
        long existing = matchRepository.countByTournamentId(tournamentId);
        if (existing > 0) {
            throw new BadRequestException("Fixtures already generated. Delete existing matches first.");
        }

        List<Match> matches = new ArrayList<>();
        int matchNumber = 1;
        LocalDate matchDate = tournament.getStartDate();

        // Round-robin: each team plays every other team once
        for (int i = 0; i < teams.size() - 1; i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                Match match = Match.builder()
                        .tournament(tournament)
                        .teamA(teams.get(i))
                        .teamB(teams.get(j))
                        .matchDate(matchDate)
                        .matchNumber(matchNumber++)
                        .roundNumber((i * (teams.size() - 1 - i) + j - i))
                        .venue(tournament.getLocation())
                        .status(Match.MatchStatus.UPCOMING)
                        .innings1Runs(0).innings1Wickets(0).innings1Overs(0.0)
                        .innings2Runs(0).innings2Wickets(0).innings2Overs(0.0)
                        .currentInnings(1)
                        .build();
                matches.add(match);
                // Spread matches: 2 per day
                if (matchNumber % 2 == 0) matchDate = matchDate.plusDays(1);
            }
        }

        List<Match> saved = matchRepository.saveAll(matches);
        tournament.setStatus(Tournament.TournamentStatus.ONGOING);
        tournamentRepository.save(tournament);

        log.info("Generated {} fixtures for tournament '{}'", saved.size(), tournament.getName());
        return saved.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ================================================================
    // TOSS
    // ================================================================
    @Transactional
    public MatchDto.Response updateToss(Long matchId, MatchDto.TossUpdateRequest request) {
        Match match = findById(matchId);

        if (match.getStatus() != Match.MatchStatus.UPCOMING) {
            throw new BadRequestException("Toss can only be set for upcoming matches");
        }

        Team tossWinner = teamRepository.findById(request.getTossWinnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", request.getTossWinnerId()));

        match.setTossWinner(tossWinner);
        match.setTossDecision(request.getDecision());

        // Determine who bats first
        Team battingFirst;
        if (request.getDecision() == Match.TossDecision.BAT) {
            battingFirst = tossWinner;
        } else {
            battingFirst = match.getTeamA().equals(tossWinner) ? match.getTeamB() : match.getTeamA();
        }

        match.setBattingFirstTeam(battingFirst);
        match.setCurrentBattingTeam(battingFirst);
        match.setStatus(Match.MatchStatus.LIVE);

        Match saved = matchRepository.save(match);
        broadcastUpdate(saved);
        return toResponse(saved);
    }

    // ================================================================
    // LIVE SCORING — Ball by Ball
    // ================================================================
    @Transactional
    public MatchDto.Response updateScore(Long matchId, MatchDto.ScoreUpdateRequest request) {
        Match match = findById(matchId);

        if (match.getStatus() != Match.MatchStatus.LIVE) {
            throw new BadRequestException("Match is not live. Current status: " + match.getStatus());
        }

        int innings = match.getCurrentInnings();
        boolean isExtra = request.getBallType() != BallEvent.BallType.NORMAL;
        boolean countsBall = request.getBallType() == BallEvent.BallType.NORMAL
                || request.getBallType() == BallEvent.BallType.BYE
                || request.getBallType() == BallEvent.BallType.LEG_BYE;

        int runs = request.getRunsScored() + (request.getExtraRuns() != null ? request.getExtraRuns() : 0);

        // Save ball event
        BallEvent ball = BallEvent.builder()
                .match(match)
                .inningsNumber(innings)
                .runsScored(request.getRunsScored())
                .ballType(request.getBallType())
                .isWicket(request.isWicket())
                .wicketType(request.getWicketType())
                .extraRuns(request.getExtraRuns() != null ? request.getExtraRuns() : 0)
                .commentary(request.getCommentary())
                .build();

        // Set over/ball numbers
        if (innings == 1) {
            int[] ob = calculateOverBall(match.getInnings1Overs());
            ball.setOverNumber(ob[0]);
            ball.setBallNumber(ob[1] + 1);
        } else {
            int[] ob = calculateOverBall(match.getInnings2Overs());
            ball.setOverNumber(ob[0]);
            ball.setBallNumber(ob[1] + 1);
        }

        if (request.getBatsmanId() != null) {
            ball.setBatsman(new Player());
            ball.getBatsman().setId(request.getBatsmanId());
        }
        if (request.getBowlerId() != null) {
            ball.setBowler(new Player());
            ball.getBowler().setId(request.getBowlerId());
        }

        ballEventRepository.save(ball);

        // Update innings stats
        int maxWickets = 10;
        Tournament tournament = match.getTournament();
        int maxOvers = tournament.getOversPerInnings();

        if (innings == 1) {
            match.setInnings1Runs(match.getInnings1Runs() + runs);
            if (request.isWicket()) {
                match.setInnings1Wickets(match.getInnings1Wickets() + 1);
            }
            if (countsBall) {
                match.setInnings1Overs(incrementOvers(match.getInnings1Overs()));
            }
            double overs = match.getInnings1Overs();
            match.setInnings1RunRate(overs > 0 ? Math.round((match.getInnings1Runs() / overs) * 100.0) / 100.0 : 0.0);

            // Check if innings 1 complete
            boolean allOut = match.getInnings1Wickets() >= maxWickets;
            boolean oversComplete = getCompletedOvers(match.getInnings1Overs()) >= maxOvers;

            if (allOut || oversComplete) {
                match.setCurrentInnings(2);
                match.setTarget(match.getInnings1Runs() + 1);
                Team battingSecond = match.getBattingFirstTeam().equals(match.getTeamA())
                        ? match.getTeamB() : match.getTeamA();
                match.setCurrentBattingTeam(battingSecond);
                match.setStatus(Match.MatchStatus.INNINGS_BREAK);
                log.info("Match {} - Innings 1 complete. Target: {}", matchId, match.getTarget());
            }

        } else {
            match.setInnings2Runs(match.getInnings2Runs() + runs);
            if (request.isWicket()) {
                match.setInnings2Wickets(match.getInnings2Wickets() + 1);
            }
            if (countsBall) {
                match.setInnings2Overs(incrementOvers(match.getInnings2Overs()));
            }
            double overs = match.getInnings2Overs();
            match.setInnings2RunRate(overs > 0 ? Math.round((match.getInnings2Runs() / overs) * 100.0) / 100.0 : 0.0);

            boolean allOut = match.getInnings2Wickets() >= maxWickets;
            boolean oversComplete = getCompletedOvers(match.getInnings2Overs()) >= maxOvers;
            boolean chased = match.getInnings2Runs() >= match.getTarget();

            if (allOut || oversComplete || chased) {
                completeMatch(match);
            }
        }

        Match saved = matchRepository.save(match);
        broadcastUpdate(saved);
        return toResponse(saved);
    }

    // ================================================================
    // START INNINGS 2
    // ================================================================
    @Transactional
    public MatchDto.Response startInnings2(Long matchId) {
        Match match = findById(matchId);
        if (match.getStatus() != Match.MatchStatus.INNINGS_BREAK) {
            throw new BadRequestException("Match is not in innings break");
        }
        match.setStatus(Match.MatchStatus.LIVE);
        Match saved = matchRepository.save(match);
        broadcastUpdate(saved);
        return toResponse(saved);
    }

    // ================================================================
    // COMPLETE MATCH MANUALLY
    // ================================================================
    @Transactional
    public MatchDto.Response declareResult(Long matchId, MatchDto.MatchResultRequest request) {
        Match match = findById(matchId);

        if (request.getWinnerId() != null) {
            Team winner = teamRepository.findById(request.getWinnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", request.getWinnerId()));
            match.setWinner(winner);
        }

        match.setResultDescription(request.getResultDescription());
        match.setStatus(Match.MatchStatus.COMPLETED);

        // Update leaderboard
        leaderboardService.updateAfterMatch(match);

        Match saved = matchRepository.save(match);
        broadcastUpdate(saved);
        log.info("Match {} completed. Winner: {}", matchId,
                match.getWinner() != null ? match.getWinner().getName() : "No result");
        return toResponse(saved);
    }

    // ================================================================
    // QUERIES
    // ================================================================
    @Transactional(readOnly = true)
    public List<MatchDto.Response> getByTournament(Long tournamentId) {
        return matchRepository.findByTournamentIdOrderByMatchNumberAsc(tournamentId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MatchDto.LiveMatchResponse getLiveMatch(Long matchId) {
        Match match = findById(matchId);
        List<BallEvent> recentBalls = ballEventRepository
                .findByMatchIdOrderByCreatedAtDesc(matchId)
                .stream().limit(6).collect(Collectors.toList());

        int requiredRuns = 0;
        double requiredRunRate = 0.0;
        int ballsRemaining = 0;

        if (match.getCurrentInnings() == 2 && match.getTarget() != null) {
            requiredRuns = match.getTarget() - match.getInnings2Runs();
            int maxBalls = match.getTournament().getOversPerInnings() * 6;
            int ballsFaced = (int)(getCompletedOvers(match.getInnings2Overs()) * 6
                    + (match.getInnings2Overs() % 1.0 * 10));
            ballsRemaining = Math.max(0, maxBalls - ballsFaced);
            requiredRunRate = ballsRemaining > 0
                    ? Math.round((requiredRuns / (ballsRemaining / 6.0)) * 100.0) / 100.0 : 0.0;
        }

        return MatchDto.LiveMatchResponse.builder()
                .match(toResponse(match))
                .recentBalls(recentBalls.stream().map(this::toBallResponse).collect(Collectors.toList()))
                .requiredRuns(requiredRuns)
                .requiredRunRate(requiredRunRate)
                .ballsRemaining(ballsRemaining)
                .build();
    }

    @Transactional(readOnly = true)
    public MatchDto.Response getByPublicCode(String code) {
        Match match = matchRepository.findByPublicCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with code: " + code));
        return toResponse(match);
    }

    @Transactional(readOnly = true)
    public List<MatchDto.Response> getLiveMatches() {
        return matchRepository.findByStatusOrderByMatchDateAsc(Match.MatchStatus.LIVE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ================================================================
    // HELPERS
    // ================================================================
    private void completeMatch(Match match) {
        match.setStatus(Match.MatchStatus.COMPLETED);

        Team battingSecond = match.getCurrentBattingTeam();
        Team battingFirst = match.getBattingFirstTeam();

        if (match.getInnings2Runs() >= match.getTarget()) {
            // Chased successfully — batting second wins
            match.setWinner(battingSecond);
            int wicketsRemaining = 10 - match.getInnings2Wickets();
            match.setResultDescription(battingSecond.getName() + " won by "
                    + wicketsRemaining + " wicket(s)");
        } else if (match.getInnings2Runs() < match.getTarget() - 1) {
            // Batting first wins
            match.setWinner(battingFirst);
            int margin = (match.getTarget() - 1) - match.getInnings2Runs();
            match.setResultDescription(battingFirst.getName() + " won by " + margin + " run(s)");
        } else {
            // Tie
            match.setResultDescription("Match Tied!");
        }

        leaderboardService.updateAfterMatch(match);
        log.info("Match auto-completed: {}", match.getResultDescription());
    }

    private double incrementOvers(double currentOvers) {
        int completedOvers = (int) currentOvers;
        int balls = (int) Math.round((currentOvers - completedOvers) * 10);
        balls++;
        if (balls >= 6) {
            return completedOvers + 1.0;
        }
        return completedOvers + (balls / 10.0);
    }

    private int[] calculateOverBall(double overs) {
        int completedOvers = (int) overs;
        int balls = (int) Math.round((overs - completedOvers) * 10);
        return new int[]{completedOvers, balls};
    }

    private int getCompletedOvers(double overs) {
        return (int) overs;
    }

    private void broadcastUpdate(Match match) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/match/" + match.getId(), toResponse(match));
        } catch (Exception e) {
            log.warn("WebSocket broadcast failed for match {}: {}", match.getId(), e.getMessage());
        }
    }

    public Match findById(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match", id));
    }

    public MatchDto.Response toResponse(Match m) {
        String shareLink = "/match/" + m.getPublicCode();
        return MatchDto.Response.builder()
                .id(m.getId())
                .publicCode(m.getPublicCode())
                .tournamentId(m.getTournament().getId())
                .tournamentName(m.getTournament().getName())
                .oversPerInnings(m.getTournament().getOversPerInnings())
                .teamAId(m.getTeamA().getId())
                .teamAName(m.getTeamA().getName())
                .teamAColor(m.getTeamA().getColorCode())
                .teamBId(m.getTeamB().getId())
                .teamBName(m.getTeamB().getName())
                .teamBColor(m.getTeamB().getColorCode())
                .matchDate(m.getMatchDate())
                .matchTime(m.getMatchTime())
                .venue(m.getVenue())
                .roundNumber(m.getRoundNumber())
                .matchNumber(m.getMatchNumber())
                .status(m.getStatus())
                .tossWinnerId(m.getTossWinner() != null ? m.getTossWinner().getId() : null)
                .tossWinnerName(m.getTossWinner() != null ? m.getTossWinner().getName() : null)
                .tossDecision(m.getTossDecision())
                .battingFirstTeamId(m.getBattingFirstTeam() != null ? m.getBattingFirstTeam().getId() : null)
                .battingFirstTeamName(m.getBattingFirstTeam() != null ? m.getBattingFirstTeam().getName() : null)
                .innings1Runs(m.getInnings1Runs())
                .innings1Wickets(m.getInnings1Wickets())
                .innings1Overs(m.getInnings1Overs())
                .innings1RunRate(m.getInnings1RunRate())
                .innings2Runs(m.getInnings2Runs())
                .innings2Wickets(m.getInnings2Wickets())
                .innings2Overs(m.getInnings2Overs())
                .innings2RunRate(m.getInnings2RunRate())
                .currentInnings(m.getCurrentInnings())
                .currentBattingTeamId(m.getCurrentBattingTeam() != null ? m.getCurrentBattingTeam().getId() : null)
                .currentBattingTeamName(m.getCurrentBattingTeam() != null ? m.getCurrentBattingTeam().getName() : null)
                .target(m.getTarget())
                .winnerId(m.getWinner() != null ? m.getWinner().getId() : null)
                .winnerName(m.getWinner() != null ? m.getWinner().getName() : null)
                .resultDescription(m.getResultDescription())
                .shareLink(shareLink)
                .createdAt(m.getCreatedAt())
                .build();
    }

    private MatchDto.BallEventResponse toBallResponse(BallEvent b) {
        return MatchDto.BallEventResponse.builder()
                .id(b.getId())
                .inningsNumber(b.getInningsNumber())
                .overNumber(b.getOverNumber())
                .ballNumber(b.getBallNumber())
                .batsmanName(b.getBatsman() != null ? b.getBatsman().getName() : null)
                .bowlerName(b.getBowler() != null ? b.getBowler().getName() : null)
                .runsScored(b.getRunsScored())
                .ballType(b.getBallType())
                .isWicket(b.isWicket())
                .wicketType(b.getWicketType())
                .extraRuns(b.getExtraRuns())
                .commentary(b.getCommentary())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
