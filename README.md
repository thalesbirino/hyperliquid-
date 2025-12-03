# Hyperliquid Trading Bot

Spring Boot application that receives TradingView webhooks and executes trades on Hyperliquid DeFi Exchange.

## Features

- TradingView webhook integration
- Real Hyperliquid API with EIP-712 signing (via Python SDK)
- JWT authentication for admin endpoints
- Strategy management with password protection
- 4 trading modes: pyramid/inverse combinations
- H2 in-memory database

## Tech Stack

- Java 17 + Spring Boot 3.2.0
- Python 3 + hyperliquid-python-sdk
- Docker + Docker Compose
- H2 Database (in-memory)

---

## Deployment

### Option 1: Docker (Recommended)

**Requirements:** Docker + Docker Compose installed

```bash
# 1. Clone repository
git clone https://github.com/thalesbirino/hyperliquid-.git
cd hyperliquid-

# 2. Start application
docker compose up -d --build

# 3. Check status
docker compose logs -f app

# 4. Verify health
curl http://localhost:8080/actuator/health
```

**Application will be available at:** `http://145.223.117.151:8080`

### Option 2: Local Development (without Docker)

```bash
# Requires Java 17+ and Maven
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## Post-Deployment Configuration

### 1. Login and Get JWT Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'
```

Save the `token` from response.

### 2. Configure Hyperliquid Wallet Credentials

The seed data contains placeholder credentials. Update with your real wallet:

```bash
curl -X PUT http://localhost:8080/api/user/2 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "username": "trader001",
    "email": "trader001@example.com",
    "password": "password123",
    "role": "USER",
    "hyperliquidAddress": "YOUR_VAULT_ADDRESS",
    "apiWalletPrivateKey": "YOUR_API_WALLET_PRIVATE_KEY",
    "apiWalletAddress": "YOUR_API_WALLET_ADDRESS",
    "isTestnet": false,
    "active": true
  }'
```

**Required Fields:**
| Field | Description |
|-------|-------------|
| `hyperliquidAddress` | Your vault address (from app.hyperliquid.xyz) |
| `apiWalletPrivateKey` | Private key of API Wallet (created in Hyperliquid settings) |
| `apiWalletAddress` | Public address of API Wallet |
| `isTestnet` | `false` for mainnet, `true` for testnet |

**Security:** Never commit real private keys to version control!

---

## TradingView Webhook Setup

### 1. Webhook URL

```
http://145.223.117.151:8080/api/webhook
```

### 2. Alert Message Format

```json
{
  "action": "{{strategy.order.action}}",
  "strategyId": "11111111-1111-1111-1111-111111111111",
  "password": "password123"
}
```

### 3. Available Strategies (Seed Data - ETH/USDC)

| Strategy | strategyId | Password |
|----------|-----------|----------|
| ETH Mode 1 - Normal | `11111111-1111-1111-1111-111111111111` | `password123` |
| ETH Mode 2 - Pyramid | `22222222-2222-2222-2222-222222222222` | `password123` |
| ETH Mode 3 - Inverse | `33333333-3333-3333-3333-333333333333` | `password123` |
| ETH Mode 4 - Inverse Pyramid | `44444444-4444-4444-4444-444444444444` | `password123` |

### 4. Trading Modes

| Mode | Pyramid | Inverse | Behavior |
|------|---------|---------|----------|
| 1 | false | false | Normal: buy=open long, sell=close long |
| 2 | true | false | Pyramid: buy=add to long, sell=reduce long |
| 3 | false | true | Inverse: buy=open short, sell=close short |
| 4 | true | true | Inverse Pyramid: buy=add short, sell=reduce |

---

## Testing Guide

This guide teaches you how to test the Hyperliquid Trading Bot step by step.

### Quick Start (3 Steps)

1. **Import Postman Collection** - File: `Hyperliquid-Trading-Bot.postman_collection.json`
2. **Configure Credentials** - Run "Update User - Configure Credentials" with your wallet
3. **Test Webhooks** - Run webhook requests for each trading mode

### Prerequisites

Before testing, ensure:

| Requirement | How to Check |
|-------------|--------------|
| App is running | `curl http://145.223.117.151:8080/actuator/health` returns `{"status":"UP"}` |
| Postman installed | Download from https://www.postman.com/downloads/ |
| Hyperliquid account | Create at https://app.hyperliquid.xyz |
| API Wallet created | Settings > API Wallet in Hyperliquid app |
| USDC balance | Minimum ~$5 for ETH trades (0.01 lot) |

### Step 1: Import Postman Collection

1. Open Postman
2. Click **Import** (top left)
3. Select file: `Hyperliquid-Trading-Bot.postman_collection.json`
4. Collection "Hyperliquid Trading Bot" will appear in sidebar

