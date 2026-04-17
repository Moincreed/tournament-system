package com.tournament.repository;

import com.tournament.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByTournamentId(Long tournamentId);
    boolean existsByNameAndTournamentId(String name, Long tournamentId);
    long countByTournamentId(Long tournamentId);
}
