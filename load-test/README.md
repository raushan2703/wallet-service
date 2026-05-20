# Load Test

Manual load test to verify 1000 RPS handling with zero 5XX errors.

## Prerequisites

Install `hey`:
```bash
# macOS
brew install hey

# Go
go install github.com/rakyll/hey@latest
```

## Run

```bash
# Start the app first
docker compose up -d

# Run load test (default: 10000 requests, 100 concurrency)
chmod +x load-test.sh
./load-test.sh

# Custom settings
REQUESTS=20000 CONCURRENCY=200 ./load-test.sh
```

## What to check

- **Status code distribution**: 100% should be `200`. Zero `5XX` responses.
- **Requests/sec**: should exceed 1000.
- **Final balance**: after 10000 deposits of 1, balance should be exactly 10000.

## Remove before pushing

```bash
rm -rf load-test/
```
