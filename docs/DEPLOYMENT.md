# 🚀 Complete Deployment Guide
## Local Tournament System — From Local to Live on the Internet

---

## ❌ WHY YOU GOT 404

The original config had `server.servlet.context-path=/api` which means:
- Wrong URL: `http://localhost:8080/tournaments` → 404
- Correct URL was: `http://localhost:8080/api/tournaments`

**Fixed in this version** — no context path. URLs are now:
```
http://localhost:8080/auth/login         ✅
http://localhost:8080/tournaments        ✅
http://localhost:8080/swagger-ui/index.html  ✅
```

---

## ✅ STEP 1: RUN LOCALLY (Fixed)

### 1a. Install Prerequisites
- Java 17: https://adoptium.net/temurin/releases/?version=17
- MySQL 8.0: https://dev.mysql.com/downloads/installer/
- Maven: https://maven.apache.org/download.cgi (or use `./mvnw`)

### 1b. Setup Database
```sql
-- Run in MySQL Workbench or terminal:
CREATE DATABASE tournament_db CHARACTER SET utf8mb4;
```

### 1c. Edit application.properties
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### 1d. Run Backend
```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```

You should see:
```
✅ Admin user created: admin@tournament.local / Admin@123
✅ Demo organizer created: demo@tournament.local / Demo@123
Started TournamentApplication in X seconds
```

### 1e. Test It
```
API:     http://localhost:8080/tournaments     → should return JSON
Swagger: http://localhost:8080/swagger-ui/index.html  → API docs UI
```

### 1f. Open Frontend
Just open `frontend/index.html` in your browser (double-click the file).
Login with: `demo@tournament.local` / `Demo@123`

---

## 🌍 STEP 2: DEPLOY TO THE INTERNET (Free Options)

You need two things deployed:
1. **Backend** (Spring Boot) → Railway / Render / VPS
2. **Frontend** (HTML file) → Netlify / GitHub Pages / Vercel

---

## 🚂 OPTION A: Railway (Recommended — Easiest, Free tier)

### Deploy Backend to Railway

**Step 1:** Create `Dockerfile` in the `backend/` folder:
```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Step 2:** Push to GitHub
```bash
git init
git add .
git commit -m "Tournament system"
git remote add origin https://github.com/YOUR_USERNAME/tournament-system.git
git push -u origin main
```

**Step 3:** Deploy on Railway
1. Go to https://railway.app → Sign up with GitHub
2. Click "New Project" → "Deploy from GitHub repo"
3. Select your repository → Select the `backend` folder
4. Add a **MySQL** plugin: Click "+ Add" → "Database" → "MySQL"
5. Set Environment Variables (click "Variables"):
   ```
   SPRING_DATASOURCE_URL=${{MySQL.MYSQL_URL}}
   SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}
   SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
   JWT_SECRET=YourSuperSecretKeyHere123456789012345678
   SPRING_JPA_HIBERNATE_DDL_AUTO=update
   ```
6. Click "Deploy" — Railway builds and runs automatically
7. Go to "Settings" → "Domains" → "Generate Domain"
8. Your API URL: `https://tournament-abc123.railway.app`

**Cost:** Free tier = 500 hours/month (enough for testing; $5/mo for always-on)

---

## 🎨 OPTION B: Render (Free — Sleeps after 15min inactivity)

**Step 1:** Create `Dockerfile` (same as above)

**Step 2:** Go to https://render.com → Sign up

**Step 3:** Click "New" → "Web Service" → Connect GitHub repo

**Step 4:** Settings:
- Root Directory: `backend`
- Build Command: `mvn clean package -DskipTests`
- Start Command: `java -jar target/*.jar`
- Instance Type: Free

**Step 5:** Add PostgreSQL (free):
- New → PostgreSQL → Create
- Copy the Internal Database URL

**Step 6:** Update `application.properties` for PostgreSQL:
```properties
spring.datasource.url=${DATABASE_URL}
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```
Add dependency to pom.xml:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Step 7:** Set Environment Variables in Render dashboard:
```
DATABASE_URL=postgresql://user:pass@host/dbname
JWT_SECRET=YourSecretKey
```

---

## ☁️ OPTION C: VPS (Full Control — DigitalOcean/Hostinger ~$4/mo)

### 1. Get a VPS
- DigitalOcean Droplet: https://digitalocean.com ($6/mo, use referral for $200 free)
- Hostinger VPS: https://hostinger.com (~$4/mo)
- Choose Ubuntu 22.04

### 2. Install Java + MySQL on VPS
```bash
ssh root@YOUR_VPS_IP
apt update && apt upgrade -y
apt install openjdk-17-jdk mysql-server nginx -y

# Setup MySQL
mysql_secure_installation
mysql -u root -p
CREATE DATABASE tournament_db;
CREATE USER 'tournament'@'localhost' IDENTIFIED BY 'StrongPassword123';
GRANT ALL ON tournament_db.* TO 'tournament'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Deploy JAR
```bash
# On your local machine:
cd backend
mvn clean package -DskipTests
scp target/local-tournament-system-1.0.0.jar root@YOUR_VPS_IP:/app/

