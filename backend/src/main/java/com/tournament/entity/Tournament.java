package com.tournament.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tournaments", indexes = {
    @Index(name = "idx_tournament_organizer", columnList = "organizer_id"),
    @Index(name = "idx_tournament_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SportType sportType = SportType.CRICKET;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentStatus status = TournamentStatus.UPCOMING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentFormat format = TournamentFormat.ROUND_ROBIN;

    /** Number of overs per innings (for cricket) */
    private Integer oversPerInnings = 20;

    /** Max teams allowed */
    private Integer maxTeams = 8;

    // --- Relationships ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Team> teams;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Match> matches;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum SportType {
        CRICKET, FOOTBALL, VOLLEYBALL, KABADDI, BADMINTON, OTHER
    }

    public enum TournamentStatus {
        UPCOMING, ONGOING, COMPLETED, CANCELLED
    }

    public enum TournamentFormat {
        ROUND_ROBIN, KNOCKOUT, LEAGUE_CUM_KNOCKOUT
    }
}
