# Wallet Service

REST API for wallet operations (deposit/withdraw) with high-concurrency support.
Handles 1000+ RPS per wallet using atomic database updates — no application-level locking.

## Tech Stack

- Java 17
- Spring Boot 3.2.5
- PostgreSQL 15
- Liquibase (database migrations)
- Docker + Docker Compose
- Maven

---

## Prerequisites (Fresh MacBook Setup)

Install the following on a new machine before running the project:

### 1. Homebrew (package manager)

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

After installation, follow the terminal instructions to add Homebrew to your PATH.

### 2. Java 17 (JDK)

```bash
brew install openjdk@17
```

Add to your shell profile (`~/.zshrc`):
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "/opt/homebrew/opt/openjdk@17")
export PATH="$JAVA_HOME/bin:$PATH"
```

Reload shell:
```bash
source ~/.zshrc
```

Verify:
```bash
java -version
# Expected: openjdk version "17.x.x"
```

### 3. Docker Desktop

```bash
brew install --cask docker
```

After installation:
1. Open Docker Desktop from Applications
2. Wait for the whale icon in the menu bar to stop animating (indicates Docker is ready)
3. Accept the license agreement if prompted

Verify:
```bash
docker --version
docker compose version
```

### 4. Git

```bash
brew install git
```

Verify:
```bash
git --version
```

### 5. curl (pre-installed on macOS)

Verify:
```bash
curl --version
```

### 6. (Optional) hey — for load testing

```bash
brew install hey
```

---

## How to Run

### Option A: Docker Compose (recommended — no Java/Maven needed on host)

This starts both PostgreSQL and the application in containers:

```bash
# Clone the repository
git clone <repository-url>
cd wallet-service

# Start everything
docker compose up --build
```

Wait until you see:
```
wallet-service-1  | Started WalletApplication in X.XX seconds
```

The service is now available at `http://localhost:8080`.

To stop:
```bash
docker compose down
```

To stop and remove data:
```bash
docker compose down -v
```

### Option B: Local Development (Java required on host)

```bash
# Start only PostgreSQL in Docker
docker compose up postgres -d

# Wait for PostgreSQL to be healthy
docker compose ps  # Status should show "healthy"

# Run the application locally
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

---

## API Endpoints

### 1. Perform Wallet Operation

```
POST http://localhost:8080/api/v1/wallet
Content-Type: application/json
```

**Request Body:**
```json
{
    "walletId": "550e8400-e29b-41d4-a716-446655440000",
    "operationType": "DEPOSIT",
    "amount": 1000
}
```

| Field | Type | Description |
|-------|------|-------------|
| `walletId` | UUID | Wallet identifier |
| `operationType` | String | `DEPOSIT` or `WITHDRAW` |
| `amount` | Long | Amount in minor units (positive integer) |

**Responses:**
| Status | Meaning |
|--------|---------|
| `200 OK` | Operation successful |
| `400 Bad Request` | Invalid JSON, missing fields, negative amount, or insufficient funds |
| `404 Not Found` | Wallet does not exist (WITHDRAW only; DEPOSIT auto-creates) |

### 2. Get Wallet Balance

```
GET http://localhost:8080/api/v1/wallets/{walletId}
```

**Response (200 OK):**
```json
{
    "walletId": "550e8400-e29b-41d4-a716-446655440000",
    "balance": 1000
}
```

**Error Response (404 Not Found):**
```json
{
    "message": "Wallet not found: 550e8400-e29b-41d4-a716-446655440000"
}
```

---

## Testing the Application

### Step 1: Start the application

```bash
docker compose up --build -d
```

### Step 2: Test with curl commands

```bash
# --- Test DEPOSIT (creates wallet automatically) ---
curl -X POST http://localhost:8080/api/v1/wallet \
  -H "Content-Type: application/json" \
  -d '{"walletId":"550e8400-e29b-41d4-a716-446655440000","operationType":"DEPOSIT","amount":1000}'
# Expected: HTTP 200 (empty body)

