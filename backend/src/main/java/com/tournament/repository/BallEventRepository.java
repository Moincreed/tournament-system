package com.tournament.repository;

import com.tournament.entity.BallEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BallEventRepository extends JpaRepository<BallEvent, Long> {
    List<BallEvent> findByMatchIdAndInningsNumberOrderByOverNumberAscBallNumberAsc(
            Long matchId, Integer inningsNumber);
    List<BallEvent> findByMatchIdOrderByCreatedAtDesc(Long matchId);
    void deleteByMatchId(Long matchId);
}
