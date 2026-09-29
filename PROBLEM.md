# PROBLEM.md --- Seat Reservation at Scale (reference spec)

This file is the single source of truth for the service. Keep it open in
Windsurf context (or paste into `.windsurfrules`) so every prompt is
grounded in the same spec. The email this is derived from is the
assignment; this file restates it as build-ready requirements. When code
and this file disagree, this file wins.

## 1. What we are building

A JSON HTTP API that sells assigned seats for a show (concert / movie
hall) and lets authenticated users reserve them. The business logic is
small. The entire challenge is **correctness under heavy concurrency**:
\~20,000 reservations arrive within one second, many fighting over the
same few "good" seats.

The service is the **system of record** that decides, atomically, who
gets each seat.

**Non-goals (do NOT build these)**

-   No UI. A JSON API is the whole deliverable. Not graded.
-   No payment gateway integration. We only record `amount_paise`.
-   No user-registration flow. Identity is a bearer token → `user_id`.
-   No Redis / queue / microservices required. A single Postgres
    database is encouraged.

## 2. Hard rules

-   Money is integer minor units (paise). Never floats, anywhere.
-   Identity comes from the auth token, never from the request body. A
    `user_id` in the body is ignored. A request can only ever act as the
    token's user.
-   A clean `git clone` must build and run via Docker with no manual
    steps.
-   Declines are domain outcomes (4xx), never server errors (5xx).

## 3. API contract

### 3.1 `POST /shows` --- create a show (admin)

Request:

``` json
{ "name": "friday-night", "seats": ["A1","A2","A3"], "price_paise": 25000 }
```

Response `201`: the created show with an `id` and every seat in
`available` state.

### 3.2 `POST /shows/{id}/reserve` --- reserve seat(s) (authenticated user)

Request (identity from token; NOT from body):

``` json
{ "seats": ["A12"], "idempotency_key": "..." }
```

Success `201`:

``` json
{
  "reservation_id": "...",
  "show_id": "...",
  "user_id": "...",
  "seats": ["A12"],
  "amount_paise": 25000,
  "status": "confirmed"
}
```

Decline outcomes (all **4xx**, never **5xx**):

-   Seat already held/confirmed by someone else → `409`
-   Per-user limit exceeded → `409` (or `422`) with a clear reason
-   Same idempotency key + different seats → `409`

### 3.3 Release a hold --- choose ONE model, document it

-   **Explicit cancel:** `POST /reservations/{id}/cancel` --- only the
    owner may cancel,

**OR**

-   **Time-boxed hold:** a hold auto-expires (e.g. `held_until`) and the
    seat returns to `available`.

A released/expired seat must become cleanly re-bookable. A release must
never resurrect a seat already `confirmed` to someone else.

### 3.4 `GET /shows/{id}` --- show state

Returns per-seat status (`available` / `held` / `confirmed`) and counts.
Invariant:

`available + held + confirmed == total_seats` at all times.

### 3.5 Health & metrics --- see §6.

## 4. The correctness bar (this is what gets tested)

A burst of \~20,000 concurrent reservations hits a fresh show, many
targeting the same hot seats, some retrying with the same idempotency
key. **ALL of these must hold:**

1.  **No double-sell.** For each hot seat stormed: exactly one `201`,
    everyone else `409`.
2.  **Zero 5xx across the whole burst.**
3.  **Reconciliation invariant**
    (`available + held + confirmed == total_seats`) holds to the unit,
    during and after the burst.
4.  **Idempotency.** Same key → exactly one reservation; a retry returns
    the original. Same key + different seats → `409`.
5.  **Per-user limit** (default `4`) holds under concurrency: a user
    firing 10 parallel reserves on a limit-4 show ends with at most 4
    held.
6.  **Token-derived identity.** A spoofed `user_id` in the body has no
    effect; a user can only cancel its own holds.

### The atomic-decision requirement (the heart of the exercise)

A read-then-write ("is A12 free? ok, take it") will double-sell under
load and is an automatic fail. The decision must live in a **single
atomic step**. Acceptable mechanisms:

-   A conditional `UPDATE` guarded on current state (update 0 rows =
    lost the race → `409`).
-   A unique constraint that makes a double-hold physically impossible.
-   A row lock taken in a deterministic order for multi-seat requests
    (prevents deadlock).

### Partial multi-seat requests

If a user asks for `["A12","A13"]` and only one is free, we choose and
document: **all-or-nothing (default recommendation)** --- if any
requested seat is unavailable, the whole request declines and nothing is
held. This must hold under concurrency.

## 5. Data model (guideline)

-   `shows(id, name, price_paise, per_user_limit default 4, created_at)`
-   `seats(show_id, seat_label, status ['available'|'held'|'confirmed'], held_by, held_until, reservation_id, PRIMARY KEY (show_id, seat_label))`
-   `reservations(id, show_id, user_id, amount_paise, status, created_at)`
-   `reservation_seats(reservation_id, show_id, seat_label) — link table`
-   `idempotency_keys(key, user_id, show_id, request_fingerprint, reservation_id, UNIQUE (user_id, key))`

The unique constraints and the guarded status transition are where
correctness lives.

## 6. Deploy & Observe (equally weighted with correctness)

-   **Live public URL** (Render / Railway / Fly.io free tier). Must
    survive a cold start and come up healthy. URL goes in the README.
-   **Containerized** (`Dockerfile + compose`) so a clean checkout runs
    the same way we deploy.
-   **Liveness endpoint** (always `200` if process is up).
-   **Readiness endpoint** that actually checks the DB and fails closed
    (`503`) when the dependency is down.
-   **Prometheus metrics**, at minimum:
    -   `reservations confirmed` --- counter
    -   `reservations declined by reason` --- counter labelled
        `seat_taken` / `per_user_limit` / `idempotent_replay`
    -   `seats available` --- gauge
    -   Metrics must reconcile with API state and with what the graders
        observe.
-   **Structured logs** with a correlation / request id. Public log
    access if the platform allows (or a short screen recording of live
    logs under load).
-   **One-command burst script** (`make burst` /
    `./burst.sh <BASE_URL>`) that reproduces the on-sale stampede
    against the live URL --- including a hot-seat storm (many users, one
    seat) --- and prints the outcome distribution (confirmed /
    declined-by-reason / 5xx) and the final reconciliation.

## 7. Deliverables

1.  Public Git repo with full, incremental commit history (they inspect
    how work was done).
2.  Live URL of the deployed service.
3.  One-command burst script + how to run it, in the README.
4.  Metrics + logs access.
5.  `WRITEUP.md` covering:
    -   the atomic decision (exact mechanism + why race-free; multi-seat
        deadlock avoidance)
    -   idempotency (where the key is stored, how exactly-once is
        enforced, same-key-different-body)
    -   holds & expiry model
    -   consistency vs availability under a partition (CAP stance)
    -   observability (what you'd get paged for at 2am)
    -   AI usage --- directed vs decided, specific and honest
    -   what you'd do next

## 8. Ground rules

-   AI tools allowed and expected; disclose honestly.
-   They will ask you to extend this service live in the interview ---
    the concurrency design must be genuinely yours. Own §4 cold; let AI
    handle boilerplate/plumbing.
-   A clean checkout must build and run. A down deploy or a failing
    clean build is the most common way strong submissions fail.

## 9. Chosen stack (fill in before generating code)

-   Language / framework: `Spring Boot 4.1.1 / Java 17`
-   Datastore: PostgreSQL 17
-   Deploy target: `Render`
-   Hold model: `time-boxed expiry`
-   Partial multi-seat policy: all-or-nothing (default)
