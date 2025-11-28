# Hyperliquid Trading Bot - TradingView Webhook Integration

A Spring Boot POC that receives JSON webhooks from TradingView Pine Script strategies and executes corresponding trade orders on the Hyperliquid DeFi Exchange API.

## Features

- **TradingView Webhook Integration**: Receives and processes webhooks from TradingView strategies
- **Real Hyperliquid API Integration**: Full EIP-712 signing and HTTP client implementation
- **Strategy Management**: Create and manage trading strategies with credentials
- **User Management**: Manage users with Hyperliquid wallet integration
- **Config Management**: Define trading configurations (asset, lot size, SL/TP, leverage)
- **JWT Authentication**: Secure admin endpoints with JWT tokens
- **Mock & Real Modes**: Switch between simulation and real trading via configuration
- **Swagger UI**: Interactive API documentation
- **H2 Database**: In-memory database with seed data
- **Production Ready**: Complete deployment infrastructure with environment variables

## Tech Stack

- **Java 17+**
- **Spring Boot 3.2.0**
- **Maven**
- **H2 Database** (in-memory)
- **Spring Security** (JWT)
- **Lombok**
- **Swagger/OpenAPI**
- **Web3j 4.10.3** (EIP-712 signing for Hyperliquid authentication)
- **OkHttp 4.12.0** (HTTP client for API calls)
- **MessagePack** (for Hyperliquid API)

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd pochyperliquid
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Configure Hyperliquid Wallet Credentials

**IMPORTANT**: The seed data contains placeholder wallet credentials. Before testing with real trades, you must update the user with your actual Hyperliquid wallet:

```bash
# 1. First, login to get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'

# 2. Copy the token from response, then update trader001 (user ID 2) with your credentials
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

**Required Fields for Trading:**
- `hyperliquidAddress`: Your Hyperliquid vault address (the address you see on app.hyperliquid.xyz)
- `apiWalletPrivateKey`: Private key of your API Wallet (created in Hyperliquid settings)
- `apiWalletAddress`: Public address of your API Wallet
- `isTestnet`: Set to `false` for mainnet, `true` for testnet

**Security Note**: Never commit real private keys to version control!

### 5. Access Points

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:hyperliquid-db`
  - Username: `sa`
  - Password: (leave empty)
  - **Note**: In-memory database - data resets on restart

## API Endpoints

### Authentication (Public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login and receive JWT token |

**Login Request Example:**
```json
{
  "username": "admin",
  "password": "password123"
}
```

**Login Response Example:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "username": "admin",
    "email": "admin@tradingbot.com",
    "role": "ADMIN"
  },
  "timestamp": "2025-11-13T10:30:00"
}
```

### Webhook (Public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/webhook` | Receive TradingView webhook and execute order |

**Webhook Request Example:**
```json
{
  "action": "buy",
  "strategyId": "66e858a5-ca3c-4c2c-909c-34c605b3e5c7",
  "password": "Admin@9090"
}
```

**Webhook Response Example:**
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

### Strategy Management (Protected - Requires JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/strategy` | Get all strategies |
| GET | `/api/strategy/{id}` | Get strategy by ID |
| POST | `/api/strategy` | Create new strategy |
| PUT | `/api/strategy/{id}` | Update strategy |
| DELETE | `/api/strategy/{id}` | Delete strategy |

**Create Strategy Request Example:**
```json
{
  "name": "ETH Scalping Strategy",
  "strategyId": "66e858a5-ca3c-4c2c-909c-34c605b3e5c7",
  "password": "Admin@9090",
  "configId": 1,
  "userId": 2,
  "description": "Short-term scalping strategy for ETH",
  "active": true
}
```

### User Management (Protected - Requires JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/user` | Get all users |
| GET | `/api/user/{id}` | Get user by ID |
| POST | `/api/user` | Create new user |
| PUT | `/api/user/{id}` | Update user |
| DELETE | `/api/user/{id}` | Delete user |

**Create User Request Example:**
```json
{
  "username": "trader003",
  "email": "trader003@example.com",
  "password": "securePassword123",
  "role": "USER",
  "hyperliquidPrivateKey": "0x1234567890abcdef...",
  "hyperliquidAddress": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb",
  "active": true
}
```

### Config Management (Protected - Requires JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/config` | Get all configs |
| GET | `/api/config/{id}` | Get config by ID |
| POST | `/api/config` | Create new config |
| PUT | `/api/config/{id}` | Update config |
| DELETE | `/api/config/{id}` | Delete config |

**Create Config Request Example:**
```json
{
  "name": "ETH Scalping Config",
  "asset": "ETH",
  "assetId": 1,
  "lotSize": 0.1,
  "slPercent": 2.00,
  "tpPercent": 5.00,
  "leverage": 5,
  "orderType": "LIMIT",
  "timeInForce": "Gtc"
}
```

## TradingView Webhook Configuration

### Pine Script Strategy Example

```pine
//@version=5
strategy("ETH Strategy", overlay=true)

// Strategy logic here
if (buyCondition)
    strategy.entry("Long", strategy.long)

if (sellCondition)
    strategy.entry("Short", strategy.short)

// Webhook alert
if (strategy.position_size > 0)
    alert('{"action": "buy", "strategyId": "66e858a5-ca3c-4c2c-909c-34c605b3e5c7", "password": "Admin@9090"}', alert.freq_once_per_bar)

if (strategy.position_size < 0)
    alert('{"action": "sell", "strategyId": "66e858a5-ca3c-4c2c-909c-34c605b3e5c7", "password": "Admin@9090"}', alert.freq_once_per_bar)
```