### Step 2: Login and Get JWT Token

1. Open folder: **1. Authentication**
2. Run: **Login - Admin**
3. Copy `token` from response (under `data.token`)
4. This token is used automatically by other requests

**Response example:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "username": "admin"
  }
}
```

### Step 3: Configure Hyperliquid Credentials

1. Open folder: **2. User Management**
2. Open: **Update User - Configure Credentials**
3. Edit the body with YOUR real credentials:

```json
{
  "username": "trader001",
  "email": "trader001@example.com",
  "password": "password123",
  "role": "USER",
  "hyperliquidAddress": "0xYOUR_VAULT_ADDRESS",
  "apiWalletPrivateKey": "0xYOUR_API_WALLET_PRIVATE_KEY",
  "apiWalletAddress": "0xYOUR_API_WALLET_ADDRESS",
  "isTestnet": false,
  "active": true
}
```

4. Add Authorization header: `Bearer YOUR_TOKEN`
5. Click **Send**

**Where to find your credentials:**

| Field | Where to Find |
|-------|---------------|
| `hyperliquidAddress` | Hyperliquid app > Portfolio > Your address (0x...) |
| `apiWalletPrivateKey` | Hyperliquid app > Settings > API Wallet > Export Private Key |
| `apiWalletAddress` | Hyperliquid app > Settings > API Wallet > Address |

### Step 4: Test Trading Modes

Open folder: **4. Webhook Tests** and test each mode:

#### MODE 1 - Normal (Long Only)

| Signal | Action | Expected Result |
|--------|--------|-----------------|
| BUY | Opens LONG | Position: +0.01 ETH |
| SELL | Closes LONG | Position: 0 |

**Test sequence:**
1. Run: **MODE 1 - BUY (Open Long)**
2. Check Hyperliquid app - should show LONG position (+0.01 ETH)
3. Run: **MODE 1 - SELL (Close Long)**
4. Check Hyperliquid app - position should be closed

#### MODE 2 - Pyramid (Accumulate Long)

| Signal | Action | Expected Result |
|--------|--------|-----------------|
| BUY | Adds to LONG | Position increases by 0.01 |
| SELL | Closes all | Position: 0 |

**Test sequence:**
1. Run: **MODE 2 - BUY (Open Long)**
2. Run: **MODE 2 - BUY (Add to Long)** again
3. Check Hyperliquid - position should be +0.02 ETH
4. Run: **MODE 2 - SELL** twice to close

#### MODE 3 - Inverse (Short Only)

| Signal | Action | Expected Result |
|--------|--------|-----------------|
| BUY | Opens SHORT | Position: -0.01 ETH |
| SELL | Closes SHORT | Position: 0 |

**Test sequence:**
1. Run: **MODE 3 - BUY (Open Short)**
2. Check Hyperliquid app - should show SHORT position (-0.01 ETH)
3. Run: **MODE 3 - SELL (Close Short)**
4. Check Hyperliquid app - position should be closed

#### MODE 4 - Inverse Pyramid (Accumulate Short)

| Signal | Action | Expected Result |
|--------|--------|-----------------|
| BUY | Adds to SHORT | Position decreases by 0.01 |
| SELL | Closes all | Position: 0 |

**Test sequence:**
1. Run: **MODE 4 - BUY (Open Short)**
2. Run: **MODE 4 - BUY (Add Short)** again
3. Check Hyperliquid - position should be -0.02 ETH
4. Run: **MODE 4 - SELL** twice to close

### Step 5: Verify Results

Use **5. Account Operations** folder:

1. **Get Positions** - Shows open positions on Hyperliquid
2. **Get Open Orders** - Shows pending orders (including Stop-Loss)
3. **Cancel All Orders** - Emergency cancel all orders

### Quick Reference - Strategy IDs

| Mode | Strategy ID | Pyramid | Inverse |
|------|-------------|---------|---------|
| 1 | `11111111-1111-1111-1111-111111111111` | false | false |
| 2 | `22222222-2222-2222-2222-222222222222` | true | false |
| 3 | `33333333-3333-3333-3333-333333333333` | false | true |
| 4 | `44444444-4444-4444-4444-444444444444` | true | true |

**Password for all strategies:** `password123`

### Troubleshooting

| Error | Cause | Solution |
|-------|-------|----------|
| `Invalid strategy ID or password` | Wrong credentials | Use exact strategyId and password from table above |
| `User wallet credentials not configured` | Missing API wallet | Configure via Step 3 |
| `Cannot add to existing position (Pyramid=FALSE)` | DB out of sync | Restart app: `docker compose restart app` |
| `Insufficient balance` | Not enough USDC | Add USDC to Hyperliquid account |
| `401 Unauthorized` | JWT expired/missing | Login again (Step 2) |
| `Order execution failed` | API issue | Check Hyperliquid app for details |

### Testing via cURL (Alternative)

If you prefer command line:

```bash
# Health check
curl http://145.223.117.151:8080/actuator/health

