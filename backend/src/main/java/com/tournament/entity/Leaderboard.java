package com.tournament.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Leaderboard / Points Table entry per team per tournament.
 * Updated automatically after each match completion.
 */
@Entity
@Table(name = "leaderboard",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "team_id"}),
    indexes = {
        @Index(name = "idx_leaderboard_tournament", columnList = "tournament_id"),
        @Index(name = "idx_leaderboard_points", columnList = "tournament_id, points DESC")
    })
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Leaderboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    private Integer matchesPlayed = 0;
    private Integer wins = 0;
    private Integer losses = 0;
    private Integer draws = 0;
    private Integer noResults = 0;

    /** Points: Win = 2, Draw = 1, Loss = 0 */
    private Integer points = 0;

    /** Net Run Rate = (runs scored / overs faced) - (runs conceded / overs bowled) */
    private Double netRunRate = 0.0;

    /** Total runs scored across all matches */
    private Integer totalRunsScored = 0;

    /** Total overs batted */
    private Double totalOversBatted = 0.0;

    /** Total runs conceded */
    private Integer totalRunsConceded = 0;

    /** Total overs bowled */
    private Double totalOversBowled = 0.0;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Recalculate NRR = (runs_scored/overs_batted) - (runs_conceded/overs_bowled)
     */
    public void recalculateNRR() {
        double runRateFor = totalOversBatted > 0 ? (double) totalRunsScored / totalOversBatted : 0.0;
        double runRateAgainst = totalOversBowled > 0 ? (double) totalRunsConceded / totalOversBowled : 0.0;
        this.netRunRate = Math.round((runRateFor - runRateAgainst) * 1000.0) / 1000.0;
    }
}
