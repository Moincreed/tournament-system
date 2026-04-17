# 🏏 Local Tournament Management System

A production-ready tournament management platform for local cricket and sports tournaments.
Replaces manual/WhatsApp-based management with a structured web system.

---

## 🗂 Project Structure

```
tournament-system/
├── backend/                          ← Spring Boot API
│   ├── pom.xml
│   └── src/main/java/com/tournament/
│       ├── TournamentApplication.java
│       ├── config/
│       │   ├── SecurityConfig.java   ← JWT + CORS + Role security
│       │   ├── WebSocketConfig.java  ← Live score WebSocket
│       │   └── SwaggerConfig.java    ← API docs
│       ├── controller/
│       │   ├── AuthController.java
│       │   ├── TournamentController.java
│       │   ├── TeamController.java
│       │   ├── PlayerController.java
│       │   ├── MatchController.java
│       │   └── LeaderboardController.java
│       ├── service/
│       │   ├── AuthService.java
│       │   ├── TournamentService.java
│       │   ├── TeamService.java
│       │   ├── PlayerService.java
│       │   ├── MatchService.java     ← Fixture gen + Live scoring
│       │   └── LeaderboardService.java ← Auto NRR calculation
│       ├── entity/
│       │   ├── User.java
│       │   ├── Tournament.java
│       │   ├── Team.java
│       │   ├── Player.java
│       │   ├── Match.java
│       │   ├── BallEvent.java        ← Ball-by-ball data
│       │   └── Leaderboard.java
│       ├── repository/               ← JPA Repositories
│       ├── dto/                      ← Request/Response DTOs
│       ├── security/                 ← JWT Filter + UserDetailsService
│       └── exception/                ← Global error handler
├── docs/
│   ├── schema.sql                    ← Full MySQL schema + seed data
│   ├── API.md                        ← Complete API documentation
│   └── README.md                     ← This file
└── frontend/
    └── (React/HTML frontend - see frontend README)
```

---

## ⚙️ Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |

---

## 🚀 Step-by-Step Setup

### Step 1: Create MySQL Database
```sql
CREATE DATABASE tournament_db CHARACTER SET utf8mb4;
```
Or run the full schema:
```bash
mysql -u root -p < docs/schema.sql
```

### Step 2: Configure application.properties
Edit `backend/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tournament_db?...
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### Step 3: Build & Run
```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```

The server starts at: **http://localhost:8080**

### Step 4: Verify
- API Base: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui.html
- Health: curl http://localhost:8080/api/tournaments

---

## 🔑 Default Credentials (from seed data)

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@tournament.local | Admin@123 |
| Organizer | ramesh@example.com | Test@123 |

---

## 📋 Quick API Walkthrough

### 1. Register & Login
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Ramesh","email":"r@x.com","password":"Test@123"}'

# Login → copy the token from response
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"r@x.com","password":"Test@123"}'

export TOKEN="<paste_token_here>"
```

### 2. Create Tournament
```bash
curl -X POST http://localhost:8080/api/tournaments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaon Cup 2024",
    "location": "Village Ground",
    "startDate": "2024-04-20",
    "endDate": "2024-04-30",
    "sportType": "CRICKET",
    "oversPerInnings": 20,
    "maxTeams": 6
  }'
```

### 3. Add Teams
```bash
curl -X POST http://localhost:8080/api/teams \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Rampur XI","tournamentId":1,"captainName":"Mahesh"}'
```

### 4. Generate Fixtures
```bash
curl -X POST http://localhost:8080/api/matches/generate/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 5. Record Toss & Start Match
```bash
curl -X PATCH http://localhost:8080/api/matches/1/toss \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"tossWinnerId":1,"decision":"BAT"}'
```

### 6. Live Score Ball by Ball
```bash
# 4 runs
curl -X POST http://localhost:8080/api/matches/1/score \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"runsScored":4,"ballType":"NORMAL","isWicket":false}'

# Wicket
curl -X POST http://localhost:8080/api/matches/1/score \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"runsScored":0,"ballType":"NORMAL","isWicket":true,"wicketType":"BOWLED"}'
```

### 7. Public Match View (No Login)
```bash
# By match ID
curl http://localhost:8080/api/matches/1

# By public share code
curl http://localhost:8080/api/matches/public/A3F7B2

# Live data + recent balls
curl http://localhost:8080/api/matches/1/live
```

### 8. Leaderboard
```bash
curl http://localhost:8080/api/leaderboard/1
```

---

## 📡 WebSocket Live Updates

```javascript
// Include SockJS + STOMP in your HTML
const socket = new SockJS('http://localhost:8080/api/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  // Subscribe to live match updates
  stompClient.subscribe('/topic/match/1', (message) => {
    const match = JSON.parse(message.body);
    console.log('Score update:', match.innings1Runs, '/', match.innings1Wickets);
    // Auto-update your UI here
  });
});
```

---

## 🔒 Security Model

| Endpoint | Public | Organizer | Admin |
|----------|--------|-----------|-------|
| GET /tournaments | ✅ | ✅ | ✅ |
| POST /tournaments | ❌ | ✅ | ✅ |
| POST /matches/generate | ❌ | ✅ (own) | ✅ |
| POST /matches/{id}/score | ❌ | ✅ (own) | ✅ |
| GET /leaderboard | ✅ | ✅ | ✅ |
| GET /matches/public/{code} | ✅ | ✅ | ✅ |

---

## 🔥 Key Features

- **Auto fixture generation** — Round-robin scheduling with date spreading
- **Live scoring** — Ball-by-ball with auto run-rate calculation
- **Auto match completion** — Detects chase, all-out, overs-complete
- **Net Run Rate** — Auto-calculated after every completed match
- **WebSocket** — Real-time score push to all connected viewers
- **Shareable links** — `/match/A3F7B2` — public score page, no login needed
- **WhatsApp sharing** — One-click share for live scores
- **Swagger UI** — Complete interactive API docs
- **Role-based security** — JWT, organizers manage own tournaments only

---

## 🌐 Frontend Integration

The dashboard frontend (React/HTML) should call:
- `GET /api/matches/live` — to show live matches on homepage
- `GET /api/matches/public/{code}` — public score page
- WebSocket `/topic/match/{id}` — for auto-refreshing scores
- `POST /api/auth/login` → store JWT in localStorage → send in all requests

CORS is configured to allow `localhost:3000` and `localhost:5173`.