### Setting Up Webhook in TradingView

1. Create an alert in TradingView
2. Set webhook URL: `http://your-server:8080/api/webhook`
3. Set alert message to JSON format:
   ```json
   {
     "action": "{{strategy.order.action}}",
     "strategyId": "66e858a5-ca3c-4c2c-909c-34c605b3e5c7",
     "password": "Admin@9090"
   }
   ```

## Testing with Postman

### Step 1: Login

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

Copy the `token` from the response.

### Step 2: Test Protected Endpoints

Add to request headers:
```
Authorization: Bearer <your-jwt-token>
```

### Step 3: Test Webhook

```
POST http://localhost:8080/api/webhook
Content-Type: application/json

{
  "action": "buy",
  "strategyId": "66e858a5-ca3c-4c2c-909c-34c605b3e5c7",
  "password": "Admin@9090"
}
```

Check the console for order execution logs:
```
╔══════════════════════════════════════════════════════════╗
║          HYPERLIQUID ORDER EXECUTED (MOCK MODE)          ║
╠══════════════════════════════════════════════════════════╣
║ Order ID      : MOCK-a1b2c3d4
║ Action        : BUY
║ Asset         : ETH
║ Asset ID      : 1
║ Size          : 0.1
║ Price         : $2500.00
║ Leverage      : 5x
║ Order Type    : LIMIT
║ Time In Force : Gtc
║ Stop Loss     : $2450.00 (2.00%)
║ Take Profit   : $2625.00 (5.00%)
║ User          : trader001
║ Wallet        : 0x8626...1199
║ Nonce         : 1699874400000
║ Status        : EXECUTED
╚══════════════════════════════════════════════════════════╝
```

## Seed Data

The application comes with pre-populated demo data:

### Users
- **admin** / password123 (ADMIN)
- **trader001** / password123 (USER)
- **trader002** / password123 (USER)

### Strategies
- **ETH Scalping Strategy** (strategyId: `66e858a5-ca3c-4c2c-909c-34c605b3e5c7`, password: `Admin@9090`)
- **BTC Long-term Hold** (strategyId: `f7a3b2c1-d4e5-6f78-9g01-h2i3j4k5l6m7`, password: `Admin@9090`)
- **SOL Momentum Trading** (strategyId: `a1b2c3d4-e5f6-7g89-0h12-i3j4k5l6m7n8`, password: `Admin@9090`)
- **AVAX Swing Strategy** (strategyId: `b2c3d4e5-f6g7-8h90-1i23-j4k5l6m7n8o9`, password: `Admin@9090`)

## Configuration

### application.yml

Key configurations:

```yaml
# Mock mode (set to false for real API calls)
hyperliquid:
  api:
    mock-mode: true
    use-testnet: true

# JWT settings
jwt:
  secret: <your-base64-secret>
  expiration: 86400000 # 24 hours
```

## Switching to Real Hyperliquid API

The system now has **complete real API integration** implemented. To switch from MOCK to REAL mode:

### Option 1: Using Environment Variables (Recommended for Production)

```bash
export HYPERLIQUID_MOCK_MODE=false
export HYPERLIQUID_USE_TESTNET=true  # Start with testnet
export JWT_SECRET="your-secure-jwt-secret"
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Option 2: Update application.yml

```yaml
hyperliquid:
  api:
    mock-mode: false
    use-testnet: true
```

### Required Steps:

1. **Get Hyperliquid Credentials**:
   - Testnet: https://app.hyperliquid-testnet.xyz/
   - Mainnet: https://app.hyperliquid.xyz/
   - You need: wallet address + private key

2. **Update User Database**:
```sql
UPDATE users SET
  hyperliquid_private_key = '0xYOUR_PRIVATE_KEY',
  hyperliquid_address = '0xYOUR_ADDRESS',
  is_testnet = true
WHERE username = 'trader001';
```

3. **Run Application**:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Real API Components Implemented:

- **HyperliquidSigner**: EIP-712 signature generation
- **HyperliquidHttpClient**: HTTP client with OkHttp
- **HyperliquidRealApiClient**: Complete order placement and cancellation
- **Production Configuration**: Environment variable support

## Project Structure

```
src/main/java/com/trading/hyperliquid/
├── client/                 # Hyperliquid API clients
│   ├── HyperliquidSigner.java       # EIP-712 signing
│   ├── HyperliquidHttpClient.java   # HTTP communication
│   └── HyperliquidRealApiClient.java # Main API orchestrator
├── config/                  # Configuration classes
├── controller/              # REST controllers
├── exception/               # Custom exceptions
├── model/
│   ├── dto/                # Data Transfer Objects
│   ├── entity/             # JPA entities
│   └── hyperliquid/        # Hyperliquid API models
├── repository/             # JPA repositories
├── security/               # Security filters and components
├── service/                # Business logic
└── util/                   # Utility classes
```

## Security Notes

- All passwords are BCrypt hashed
- JWT tokens expire after 24 hours
- Private keys should be stored securely (use environment variables in production)
- Webhook endpoint is public (validates strategy password)
- All management endpoints require JWT authentication

## Troubleshooting

### Database Issues

The application uses H2 in-memory database. Data is automatically reset on each restart and repopulated from `data.sql`.

### JWT Issues

Ensure your JWT secret in application.yml is Base64 encoded and at least 256 bits.

### Build Issues

```bash
mvn clean install -U
```

## License

MIT License

## Support

For issues and questions, please open an issue on GitHub.

---

Trading Bot POC v1.0.0
