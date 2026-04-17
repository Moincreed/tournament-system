package com.tournament.service;

import com.tournament.dto.TournamentDto;
import com.tournament.entity.Team;
import com.tournament.entity.Tournament;
import com.tournament.entity.User;
import com.tournament.exception.BadRequestException;
import com.tournament.exception.ResourceNotFoundException;
import com.tournament.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final LeaderboardRepository leaderboardRepository;

    @Transactional
    public TournamentDto.Response create(TournamentDto.CreateRequest request, String organizerEmail) {
        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        Tournament tournament = Tournament.builder()
                .name(request.getName())
                .location(request.getLocation())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .sportType(request.getSportType())
                .format(request.getFormat())
                .oversPerInnings(request.getOversPerInnings())
                .maxTeams(request.getMaxTeams())
                .status(Tournament.TournamentStatus.UPCOMING)
                .organizer(organizer)
                .build();

        Tournament saved = tournamentRepository.save(tournament);
        log.info("Tournament created: {} by {}", saved.getName(), organizerEmail);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TournamentDto.Response> getAll(Pageable pageable) {
        return tournamentRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TournamentDto.Response> getByOrganizer(String email, Pageable pageable) {
        User organizer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return tournamentRepository.findByOrganizerIdOrderByCreatedAtDesc(organizer.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TournamentDto.Response getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public TournamentDto.Response update(Long id, TournamentDto.CreateRequest request, String email) {
        Tournament tournament = findById(id);
        validateOwner(tournament, email);

        tournament.setName(request.getName());
        tournament.setLocation(request.getLocation());
        tournament.setDescription(request.getDescription());
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());
        tournament.setOversPerInnings(request.getOversPerInnings());
        tournament.setMaxTeams(request.getMaxTeams());

        return toResponse(tournamentRepository.save(tournament));
    }

    @Transactional
    public void updateStatus(Long id, Tournament.TournamentStatus status, String email) {
        Tournament tournament = findById(id);
        validateOwner(tournament, email);
        tournament.setStatus(status);
        tournamentRepository.save(tournament);
        log.info("Tournament {} status changed to {}", id, status);
    }

    @Transactional
    public void delete(Long id, String email) {
        Tournament tournament = findById(id);
        validateOwner(tournament, email);

        if (tournament.getStatus() == Tournament.TournamentStatus.ONGOING) {
            throw new BadRequestException("Cannot delete an ongoing tournament");
        }

        tournamentRepository.delete(tournament);
        log.info("Tournament deleted: {}", id);
    }

    @Transactional(readOnly = true)
    public List<TournamentDto.Response> getRecent() {
        return tournamentRepository.findTop10ByOrderByStartDateDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // --- Helper Methods ---

    public Tournament findById(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", id));
    }

    private void validateOwner(Tournament tournament, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Admin can manage any tournament
        if (user.getRole() == User.Role.ADMIN) return;
        if (!tournament.getOrganizer().getEmail().equals(email)) {
            throw new BadRequestException("You are not the organizer of this tournament");
        }
    }

    public TournamentDto.Response toResponse(Tournament t) {
        long teamsCount = teamRepository.countByTournamentId(t.getId());
        long matchesCount = matchRepository.countByTournamentId(t.getId());

        return TournamentDto.Response.builder()
                .id(t.getId())
                .name(t.getName())
                .location(t.getLocation())
                .description(t.getDescription())
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .sportType(t.getSportType())
                .status(t.getStatus())
                .format(t.getFormat())
                .oversPerInnings(t.getOversPerInnings())
                .maxTeams(t.getMaxTeams())
                .teamsRegistered((int) teamsCount)
                .matchesPlayed((int) matchesCount)
                .organizerName(t.getOrganizer().getName())
                .organizerId(t.getOrganizer().getId())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
