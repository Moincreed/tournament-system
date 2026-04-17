package com.tournament.repository;

import com.tournament.entity.Tournament;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    Page<Tournament> findByOrganizerIdOrderByCreatedAtDesc(Long organizerId, Pageable pageable);

    Page<Tournament> findByStatusOrderByStartDateDesc(Tournament.TournamentStatus status, Pageable pageable);

    @Query("SELECT t FROM Tournament t WHERE t.name LIKE %:keyword% OR t.location LIKE %:keyword%")
    List<Tournament> searchByKeyword(@Param("keyword") String keyword);

    List<Tournament> findTop10ByOrderByStartDateDesc();
}
