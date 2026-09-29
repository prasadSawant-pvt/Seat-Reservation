# WRITEUP.md - Seat Reservation System

## Atomic Decision Mechanism

The core concurrency challenge is ensuring exactly one reservation succeeds per seat under high load (~20,000 concurrent requests). The system uses **PostgreSQL conditional UPDATEs** as the atomic decision mechanism.

### Implementation

The `reserve_seats()` PostgreSQL function (in `V1__init.sql`) performs a conditional UPDATE:

```sql
UPDATE seats
SET status = 'held', held_by = p_user_id, held_until = p_held_until, reservation_id = p_reservation_id
WHERE show_id = p_show_id
  AND seat_label = ANY(p_seat_labels)
  AND (status = 'available' OR (status = 'held' AND held_until < CURRENT_TIMESTAMP));
```

This UPDATE is atomic at the database level. The function returns the number of rows updated. If the count doesn't match the requested seats, the transaction rolls back and returns 409.

### Why This Is Race-Free

- PostgreSQL's MVCC ensures the UPDATE sees a consistent snapshot
- The WHERE clause conditionally updates only if seats are available OR holds have expired
- The function executes in a single database transaction, eliminating read-then-write race conditions
- No application-level locking required - the database handles concurrency

### Multi-Seat Deadlock Avoidance

For multi-seat requests, seats are sorted alphabetically before reservation:

```java
List<String> sortedSeats = new ArrayList<>(uniqueSeats);
Collections.sort(sortedSeats);
```

This ensures all concurrent requests attempt to lock seats in the same deterministic order, preventing circular wait conditions that cause deadlocks.

## Idempotency

### Storage

Idempotency keys are stored in the `idempotency_keys` table with a UNIQUE constraint on `(user_id, key)`:

```sql
UNIQUE (user_id, key)
```

### Exactly-Once Enforcement

1. On reservation request, the system attempts to insert a new idempotency key record
2. If the UNIQUE constraint is violated, the system checks if the existing key has the same request fingerprint
3. **Same fingerprint**: Returns the original reservation (idempotent replay)
4. **Different fingerprint**: Returns 409 Conflict (same key used with different request body)

### Request Fingerprint

A SHA-256 hash of the sorted seat labels ensures that:
- `["A1", "A2"]` and `["A2", "A1"]` have the same fingerprint (seats are sorted)
- Different seat combinations have different fingerprints
- The fingerprint is stored in `idempotency_keys.request_fingerprint`

## Holds & Expiry Model

The system implements **time-boxed hold expiry** with a 5-minute hold duration:

### Hold Creation

When a reservation is created:
- Seats are set to `held` status with `held_until = NOW() + 300 seconds`
- The `reservation_id` is linked to the seats
- The `held_by` field tracks the owning user

### Expiry Mechanism

A scheduled job (`SeatExpiryScheduler`) runs every 60 seconds and calls the PostgreSQL `expire_held_seats()` function:

```sql
UPDATE seats
SET status = 'available', held_by = NULL, held_until = NULL, reservation_id = NULL
WHERE status = 'held' AND held_until < CURRENT_TIMESTAMP;
```

This function atomically releases expired holds back to available status.

### Explicit Cancel

Users can explicitly cancel held reservations via `POST /reservations/{id}/cancel`. The `cancel_reservation()` PostgreSQL function:
- Only cancels reservations in `held` status (never confirmed ones)
- Only allows the owner to cancel their own reservations
- Releases associated seats back to `available`
- Never resurrects a seat that has been confirmed to someone else

## Consistency vs Availability (CAP Stance)

The system prioritizes **consistency over availability** (CP system):

### Consistency Guarantees

- **No double-sell**: PostgreSQL UNIQUE constraints and conditional UPDATEs prevent duplicate reservations
- **Reconciliation invariant**: `available + held + confirmed == total_seats` always holds
- **Strong consistency**: All reads see the latest committed state (single database)

### Availability Trade-offs

- During database partitions or outages, the readiness health check returns 503
- The system fails closed rather than serving stale or inconsistent data
- No caching layer that could serve stale seat availability

### Rationale

For a ticket reservation system, consistency is critical:
- Selling the same seat twice is unacceptable
- Users must see accurate seat availability
- Financial transactions require strong consistency

## Observability

### Metrics (Prometheus)

The system exposes the following metrics at `/actuator/prometheus`:

1. **reservations_confirmed_total** (Counter) - Total successful reservations
2. **reservations_declined_total** (Counter, tagged by reason):
   - `reason=seat_taken` - Seat already held/confirmed
   - `reason=per_user_limit` - User exceeded seat limit
   - `reason=idempotent_replay` - Idempotent replay of existing request
3. **seats_available** (Gauge) - Current count of available seats globally

### What Would Trigger a Page at 2am

1. **High 5xx error rate** - Indicates server errors, not domain declines
2. **Zero reservations confirmed during expected high traffic** - Possible deadlock or blocking
3. **Reconciliation invariant broken** - `available + held + confirmed != total`
4. **Database connection pool exhaustion** - HikariCP metrics
5. **Scheduled job failures** - Seat expiry not running

### Structured Logging

Logs include:
- `request_id` - Correlation ID for tracing requests
- `user_id` - Authenticated user from JWT token
- `method`, `path`, `status`, `duration_ms` - Request metadata

Example log format:
```
2026-09-29T23:49:33.531+0530 [http-nio-0.0.0.0-8080-exec-3] INFO c.p.s.filter.RequestLoggingFilter - request_id=d1d0f0c5-d4fe-43eb-82f6-a716-a17d6345c1d2 user_id=550e8400-e29b-42d4-a716-000000231456 - method=POST path=/shows/aa24aa61-5b80-4fa9-8ff7-3a678db9bf51/reserve status=201 duration_ms=730
```

## AI Usage

### Directed vs Decided

**Directed (AI-assisted):**
- Boilerplate code (DTOs, entities, repositories)
- Filter ordering and logging setup
- Docker configuration and deployment setup
- Burst test script implementation

**Decided (human-owned):**
- Concurrency design (PostgreSQL conditional UPDATEs)
- Idempotency strategy (UNIQUE constraint + fingerprint)
- Deadlock prevention (seat sorting)
- Hold expiry model (time-boxed with scheduled job)
- CAP stance (consistency over availability)
- Metric selection and observability strategy

### Specific and Honest

AI was used for:
- Generating initial project structure and Spring Boot configuration
- Writing the burst test script
- Implementing filter ordering fixes
- Creating documentation templates

The core concurrency correctness mechanisms were designed and implemented by the developer, with AI handling supporting infrastructure.

## What I'd Do Next

1. **Add confirm endpoint** - `POST /reservations/{id}/confirm` to move from held to confirmed status
2. **Implement optimistic locking** - Add version column to reservations for safer concurrent updates
3. **Add circuit breaker** - For database connectivity to fail fast during outages
4. **Implement distributed tracing** - OpenTelemetry integration for cross-service tracing
5. **Add rate limiting** - Per-user rate limiting to prevent abuse
6. **Implement webhook notifications** - Notify users when holds are about to expire
7. **Add admin dashboard** - Real-time view of seat availability and reservation metrics
8. **Implement caching layer** - Redis cache for show state with proper invalidation
9. **Add comprehensive integration tests** - Load testing with Locust or k6
10. **Implement multi-region deployment** - Geo-distributed deployment with database replication
