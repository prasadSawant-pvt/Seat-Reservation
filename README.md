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
[To be added after deployment]

## Architecture

- **Framework**: Spring Boot 4.1.1
- **Database**: PostgreSQL 17
- **Build Tool**: Maven 3.9
- **Container**: Docker multi-stage build
