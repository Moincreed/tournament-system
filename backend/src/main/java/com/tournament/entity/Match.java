package com.tournament.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "matches", indexes = {
    @Index(name = "idx_match_tournament", columnList = "tournament_id"),
    @Index(name = "idx_match_status", columnList = "status"),
    @Index(name = "idx_match_public_code", columnList = "publicCode")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique public-shareable code (UUID short) */
    @Column(unique = true, nullable = false, updatable = false)
    private String publicCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_a_id", nullable = false)
    private Team teamA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_b_id", nullable = false)
    private Team teamB;

    @Column(nullable = false)
    private LocalDate matchDate;

    private LocalTime matchTime;

    private String venue;

    /** Round number (1, 2, etc.) */
    private Integer roundNumber;

    /** Match number within tournament */
    private Integer matchNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.UPCOMING;

    /** Who won the toss */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toss_winner_id")
    private Team tossWinner;

    /** Toss decision */
    @Enumerated(EnumType.STRING)
    private TossDecision tossDecision;

    /** Who is batting first */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batting_first_team_id")
    private Team battingFirstTeam;

    // --- Innings 1 (Team batting first) ---
    private Integer innings1Runs = 0;
    private Integer innings1Wickets = 0;
    private Double innings1Overs = 0.0;
    private Double innings1RunRate = 0.0;

    // --- Innings 2 ---
    private Integer innings2Runs = 0;
    private Integer innings2Wickets = 0;
    private Double innings2Overs = 0.0;
    private Double innings2RunRate = 0.0;

    /** Which innings is currently live (1 or 2) */
    private Integer currentInnings = 1;

    /** Which team is currently batting */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_batting_team_id")
    private Team currentBattingTeam;

    /** Target for 2nd innings */
    private Integer target;

    /** Match winner */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Team winner;

    /** Result description e.g. "Team A won by 45 runs" */
    @Column(columnDefinition = "TEXT")
    private String resultDescription;

    /** Notes / commentary */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Ball-by-ball records */
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BallEvent> ballEvents;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    public void generatePublicCode() {
        if (this.publicCode == null) {
            this.publicCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    public enum MatchStatus {
        UPCOMING, LIVE, INNINGS_BREAK, COMPLETED, ABANDONED
    }

    public enum TossDecision {
        BAT, BOWL
    }
}
