package com.tournament.repository;

import com.tournament.entity.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaderboardRepository extends JpaRepository<Leaderboard, Long> {

    List<Leaderboard> findByTournamentIdOrderByPointsDescNetRunRateDesc(Long tournamentId);

    Optional<Leaderboard> findByTournamentIdAndTeamId(Long tournamentId, Long teamId);

    void deleteByTournamentId(Long tournamentId);
}
