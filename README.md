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

### Option 1: VPS/Cloud Server (Recommended)

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

**Application will be available at:** `http://YOUR_SERVER_IP:8080`

### Option 2: GitHub Actions CI/CD

Every push to `main` triggers automated Docker testing:
- Builds Docker image
- Runs health checks
- Tests login endpoint
- Tests webhook endpoint
- Tests protected endpoints with JWT

Check workflow status at: `Actions` tab in GitHub repository.

### Option 3: Local Development (without Docker)

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
http://YOUR_SERVER_IP:8080/api/webhook
```

### 2. Alert Message Format

```json
{
  "action": "{{strategy.order.action}}",
  "strategyId": "66e858a5-ca3c-4c2c-909c-34c605b3e5c7",
  "password": "Admin@9090"
}
```

### 3. Available Strategies (Seed Data)

| Strategy | strategyId | Password |
|----------|-----------|----------|
| ETH Scalping | `66e858a5-ca3c-4c2c-909c-34c605b3e5c7` | `Admin@9090` |
| BTC Long-term | `f7a3b2c1-d4e5-6f78-9g01-h2i3j4k5l6m7` | `Admin@9090` |
| SOL Momentum | `a1b2c3d4-e5f6-7g89-0h12-i3j4k5l6m7n8` | `Admin@9090` |
| AVAX Swing | `b2c3d4e5-f6g7-8h90-1i23-j4k5l6m7n8o9` | `Admin@9090` |

### 4. Trading Modes

| Mode | Pyramid | Inverse | Behavior |
|------|---------|---------|----------|
| 1 | false | false | Normal: buy=open long, sell=close long |
| 2 | true | false | Pyramid: buy=add to long, sell=reduce long |
| 3 | false | true | Inverse: buy=open short, sell=close short |
| 4 | true | true | Inverse Pyramid: buy=add short, sell=reduce |

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
| GET | `/api/account/positions` | Get open positions |
| GET | `/api/account/open-orders` | Get open orders |
| DELETE | `/api/account/cancel-all` | Cancel all orders |

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
    "strategyId": "66e858a5-ca3c-4c2c-909c-34c605b3e5c7",
    "password": "Admin@9090"
  }'
```

**Get Positions (with JWT):**
```bash
curl http://localhost:8080/api/account/positions?userId=2 \
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
| trader002 | password123 | USER |

### Configs
| Name | Asset | Lot Size | Leverage |
|------|-------|----------|----------|
| ETH Scalping | ETH | 0.1 | 5x |
| BTC Long-term | BTC | 0.01 | 3x |
| SOL Momentum | SOL | 1.0 | 10x |
| AVAX Swing | AVAX | 5.0 | 5x |

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
