-- ============================================================
-- LOCAL TOURNAMENT MANAGEMENT SYSTEM — MySQL Schema
-- ============================================================
-- Run this ONCE to set up the database from scratch.
-- Spring Boot will manage table updates via JPA (ddl-auto=update).
-- This file is useful for fresh installs or documentation.
-- ============================================================

CREATE DATABASE IF NOT EXISTS tournament_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE tournament_db;

-- ============================================================
-- 1. USERS (Organizers & Admins)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    phone       VARCHAR(15),
    role        ENUM('ADMIN','ORGANIZER') NOT NULL DEFAULT 'ORGANIZER',
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 2. TOURNAMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS tournaments (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(200) NOT NULL,
    location            VARCHAR(200) NOT NULL,
    description         TEXT,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    sport_type          ENUM('CRICKET','FOOTBALL','VOLLEYBALL','KABADDI','BADMINTON','OTHER') DEFAULT 'CRICKET',
    status              ENUM('UPCOMING','ONGOING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'UPCOMING',
    format              ENUM('ROUND_ROBIN','KNOCKOUT','LEAGUE_CUM_KNOCKOUT') NOT NULL DEFAULT 'ROUND_ROBIN',
    overs_per_innings   INT DEFAULT 20,
    max_teams           INT DEFAULT 8,
    organizer_id        BIGINT NOT NULL,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (organizer_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_tournament_organizer (organizer_id),
    INDEX idx_tournament_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 3. TEAMS
-- ============================================================
CREATE TABLE IF NOT EXISTS teams (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    logo_url        VARCHAR(500),
    captain_name    VARCHAR(100),
    home_ground     VARCHAR(200),
    color_code      VARCHAR(10),
    tournament_id   BIGINT NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (tournament_id) REFERENCES tournaments(id) ON DELETE CASCADE,
    INDEX idx_team_tournament (tournament_id),
    UNIQUE KEY uk_team_name_tournament (name, tournament_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 4. PLAYERS
-- ============================================================
CREATE TABLE IF NOT EXISTS players (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    jersey_number   INT,
    role            ENUM('BATSMAN','BOWLER','ALL_ROUNDER','WICKET_KEEPER') DEFAULT 'ALL_ROUNDER',
    batting_style   ENUM('RIGHT_HAND','LEFT_HAND') DEFAULT 'RIGHT_HAND',
    bowling_style   ENUM('RIGHT_ARM_FAST','RIGHT_ARM_MEDIUM','RIGHT_ARM_SPIN',
                         'LEFT_ARM_FAST','LEFT_ARM_MEDIUM','LEFT_ARM_SPIN'),
    phone           VARCHAR(15),
    age             INT,
    team_id         BIGINT NOT NULL,
    -- Career stats (updated after each match)
    matches_played  INT DEFAULT 0,
    total_runs      INT DEFAULT 0,
    total_wickets   INT DEFAULT 0,
    highest_score   INT DEFAULT 0,
    best_bowling    INT DEFAULT 0,
    catches         INT DEFAULT 0,
    half_centuries  INT DEFAULT 0,
    centuries       INT DEFAULT 0,
    five_wickets    INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    INDEX idx_player_team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 5. MATCHES
-- ============================================================
CREATE TABLE IF NOT EXISTS matches (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_code             VARCHAR(20) NOT NULL UNIQUE,
    tournament_id           BIGINT NOT NULL,
    team_a_id               BIGINT NOT NULL,
    team_b_id               BIGINT NOT NULL,
    match_date              DATE NOT NULL,
    match_time              TIME,
    venue                   VARCHAR(200),
    round_number            INT,
    match_number            INT,
    status                  ENUM('UPCOMING','LIVE','INNINGS_BREAK','COMPLETED','ABANDONED') DEFAULT 'UPCOMING',

    -- Toss
    toss_winner_id          BIGINT,
    toss_decision           ENUM('BAT','BOWL'),
    batting_first_team_id   BIGINT,

    -- Innings 1
    innings1_runs           INT DEFAULT 0,
    innings1_wickets        INT DEFAULT 0,
    innings1_overs          DOUBLE DEFAULT 0,
    innings1_run_rate       DOUBLE DEFAULT 0,

    -- Innings 2
    innings2_runs           INT DEFAULT 0,
    innings2_wickets        INT DEFAULT 0,
    innings2_overs          DOUBLE DEFAULT 0,
    innings2_run_rate       DOUBLE DEFAULT 0,

    current_innings         INT DEFAULT 1,
    current_batting_team_id BIGINT,
    target                  INT,

    winner_id               BIGINT,
    result_description      TEXT,
    notes                   TEXT,

    created_at              DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (tournament_id)           REFERENCES tournaments(id) ON DELETE CASCADE,
    FOREIGN KEY (team_a_id)               REFERENCES teams(id),
    FOREIGN KEY (team_b_id)               REFERENCES teams(id),
    FOREIGN KEY (toss_winner_id)          REFERENCES teams(id),
    FOREIGN KEY (batting_first_team_id)   REFERENCES teams(id),
    FOREIGN KEY (current_batting_team_id) REFERENCES teams(id),
    FOREIGN KEY (winner_id)               REFERENCES teams(id),

    INDEX idx_match_tournament (tournament_id),
    INDEX idx_match_status    (status),
    INDEX idx_match_public    (public_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 6. BALL EVENTS (Ball-by-ball scoring)
-- ============================================================
CREATE TABLE IF NOT EXISTS ball_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_id        BIGINT NOT NULL,
    innings_number  INT NOT NULL,
    over_number     INT NOT NULL,
    ball_number     INT NOT NULL,
    batsman_id      BIGINT,
    bowler_id       BIGINT,
    runs_scored     INT DEFAULT 0,
    ball_type       ENUM('NORMAL','WIDE','NO_BALL','BYE','LEG_BYE') DEFAULT 'NORMAL',
    is_wicket       BOOLEAN DEFAULT FALSE,
    wicket_type     ENUM('BOWLED','CAUGHT','LBW','RUN_OUT','STUMPED','HIT_WICKET','RETIRED_HURT'),
    fielder_id      BIGINT,
    extra_runs      INT DEFAULT 0,
    commentary      VARCHAR(500),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (match_id)    REFERENCES matches(id) ON DELETE CASCADE,
    FOREIGN KEY (batsman_id)  REFERENCES players(id),
    FOREIGN KEY (bowler_id)   REFERENCES players(id),
    FOREIGN KEY (fielder_id)  REFERENCES players(id),

    INDEX idx_ball_match   (match_id),
    INDEX idx_ball_innings (match_id, innings_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 7. LEADERBOARD (Points Table)
-- ============================================================
CREATE TABLE IF NOT EXISTS leaderboard (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_id       BIGINT NOT NULL,
    team_id             BIGINT NOT NULL,
    matches_played      INT DEFAULT 0,
    wins                INT DEFAULT 0,
    losses              INT DEFAULT 0,
    draws               INT DEFAULT 0,
    no_results          INT DEFAULT 0,
    points              INT DEFAULT 0,
    net_run_rate        DOUBLE DEFAULT 0.0,
    total_runs_scored   INT DEFAULT 0,
    total_overs_batted  DOUBLE DEFAULT 0.0,
    total_runs_conceded INT DEFAULT 0,
    total_overs_bowled  DOUBLE DEFAULT 0.0,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (tournament_id) REFERENCES tournaments(id) ON DELETE CASCADE,
    FOREIGN KEY (team_id)       REFERENCES teams(id) ON DELETE CASCADE,
    UNIQUE KEY uk_leaderboard (tournament_id, team_id),
    INDEX idx_leaderboard_points (tournament_id, points)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- SEED DATA — Sample tournament, teams, players
-- ============================================================

-- Admin user (password: Admin@123)
INSERT IGNORE INTO users (name, email, password, role) VALUES
('System Admin', 'admin@tournament.local',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X6ZxvxMtFmxFjW1IG', 'ADMIN');

-- Sample organizer (password: Test@123)
INSERT IGNORE INTO users (name, email, password, phone, role) VALUES
('Ramesh Sharma', 'ramesh@example.com',
 '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGdbQ9k3HfRVSBGRla', '9876543210', 'ORGANIZER');

-- Sample tournament
INSERT IGNORE INTO tournaments (name, location, description, start_date, end_date, sport_type, status, format, overs_per_innings, max_teams, organizer_id)
SELECT 'Gaon Cup 2024', 'Rampur Village Ground', 'Annual village cricket tournament', 
       '2024-04-01', '2024-04-30', 'CRICKET', 'ONGOING', 'ROUND_ROBIN', 20, 8, id
FROM users WHERE email = 'ramesh@example.com' LIMIT 1;
