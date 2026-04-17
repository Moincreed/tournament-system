package com.tournament.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "players", indexes = {
    @Index(name = "idx_player_team", columnList = "team_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    /** Jersey number */
    private Integer jerseyNumber;

    @Enumerated(EnumType.STRING)
    private PlayerRole role = PlayerRole.ALL_ROUNDER;

    /** Batting style */
    @Enumerated(EnumType.STRING)
    private BattingStyle battingStyle = BattingStyle.RIGHT_HAND;

    /** Bowling style */
    @Enumerated(EnumType.STRING)
    private BowlingStyle bowlingStyle;

    private String phone;
    private Integer age;

    // --- Career Stats ---
    private Integer matchesPlayed = 0;
    private Integer totalRuns = 0;
    private Integer totalWickets = 0;
    private Integer highestScore = 0;
    private Integer bestBowling = 0;  // wickets in best bowling spell
    private Integer catches = 0;
    private Integer halfCenturies = 0;
    private Integer centuries = 0;
    private Integer fiveWickets = 0;  // five-wicket hauls

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum PlayerRole {
        BATSMAN, BOWLER, ALL_ROUNDER, WICKET_KEEPER
    }

    public enum BattingStyle {
        RIGHT_HAND, LEFT_HAND
    }

    public enum BowlingStyle {
        RIGHT_ARM_FAST, RIGHT_ARM_MEDIUM, RIGHT_ARM_SPIN,
        LEFT_ARM_FAST, LEFT_ARM_MEDIUM, LEFT_ARM_SPIN
    }
}
