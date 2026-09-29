# Seat-Reservation
Seat Reservation at Scale handling 20k Requests

## Local Development

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker

### Setup
1. Start PostgreSQL:
```bash
docker-compose up -d
```

2. Run application:
```bash
./mvnw spring-boot:run
```

Or run from IntelliJ with VM options: `-Duser.timezone=Asia/Kolkata`

### Health Endpoints
- Liveness: http://localhost:8080/actuator/health/liveness
- Readiness: http://localhost:8080/actuator/health/readiness
- Prometheus: http://localhost:8080/actuator/prometheus

## Deployment

### Render Deployment

This project is configured for deployment on Render using the existing Dockerfile and managed PostgreSQL.

#### Environment Variables
The application uses the following environment variables (configured automatically via render.yaml):

- `DATABASE_URL` - PostgreSQL connection string (from Render managed DB)
- `DB_USERNAME` - Database username (from Render managed DB)
- `DB_PASSWORD` - Database password (from Render managed DB)

#### Deployment Steps

1. **Push code to GitHub** (if not already)
2. **Create Render account** at https://render.com
3. **Connect GitHub repository** to Render
4. **Render will automatically detect** `render.yaml` and create:
   - Web service (seat-reservation)
   - PostgreSQL database (seat-reservation-db)
5. **Environment variables** are automatically populated from the managed database
6. **Deploy** - Render will build using the Dockerfile and deploy

#### Health Checks
- Render uses `/actuator/health/readiness` as the health check path
- The readiness probe includes database connectivity checks
- Cold start is configured to fail closed if database is unavailable

#### Live URL
**Live URL: [To be added after deployment]**

### Burst Testing

**On Linux/Mac/Git Bash:**
```bash
chmod +x burst.sh
./burst.sh http://localhost:8080
```

**On Windows (PowerShell):**
```powershell
.\burst.ps1 http://localhost:8080
```

For a deployed instance:
```bash
./burst.sh https://your-app-url.onrender.com
# or on Windows:
.\burst.ps1 https://your-app-url.onrender.com
```

The script performs:
1. Health check
2. Show creation with 100 seats
3. 20 user token generation
4. Normal concurrent reservations (50 requests)
5. Hot-seat storm test (50 requests for same seat)
6. Final reconciliation verification

For 20k concurrent load testing, modify the script variables:
- `CONCURRENT_REQUESTS=20000` for full burst test
- `NUM_USERS=100` for more concurrent users
- `NUM_SEATS=500` for larger show

## API Testing Guide

### 1. Generate Auth Token

```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"user_id": "550e8400-e29b-42d4-a716-000000231456"}'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "user_id": "550e8400-e29b-42d4-a716-000000231456"
}
```

### 2. Create a Show

```bash
curl -X POST http://localhost:8080/shows \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Avengers: Endgame - 9:30 PM",
    "seats": ["A1","A2","A3","A4","A5","B1","B2","B3","B4","B5"],
    "price_paise": 35000
  }'
```

Response (201):
```json
{
  "id": "b45bf0e0-573c-4395-8157-cf36b6571435",
  "name": "Avengers: Endgame - 9:30 PM",
  "pricePaise": 35000,
  "perUserLimit": 4,
  "createdAt": "2026-09-29T18:31:01.147454Z",
  "seats": [
    {"seatLabel": "A1", "status": "available"},
    {"seatLabel": "A2", "status": "available"}
  ]
}
```

### 3. Reserve Seats

```bash
curl -X POST http://localhost:8080/shows/{showId}/reserve \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "seats": ["A1", "A2"],
    "idempotency_key": "unique-key-123"
  }'
```

Response (201 for new reservation, 200 for idempotent replay):
```json
{
  "reservationId": "5f1f3cb4-97e7-41fa-99d8-b5ba982eb4b7",
  "showId": "b45bf0e0-573c-4395-8157-cf36b6571435",
  "userId": "550e8400-e29b-42d4-a716-000000231456",
  "seats": ["A1", "A2"],
  "amountPaise": 70000,
  "status": "held",
  "createdAt": "2026-09-29T18:39:23.963339100Z"
}
```

### 4. Get Show State

```bash
curl -X GET http://localhost:8080/shows/{showId}
```

Response:
```json
{
  "id": "b45bf0e0-573c-4395-8157-cf36b6571435",
  "name": "Avengers: Endgame - 9:30 PM",
  "pricePaise": 35000,
  "perUserLimit": 4,
  "createdAt": "2026-09-29T18:31:01.147454Z",
  "counts": {
    "available": 8,
    "held": 2,
    "confirmed": 0,
    "total": 10
  },
  "seats": [
    {"seatLabel": "A1", "status": "held"},
    {"seatLabel": "A2", "status": "held"},
    {"seatLabel": "A3", "status": "available"}
  ]
}
```

### 5. Health Checks

```bash
# Liveness (always 200 if process is up)
curl http://localhost:8080/actuator/health/liveness

# Readiness (checks DB connectivity)
curl http://localhost:8080/actuator/health/readiness

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

### 6. 20k Concurrent Load Test

Modify `burst.sh` with these values for full burst test:
```bash
NUM_USERS=100
NUM_SEATS=500
CONCURRENT_REQUESTS=20000
HOT_SEAT="A1"
```

Then run:
```bash
./burst.sh http://localhost:8080
```

## Architecture

- **Framework**: Spring Boot 4.1.1
- **Database**: PostgreSQL 17
- **Build Tool**: Maven 3.9
- **Container**: Docker multi-stage build
