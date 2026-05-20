#!/bin/bash

# Load test for wallet-service
# Requires: hey (https://github.com/rakyll/hey)
# Install: brew install hey (macOS) or go install github.com/rakyll/hey@latest

BASE_URL="${BASE_URL:-http://localhost:8080}"
WALLET_ID="${WALLET_ID:-550e8400-e29b-41d4-a716-446655440000}"
REQUESTS="${REQUESTS:-10000}"
CONCURRENCY="${CONCURRENCY:-100}"

echo "=== Wallet Service Load Test ==="
echo "Target: $BASE_URL"
echo "Wallet: $WALLET_ID"
echo "Requests: $REQUESTS"
echo "Concurrency: $CONCURRENCY"
echo ""

echo "--- Step 1: Deposit load test (creates wallet if not exists) ---"
hey -n "$REQUESTS" -c "$CONCURRENCY" \
    -m POST \
    -H "Content-Type: application/json" \
    -d "{\"walletId\":\"$WALLET_ID\",\"operationType\":\"DEPOSIT\",\"amount\":1}" \
    "$BASE_URL/api/v1/wallet"

echo ""
echo "--- Step 2: Check final balance (should be $REQUESTS) ---"
curl -s "$BASE_URL/api/v1/wallets/$WALLET_ID" | python3 -m json.tool

echo ""
echo "--- Step 3: Mixed deposit/withdraw load test ---"
echo "Depositing 5000..."
hey -n 5000 -c "$CONCURRENCY" \
    -m POST \
    -H "Content-Type: application/json" \
    -d "{\"walletId\":\"$WALLET_ID\",\"operationType\":\"DEPOSIT\",\"amount\":1}" \
    "$BASE_URL/api/v1/wallet" > /dev/null 2>&1

echo "Withdrawing 3000..."
hey -n 3000 -c "$CONCURRENCY" \
    -m POST \
    -H "Content-Type: application/json" \
    -d "{\"walletId\":\"$WALLET_ID\",\"operationType\":\"WITHDRAW\",\"amount\":1}" \
    "$BASE_URL/api/v1/wallet"

echo ""
echo "--- Final balance ---"
curl -s "$BASE_URL/api/v1/wallets/$WALLET_ID" | python3 -m json.tool