# On the VPS:
mkdir -p /app
```

### 4. Create systemd service (auto-restart on crash/reboot)
```bash
# On VPS:
cat > /etc/systemd/system/tournament.service << 'EOF'
[Unit]
Description=Tournament Management System
After=syslog.target network.target mysql.service

[Service]
User=root
WorkingDirectory=/app
ExecStart=/usr/bin/java -jar /app/local-tournament-system-1.0.0.jar
Environment=SPRING_DATASOURCE_USERNAME=tournament
Environment=SPRING_DATASOURCE_PASSWORD=StrongPassword123
Environment=SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/tournament_db
Environment=JWT_SECRET=YourSuperSecretKeyMakeThisLong123456
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable tournament
systemctl start tournament
systemctl status tournament
```

### 5. Setup Nginx reverse proxy + domain
```bash
cat > /etc/nginx/sites-available/tournament << 'EOF'
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;

    # Serve frontend
    root /var/www/tournament;
    index index.html;

    # API proxy
    location /api/ {
        rewrite ^/api/(.*) /$1 break;
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # WebSocket proxy
    location /ws/ {
        proxy_pass http://localhost:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # Frontend fallback
    location / {
        try_files $uri $uri/ /index.html;
    }
}
EOF

ln -s /etc/nginx/sites-available/tournament /etc/nginx/sites-enabled/
nginx -t && systemctl restart nginx

# Deploy frontend
mkdir -p /var/www/tournament
cp /path/to/frontend/index.html /var/www/tournament/
```

### 6. Add FREE HTTPS with Let's Encrypt
```bash
apt install certbot python3-certbot-nginx -y
certbot --nginx -d yourdomain.com -d www.yourdomain.com
# Follow the prompts — auto-renews!
```

Your app is now at: `https://yourdomain.com` 🎉

---

## 🌐 STEP 3: DEPLOY FRONTEND

### Option A: Netlify (Free, Instant)
1. Go to https://netlify.com
2. Drag and drop your `frontend/` folder onto the Netlify dashboard
3. Get instant URL like: `https://tournament-abc.netlify.app`
4. **Update API_BASE** in `frontend/index.html`:
   ```javascript
   const API_BASE = 'https://your-backend.railway.app';
   ```
5. Redeploy

### Option B: GitHub Pages (Free)
```bash
# Create a repo, push frontend/index.html to gh-pages branch
# Settings → Pages → Source: gh-pages branch
```

### Option C: Same Server (VPS)
Already covered above — Nginx serves the HTML file.

---

## 🔗 STEP 4: CONNECT FRONTEND TO BACKEND

After deploying backend, update this ONE line in `frontend/index.html`:
```javascript
// Line ~200 in index.html
const API_BASE = 'https://YOUR-BACKEND-URL.railway.app';
// Example:
const API_BASE = 'https://tournament-system-production.railway.app';
```

Also update CORS in `application.properties`:
```properties
cors.allowed-origins=https://your-frontend.netlify.app,https://yourdomain.com
```

---

## 📱 SHARE YOUR APP

Once deployed, you can share:

| What | URL |
|------|-----|
| Main app | `https://yourdomain.com` |
| Live match | `https://yourdomain.com?match=A3F7B2` |
| Leaderboard | `https://yourdomain.com?leaderboard=1` |

The live match link works WITHOUT login — perfect for WhatsApp sharing!

---

## 🔒 PRODUCTION SECURITY CHECKLIST

Before going live:
- [ ] Change JWT secret to a random 64+ char string
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` (not `update`)
- [ ] Set strong MySQL passwords
- [ ] Lock CORS to your frontend domain only
- [ ] Enable HTTPS (certbot on VPS, automatic on Railway/Render)
- [ ] Set `logging.level.com.tournament=WARN` in production

---

## 💰 COST COMPARISON

| Platform | Backend | Database | Frontend | Monthly |
|----------|---------|----------|----------|---------|
| Railway | Free 500hr | Free MySQL | Netlify Free | **₹0** |
| Render | Free (sleeps) | Free PostgreSQL | Netlify Free | **₹0** |
| VPS (Hostinger) | $4/mo | Included | Same server | **~₹330** |
| Full VPS (DO) | $6/mo | Included | Same server | **~₹500** |

**Recommendation for starting out:** Railway (backend) + Netlify (frontend) = Free

---

## ❓ TROUBLESHOOTING

| Problem | Fix |
|---------|-----|
| 404 on all endpoints | Remove `server.servlet.context-path=/api` from properties |
| 401 Unauthorized | Send `Authorization: Bearer TOKEN` header |
| CORS error in browser | Add your frontend URL to `cors.allowed-origins` |
| DB connection failed | Check username/password in application.properties |
| Port already in use | `lsof -ti:8080 \| xargs kill` |
| Swagger shows 404 | Access `http://localhost:8080/swagger-ui/index.html` (not `/swagger-ui.html`) |
| WebSocket not connecting | Normal if on free tier — scoring still works via HTTP polling |
