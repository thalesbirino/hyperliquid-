# Quick Start Guide - Hyperliquid Trading Bot

## 5-Minute Setup

### Step 1: Run the Application

```bash
mvn spring-boot:run
```

Wait for the application to start. You should see:
```
Started HyperliquidApplication in X.XXX seconds
```

### Step 2: Test the Webhook (Simplest Test)

Open a new terminal and run:

```bash
curl -X POST http://localhost:8080/api/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "action": "buy",
    "strategyId": "66e858a5-ca3c-4c2c-909c-34c605b3e5c7",
    "password": "Admin@9090"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Order executed successfully",
  "orderId": "MOCK-a1b2c3d4",
  "action": "BUY",
  "asset": "ETH",
  "size": "0.1",
  "price": "MARKET",
  "status": "EXECUTED",
  "executedAt": "2025-11-13T10:35:00"
}
```

**Check Console Output** - You'll see a beautiful order execution log:
```
╔══════════════════════════════════════════════════════════╗
║          HYPERLIQUID ORDER EXECUTED (MOCK MODE)          ║
╠══════════════════════════════════════════════════════════╣
║ Order ID      : MOCK-a1b2c3d4
║ Action        : BUY
║ Asset         : ETH
...
╚══════════════════════════════════════════════════════════╝
```

### Step 3: Access Swagger UI

Open your browser: **http://localhost:8080/swagger-ui/index.html**

You now have full interactive API documentation!

### Step 4: Login and Get JWT Token

In Swagger UI:
1. Expand **Authentication** → **POST /api/auth/login**
2. Click **Try it out**
3. Use these credentials:
   ```json
   {
     "username": "admin",
     "password": "password123"
   }
   ```
4. Click **Execute**
5. Copy the `token` from the response
6. Click **Authorize** button (top right)
7. Enter: `Bearer <your-token>`
8. Click **Authorize**

Now you can access all protected endpoints!

### Step 5: Explore the Database

Open browser: **http://localhost:8080/h2-console**

**Connection Settings:**
- JDBC URL: `jdbc:h2:file:./data/hyperliquid-db`
- Username: `sa`
- Password: *(leave empty)*

Click **Connect** and explore the tables!

## Pre-configured Test Data

### Users (Login: password123)
- **admin** (ADMIN role)
- **trader001** (USER role)
- **trader002** (USER role)

### Strategies (Password: Admin@9090)
- **ETH Scalping** - ID: `66e858a5-ca3c-4c2c-909c-34c605b3e5c7`
- **BTC Long-term** - ID: `f7a3b2c1-d4e5-6f78-9g01-h2i3j4k5l6m7`
- **SOL Momentum** - ID: `a1b2c3d4-e5f6-7g89-0h12-i3j4k5l6m7n8`
- **AVAX Swing** - ID: `b2c3d4e5-f6g7-8h90-1i23-j4k5l6m7n8o9`

## Common Operations

### Test Different Actions

**BUY Order:**
```bash
curl -X POST http://localhost:8080/api/webhook \
  -H "Content-Type: application/json" \
  -d '{"action": "buy", "strategyId": "66e858a5-ca3c-4c2c-909c-34c605b3e5c7", "password": "Admin@9090"}'
```

**SELL Order:**
```bash
curl -X POST http://localhost:8080/api/webhook \
  -H "Content-Type: application/json" \
  -d '{"action": "sell", "strategyId": "66e858a5-ca3c-4c2c-909c-34c605b3e5c7", "password": "Admin@9090"}'
```

### Import Postman Collection

1. Open Postman
2. Import `Hyperliquid-Trading-Bot.postman_collection.json`
3. Run the **Login** request first
4. JWT token will be auto-saved
5. Test all other endpoints!

## Next Steps

- Modify configs to test different assets
- Create new strategies
- Add new users
- Check logs for detailed execution info
- Explore Swagger UI for all available endpoints

## Troubleshooting

**Port already in use:**
```bash
# Change port in application.yml
server:
  port: 8081
```

**Database issues:**
```bash
# H2 in-memory database resets automatically on restart
# Just restart the application
mvn spring-boot:run
```

**Build errors:**
```bash
mvn clean install -U
```

## What's Running?

- **REST API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **H2 Console**: http://localhost:8080/h2-console
- **Webhook Endpoint**: http://localhost:8080/api/webhook (PUBLIC)
- **Auth Endpoint**: http://localhost:8080/api/auth/login (PUBLIC)
- **Management APIs**: Require JWT authentication

Enjoy testing! 🚀