# Login
curl -X POST http://145.223.117.151:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'

# MODE 1 - Open Long
curl -X POST http://145.223.117.151:8080/api/webhook \
  -H "Content-Type: application/json" \
  -d '{"action":"buy","strategyId":"11111111-1111-1111-1111-111111111111","password":"password123"}'

# MODE 1 - Close Long
curl -X POST http://145.223.117.151:8080/api/webhook \
  -H "Content-Type: application/json" \
  -d '{"action":"sell","strategyId":"11111111-1111-1111-1111-111111111111","password":"password123"}'
```

### Reset Database

If you get sync issues between the app database and Hyperliquid:

```bash
# Restart app to reset H2 database
ssh root@145.223.117.151 "cd /opt/apps/hyperliquid- && docker compose restart app"

# Wait 30 seconds, then reconfigure credentials (Step 3)
```

---

## API Reference

### Public Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Get JWT token |
| POST | `/api/webhook` | TradingView webhook |
| GET | `/actuator/health` | Health check |

### Protected Endpoints (Require JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET/POST/PUT/DELETE | `/api/user/*` | User management |
| GET/POST/PUT/DELETE | `/api/config/*` | Config management |
| GET/POST/PUT/DELETE | `/api/strategy/*` | Strategy management |
| GET | `/api/account/{userId}/positions` | Get open positions |
| GET | `/api/account/{userId}/open-orders` | Get open orders |
| DELETE | `/api/account/{userId}/cancel-all` | Cancel all orders |

### Example Requests

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'
```

**Webhook:**
```bash
curl -X POST http://localhost:8080/api/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "action": "buy",
    "strategyId": "11111111-1111-1111-1111-111111111111",
    "password": "password123"
  }'
```

**Get Positions (with JWT):**
```bash
curl "http://localhost:8080/api/account/2/positions" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Docker Commands

```bash
# Start
docker compose up -d --build

# View logs
docker compose logs -f app

# Stop
docker compose down

# Rebuild after changes
docker compose up -d --build --force-recreate

# Check container status
docker ps

# Enter container shell
docker exec -it hyperliquid-bot sh
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 8080 | Application port |
| `SPRING_PROFILES_ACTIVE` | local | Spring profile |
| `JWT_SECRET` | (base64 string) | JWT signing key |
| `JWT_EXPIRATION` | 86400000 | Token expiration (24h) |
| `JAVA_OPTS` | -Xms384m -Xmx768m | JVM memory settings |

Override in `docker-compose.yml` or pass at runtime:
```bash
SERVER_PORT=9090 docker compose up -d
```

---

## Access Points

| Service | URL |
|---------|-----|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| H2 Console | http://localhost:8080/h2-console |
| Health Check | http://localhost:8080/actuator/health |

**H2 Console credentials:**
- JDBC URL: `jdbc:h2:mem:hyperliquid-db`
- Username: `sa`
- Password: (empty)

---

## Seed Data

### Users
| Username | Password | Role |
|----------|----------|------|
| admin | password123 | ADMIN |
| trader001 | password123 | USER |

### Configs
| Name | Asset | Lot Size | Leverage |
|------|-------|----------|----------|
| ETH Trading Config | ETH | 0.1 | 5x |

---

## Troubleshooting

### Container keeps restarting
```bash
# Check logs
docker compose logs app --tail 100

# Increase memory
# Edit docker-compose.yml: JAVA_OPTS=-Xms512m -Xmx1024m
```

### Health check fails
```bash
# Wait longer for startup (Spring Boot + Maven can be slow)
# Default start_period is 120s

# Manual health check
curl http://localhost:8080/actuator/health
```

### Database reset on restart
This is expected - H2 is in-memory. Data is reloaded from `data.sql` on each startup.

### JWT token expired
Tokens expire after 24 hours. Login again to get a new token.

---

## Project Structure

```
hyperliquid-/
├── .github/workflows/     # GitHub Actions CI/CD
│   └── docker-test.yml
├── scripts/               # Python SDK integration
│   └── order_executor.py
├── src/main/
│   ├── java/com/trading/hyperliquid/
│   │   ├── controller/    # REST endpoints
│   │   ├── service/       # Business logic
│   │   ├── model/         # Entities & DTOs
│   │   └── security/      # JWT authentication
│   └── resources/
│       ├── application.yml
│       ├── application-local.yml
│       └── data.sql       # Seed data
├── Dockerfile             # Multi-stage build
├── docker-compose.yml     # Container orchestration
└── pom.xml
```

---

## License

MIT License
