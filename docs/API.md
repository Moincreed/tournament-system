# Local Tournament Management System — API Documentation

Base URL: `http://localhost:8080/api`

All protected endpoints require:
```
Authorization: Bearer <jwt_token>
```

All responses follow the envelope:
```json
{
  "success": true,
  "message": "...",
  "data": { ... },
  "timestamp": "2024-04-17T10:30:00"
}
```

---

## AUTH

### POST /auth/register
Register a new organizer account.
```json
{
  "name": "Ramesh Sharma",
  "email": "ramesh@example.com",
  "password": "Test@123",
  "phone": "9876543210"
}
```
**Response:** JWT token + user info

### POST /auth/login
```json
{ "email": "ramesh@example.com", "password": "Test@123" }
```
**Response:** JWT token + user info

---

## TOURNAMENTS

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /tournaments | ✅ | Create tournament |
| GET | /tournaments | ❌ | List all (paginated) |
| GET | /tournaments/recent | ❌ | Last 10 tournaments |
| GET | /tournaments/my | ✅ | Organizer's own |
| GET | /tournaments/{id} | ❌ | Get by ID |
| PUT | /tournaments/{id} | ✅ | Update |
| PATCH | /tournaments/{id}/status?status=ONGOING | ✅ | Change status |
| DELETE | /tournaments/{id} | ✅ | Delete |

**Create Request:**
```json
{
  "name": "Gaon Cup 2024",
  "location": "Rampur Village Ground",
  "description": "Annual village cricket tournament",
  "startDate": "2024-04-01",
  "endDate": "2024-04-30",
  "sportType": "CRICKET",
  "format": "ROUND_ROBIN",
  "oversPerInnings": 20,
  "maxTeams": 8
}
```

---

## TEAMS

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /teams | ✅ | Add team to tournament |
| GET | /teams/tournament/{id} | ❌ | Teams in tournament |
| GET | /teams/{id} | ❌ | Get team |
| PUT | /teams/{id} | ✅ | Update team |
| DELETE | /teams/{id} | ✅ | Remove team |

**Create Request:**
```json
{
  "name": "Rampur XI",
  "tournamentId": 1,
  "captainName": "Mahesh Kumar",
  "homeGround": "Rampur Ground",
  "colorCode": "#1E90FF"
}
```

---

## PLAYERS

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /players | ✅ | Add player to team |
| GET | /players/team/{teamId} | ❌ | Players in team |
| GET | /players/{id} | ❌ | Get player |
| PUT | /players/{id} | ✅ | Update player |
| DELETE | /players/{id} | ✅ | Remove player |
| GET | /players/tournament/{id}/top-batsmen | ❌ | Top 10 batsmen |
| GET | /players/tournament/{id}/top-bowlers | ❌ | Top 10 bowlers |

**Create Request:**
```json
{
  "name": "Mahesh Kumar",
  "teamId": 1,
  "jerseyNumber": 7,
  "role": "BATSMAN",
  "battingStyle": "RIGHT_HAND",
  "bowlingStyle": "RIGHT_ARM_MEDIUM",
  "age": 28,
  "phone": "9876543210"
}
```
**Roles:** BATSMAN · BOWLER · ALL_ROUNDER · WICKET_KEEPER

---

## MATCHES & SCORING

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /matches/generate/{tournamentId} | ✅ | Auto-generate round-robin fixtures |
| GET | /matches/tournament/{id} | ❌ | All matches for tournament |
| GET | /matches/live | ❌ | Currently live matches |
| GET | /matches/{id} | ❌ | Match details |
| GET | /matches/{id}/live | ❌ | Live match + recent balls + chase stats |
| GET | /matches/public/{code} | ❌ | Match by shareable code |
| PATCH | /matches/{id}/toss | ✅ | Record toss, start match |
| POST | /matches/{id}/score | ✅ | Record ball event |
| PATCH | /matches/{id}/innings2/start | ✅ | Start 2nd innings |
| PATCH | /matches/{id}/result | ✅ | Declare result |

**Toss Request:**
```json
{ "tossWinnerId": 1, "decision": "BAT" }
```
`decision`: BAT or BOWL

**Score Update (Ball Event):**
```json
{
  "runsScored": 4,
  "ballType": "NORMAL",
  "isWicket": false,
  "batsmanId": 5,
  "bowlerId": 12,
  "extraRuns": 0,
  "commentary": "Mahesh drives through covers for four!"
}
```
**ballType:** NORMAL · WIDE · NO_BALL · BYE · LEG_BYE

**Wicket Example:**
```json
{
  "runsScored": 0,
  "ballType": "NORMAL",
  "isWicket": true,
  "wicketType": "CAUGHT",
  "batsmanId": 5,
  "bowlerId": 12,
  "fielderId": 23,
  "commentary": "Caught at mid-off!"
}
```
**wicketType:** BOWLED · CAUGHT · LBW · RUN_OUT · STUMPED · HIT_WICKET · RETIRED_HURT

**Declare Result:**
```json
{
  "winnerId": 1,
  "resultDescription": "Rampur XI won by 34 runs"
}
```

---

## LEADERBOARD

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /leaderboard/{tournamentId} | ❌ | Full points table + top players |

**Response includes:**
- Standings: rank, team, P/W/L/D, points, NRR
- Top 5 batsmen
- Top 5 bowlers

---

## PUBLIC SHAREABLE LINKS

Each match gets a unique 8-character code:
```
GET /matches/public/A3F7B2
```
Share this URL for live score viewing without login.

WebSocket subscription for live updates:
```javascript
const socket = new SockJS('http://localhost:8080/api/ws');
const client = Stomp.over(socket);
client.connect({}, () => {
  client.subscribe('/topic/match/42', (msg) => {
    const matchData = JSON.parse(msg.body);
    // Update your UI
  });
});
```

---

## SWAGGER UI

Available at: `http://localhost:8080/api/swagger-ui.html`
API JSON: `http://localhost:8080/api/api-docs`

---

## ERROR CODES

| HTTP | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad request / Validation error |
| 401 | Unauthorized (missing/invalid token) |
| 403 | Forbidden (wrong role) |
| 404 | Resource not found |
| 409 | Conflict (duplicate) |
| 500 | Server error |