# --- Test GET BALANCE ---
curl http://localhost:8080/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000
# Expected: {"walletId":"550e8400-e29b-41d4-a716-446655440000","balance":1000}

# --- Test another DEPOSIT (adds to existing balance) ---
curl -X POST http://localhost:8080/api/v1/wallet \
  -H "Content-Type: application/json" \
  -d '{"walletId":"550e8400-e29b-41d4-a716-446655440000","operationType":"DEPOSIT","amount":500}'
# Expected: HTTP 200

curl http://localhost:8080/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000
# Expected: {"walletId":"550e8400-e29b-41d4-a716-446655440000","balance":1500}

# --- Test WITHDRAW (success) ---
curl -X POST http://localhost:8080/api/v1/wallet \
  -H "Content-Type: application/json" \
  -d '{"walletId":"550e8400-e29b-41d4-a716-446655440000","operationType":"WITHDRAW","amount":300}'
# Expected: HTTP 200

curl http://localhost:8080/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000
# Expected: {"walletId":"550e8400-e29b-41d4-a716-446655440000","balance":1200}

# --- Test WITHDRAW with INSUFFICIENT FUNDS ---
curl -v -X POST http://localhost:8080/api/v1/wallet \
  -H "Content-Type: application/json" \
  -d '{"walletId":"550e8400-e29b-41d4-a716-446655440000","operationType":"WITHDRAW","amount":99999}'
# Expected: HTTP 400
# Body: {"message":"Insufficient funds in wallet: 550e8400-e29b-41d4-a716-446655440000"}

# --- Test GET non-existent wallet ---
curl -v http://localhost:8080/api/v1/wallets/00000000-0000-0000-0000-000000000000
# Expected: HTTP 404
# Body: {"message":"Wallet not found: 00000000-0000-0000-0000-000000000000"}

# --- Test WITHDRAW from non-existent wallet ---
curl -v -X POST http://localhost:8080/api/v1/wallet \
  -H "Content-Type: application/json" \
  -d '{"walletId":"00000000-0000-0000-0000-000000000000","operationType":"WITHDRAW","amount":100}'
# Expected: HTTP 404
# Body: {"message":"Wallet not found: 00000000-0000-0000-0000-000000000000"}

# --- Test INVALID JSON ---
curl -v -X POST http://localhost:8080/api/v1/wallet \
  -H "Content-Type: application/json" \
  -d '{invalid json}'
# Expected: HTTP 400
# Body: {"message":"Invalid request body"}

# --- Test MISSING FIELDS ---
curl -v -X POST http://localhost:8080/api/v1/wallet \
  -H "Content-Type: application/json" \
  -d '{"operationType":"DEPOSIT","amount":100}'
# Expected: HTTP 400
# Body: {"message":"walletId: walletId must not be null"}

# --- Test NEGATIVE AMOUNT ---
curl -v -X POST http://localhost:8080/api/v1/wallet \
  -H "Content-Type: application/json" \
  -d '{"walletId":"550e8400-e29b-41d4-a716-446655440000","operationType":"DEPOSIT","amount":-100}'
# Expected: HTTP 400
# Body: {"message":"amount: amount must be greater than zero"}

# --- Test INVALID OPERATION TYPE ---
curl -v -X POST http://localhost:8080/api/v1/wallet \
  -H "Content-Type: application/json" \
  -d '{"walletId":"550e8400-e29b-41d4-a716-446655440000","operationType":"TRANSFER","amount":100}'
# Expected: HTTP 400
# Body: {"message":"Invalid request body"}
```

### Step 3: Run automated tests

```bash
# Make sure Docker Desktop is running (Testcontainers needs it)
./mvnw test
```

This runs:
- **6 unit tests** (WalletServiceTest) — tests service logic with mocked repository
- **10 integration tests** (WalletControllerIntegrationTest) — full HTTP tests against real PostgreSQL via Testcontainers, including a **100-thread concurrent deposit test**

### Step 4: Load test (1000 RPS verification)

```bash
# Install hey if not already
brew install hey

