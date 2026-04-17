package com.tournament.repository;

import com.tournament.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByTournamentIdOrderByMatchNumberAsc(Long tournamentId);

    List<Match> findByTournamentIdAndStatusOrderByMatchDateAsc(Long tournamentId, Match.MatchStatus status);

    Optional<Match> findByPublicCode(String publicCode);

    @Query("SELECT m FROM Match m WHERE m.tournament.id = :tid AND (m.teamA.id = :teamId OR m.teamB.id = :teamId)")
    List<Match> findByTournamentAndTeam(@Param("tid") Long tournamentId, @Param("teamId") Long teamId);

    @Query("SELECT COUNT(m) FROM Match m WHERE m.tournament.id = :tid")
    long countByTournamentId(@Param("tid") Long tournamentId);

    List<Match> findByStatusOrderByMatchDateAsc(Match.MatchStatus status);

    @Query("SELECT m FROM Match m WHERE m.tournament.id = :tid AND m.teamA.id = :teamAId AND m.teamB.id = :teamBId")
    Optional<Match> findByTournamentAndTeams(@Param("tid") Long tournamentId,
                                             @Param("teamAId") Long teamAId,
                                             @Param("teamBId") Long teamBId);
}
