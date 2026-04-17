package com.tournament.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Stores each ball event during a match.
 * Enables ball-by-ball commentary and replay.
 */
@Entity
@Table(name = "ball_events", indexes = {
    @Index(name = "idx_ball_event_match", columnList = "match_id"),
    @Index(name = "idx_ball_event_innings", columnList = "match_id, innings_number")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BallEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    private Integer inningsNumber;  // 1 or 2
    private Integer overNumber;     // 0-indexed
    private Integer ballNumber;     // 1-6

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batsman_id")
    private Player batsman;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bowler_id")
    private Player bowler;

    private Integer runsScored = 0;

    @Enumerated(EnumType.STRING)
    private BallType ballType = BallType.NORMAL;

    private boolean isWicket = false;

    @Enumerated(EnumType.STRING)
    private WicketType wicketType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fielder_id")
    private Player fielder;  // for catches/run-outs

    /** Extra runs (wides, no-balls, byes) */
    private Integer extraRuns = 0;

    /** Commentary for this ball */
    private String commentary;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum BallType {
        NORMAL, WIDE, NO_BALL, BYE, LEG_BYE
    }

    public enum WicketType {
        BOWLED, CAUGHT, LBW, RUN_OUT, STUMPED, HIT_WICKET, RETIRED_HURT
    }
}
