#!/bin/bash

# Burst test script for Seat Reservation System
# Usage: ./burst.sh <BASE_URL>
# Example: ./burst.sh http://localhost:8080

set -e

generate_seats() {
    local count=$1
    local seats="["
    for i in $(seq 1 $count); do
        if [ $i -gt 1 ]; then
            seats="$seats,"
        fi
        seats="$seats\"S$(printf '%03d' $i)\""
    done
    seats="$seats]"
    echo "$seats"
}

BASE_URL="${1:-http://localhost:8080}"
NUM_USERS=20
NUM_SEATS=100
HOT_SEAT="A1"
CONCURRENT_REQUESTS=50

echo "=========================================="
echo "Seat Reservation Burst Test"
echo "=========================================="
echo "Base URL: $BASE_URL"
echo "Users: $NUM_USERS"
echo "Seats: $NUM_SEATS"
echo "Concurrent requests: $CONCURRENT_REQUESTS"
echo "=========================================="

# Check if server is healthy
echo "Checking health endpoint..."
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health/liveness")
if [ "$HEALTH" != "200" ]; then
    echo "ERROR: Server not healthy (status: $HEALTH)"
    exit 1
fi
echo "Server is healthy ✓"

# Create a show
echo ""
echo "Creating show..."
SEATS_JSON=$(generate_seats $NUM_SEATS)
SHOW_RESPONSE=$(curl -s -X POST "$BASE_URL/shows" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"burst-test\",\"seats\":$SEATS_JSON,\"price_paise\":25000}")

SHOW_ID=$(echo "$SHOW_RESPONSE" | jq -r '.id')
echo "Show created with ID: $SHOW_ID"

# Generate user tokens
echo ""
echo "Generating user tokens..."
USER_TOKENS=()
for i in $(seq 1 $NUM_USERS); do
    USER_ID="550e8400-e29b-42d4-a716-$(printf '%012x' $i)"
    TOKEN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/token" \
        -H "Content-Type: application/json" \
        -d "{\"user_id\":\"$USER_ID\"}")
    TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.token')
    USER_TOKENS+=("$TOKEN")
done
echo "Generated $NUM_USERS user tokens ✓"

# Test 1: Normal concurrent reservations
echo ""
echo "=========================================="
echo "Test 1: Normal Concurrent Reservations"
echo "=========================================="

CONFIRMED=0
DECLINED_SEAT_TAKEN=0
DECLINED_PER_USER_LIMIT=0
DECLINED_IDEMPOTENT=0
ERRORS=0

for i in $(seq 1 $CONCURRENT_REQUESTS); do
    USER_INDEX=$(( (i - 1) % NUM_USERS ))
    TOKEN="${USER_TOKENS[$USER_INDEX]}"
    SEAT="S$(printf '%03d' $i)"
    IDEMPOTENCY_KEY="req-$i-$(date +%s%N)"

    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/shows/$SHOW_ID/reserve" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"seats\":[\"$SEAT\"],\"idempotency_key\":\"$IDEMPOTENCY_KEY\"}")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" = "201" ]; then
        ((CONFIRMED++))
    elif [ "$HTTP_CODE" = "409" ]; then
        ERROR_MSG=$(echo "$BODY" | jq -r '.error // .')
        if [ "$ERROR_MSG" = "seat_taken" ]; then
            ((DECLINED_SEAT_TAKEN++))
        elif [ "$ERROR_MSG" = "per_user_limit" ]; then
            ((DECLINED_PER_USER_LIMIT++))
        else
            ((DECLINED_IDEMPOTENT++))
        fi
    elif [[ "$HTTP_CODE" =~ ^5 ]]; then
        ((ERRORS++))
    fi
done

echo "Confirmed: $CONFIRMED"
echo "Declined (seat_taken): $DECLINED_SEAT_TAKEN"
echo "Declined (per_user_limit): $DECLINED_PER_USER_LIMIT"
echo "Declined (idempotent): $DECLINED_IDEMPOTENT"
echo "Server errors (5xx): $ERRORS"

# Test 2: Hot-seat storm (many users, one seat)
echo ""
echo "=========================================="
echo "Test 2: Hot-Seat Storm (Many Users, One Seat)"
echo "=========================================="

HOT_CONFIRMED=0
HOT_DECLINED=0
HOT_ERRORS=0

for i in $(seq 1 $CONCURRENT_REQUESTS); do
    USER_INDEX=$(( (i - 1) % NUM_USERS ))
    TOKEN="${USER_TOKENS[$USER_INDEX]}"
    IDEMPOTENCY_KEY="hot-$i-$(date +%s%N)"

    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/shows/$SHOW_ID/reserve" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"seats\":[\"$HOT_SEAT\"],\"idempotency_key\":\"$IDEMPOTENCY_KEY\"}")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

    if [ "$HTTP_CODE" = "201" ]; then
        ((HOT_CONFIRMED++))
    elif [ "$HTTP_CODE" = "409" ]; then
        ((HOT_DECLINED++))
    elif [[ "$HTTP_CODE" =~ ^5 ]]; then
        ((HOT_ERRORS++))
    fi
done

echo "Hot-seat confirmed: $HOT_CONFIRMED"
echo "Hot-seat declined: $HOT_DECLINED"
echo "Hot-seat errors (5xx): $HOT_ERRORS"

# Final reconciliation
echo ""
echo "=========================================="
echo "Final Reconciliation"
echo "=========================================="

STATE_RESPONSE=$(curl -s "$BASE_URL/shows/$SHOW_ID")
AVAILABLE=$(echo "$STATE_RESPONSE" | jq '.counts.available')
HELD=$(echo "$STATE_RESPONSE" | jq '.counts.held')
CONFIRMED=$(echo "$STATE_RESPONSE" | jq '.counts.confirmed')
TOTAL=$(echo "$STATE_RESPONSE" | jq '.counts.total')

echo "Available: $AVAILABLE"
echo "Held: $HELD"
echo "Confirmed: $CONFIRMED"
echo "Total: $TOTAL"

# Verify invariant
SUM=$((AVAILABLE + HELD + CONFIRMED))
if [ "$SUM" -eq "$TOTAL" ]; then
    echo "✓ Invariant holds: available + held + confirmed == total"
else
    echo "✗ Invariant broken: $SUM != $TOTAL"
fi

echo ""
echo "=========================================="
echo "Burst Test Complete"
echo "=========================================="