# Run the load test script
cd load-test
./load-test.sh
```

Or run manually:
```bash
# Send 10,000 requests with 100 concurrent connections
hey -n 10000 -c 100 \
  -m POST \
  -H "Content-Type: application/json" \
  -d '{"walletId":"550e8400-e29b-41d4-a716-446655440000","operationType":"DEPOSIT","amount":1}' \
  http://localhost:8080/api/v1/wallet
```

**What to verify in the output:**
- `Status code distribution: [200] 10000 responses` — all 200, zero 5XX
- `Requests/sec` — should exceed 1000
- Check final balance equals the number of requests sent

---

## Configuration

All settings are configurable via the `.env` file — no container rebuild needed:

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_DB` | wallet | Database name |
| `POSTGRES_USER` | wallet_user | Database username |
| `POSTGRES_PASSWORD` | wallet_pass | Database password |
| `POSTGRES_PORT` | 5432 | Exposed PostgreSQL port |
| `POSTGRES_MAX_CONNECTIONS` | 100 | PostgreSQL max connections |
| `POSTGRES_SHARED_BUFFERS` | 256MB | PostgreSQL shared buffers |
| `APP_PORT` | 8080 | Exposed application port |
| `HIKARI_MAX_POOL_SIZE` | 20 | Connection pool size |
| `SERVER_TOMCAT_THREADS_MAX` | 200 | Tomcat thread pool max |

**To change configuration:**
```bash
# Edit .env file
vim .env

# Restart without rebuilding
docker compose down
docker compose up
```

---

## Project Structure

```
wallet-service/
├── .env                         # Configuration (edit this, no rebuild)
├── .gitignore
├── Dockerfile                   # Multi-stage build (JDK build → JRE run)
├── docker-compose.yml           # PostgreSQL + App orchestration
├── pom.xml                      # Maven dependencies
├── mvnw / mvnw.cmd             # Maven wrapper (no Maven install needed)
├── load-test/                   # Load test scripts (remove before push)
│   ├── load-test.sh
│   └── README.md
└── src/
    ├── main/java/com/example/wallet/
    │   ├── WalletApplication.java        # Spring Boot entry point
    │   ├── controller/
    │   │   └── WalletController.java     # REST endpoints
    │   ├── dto/
    │   │   ├── WalletOperationRequest.java
    │   │   ├── WalletBalanceResponse.java
    │   │   └── ErrorResponse.java
    │   ├── entity/
    │   │   ├── Wallet.java               # JPA entity
    │   │   └── OperationType.java        # DEPOSIT/WITHDRAW enum
    │   ├── exception/
    │   │   ├── GlobalExceptionHandler.java
    │   │   ├── WalletNotFoundException.java
    │   │   └── InsufficientFundsException.java
    │   ├── repository/
    │   │   └── WalletRepository.java     # Atomic SQL queries
    │   └── service/
    │       └── WalletService.java        # Business logic
    ├── main/resources/
    │   ├── application.yml               # App config with env var placeholders
    │   └── db/changelog/
    │       ├── db.changelog-master.yaml
    │       └── changes/
    │           └── 001-create-wallet-table.yaml
    └── test/java/com/example/wallet/
        ├── WalletApplicationTests.java
        ├── controller/
        │   └── WalletControllerIntegrationTest.java  # 10 integration tests
        └── service/
            └── WalletServiceTest.java                # 6 unit tests
```

---

## Design Decisions

- **Atomic SQL UPDATE** for concurrency — the database handles row-level locking internally, keeping lock duration to microseconds. No pessimistic locking (causes thread starvation at scale) or optimistic locking (causes retry storms).
- **Balance stored as BIGINT** (minor currency units) to avoid floating-point arithmetic.
- **Database CHECK constraint** (`balance >= 0`) as a safety net against negative balances.
- **Deposit auto-creates wallet** via PostgreSQL `INSERT ... ON CONFLICT DO UPDATE` (upsert).
