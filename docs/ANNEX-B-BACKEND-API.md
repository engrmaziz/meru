# Annex B — Backend API, Workers, AuthZ (Detailed)

> NestJS modular monolith recommended. Version all routes under `/v1`.

---

## B.1 Auth & sessions
| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | `/auth/register` | public | rate limit |
| POST | `/auth/login` | public | |
| POST | `/auth/google` | public | idToken verify |
| POST | `/auth/refresh` | refresh | rotate refresh |
| POST | `/auth/logout` | user | revoke |
| POST | `/auth/password/forgot` | public | |
| POST | `/auth/password/reset` | public | |

Tokens: short-lived access JWT; opaque refresh in DB/Redis. Claims: `sub`, `role` (`user|workshop_staff|admin`), `workshop_id?`.

---

## B.2 Users
| Method | Path | Notes |
|--------|------|-------|
| GET | `/me` | profile + entitlements + flags |
| PATCH | `/me` | display, avatar, home geo, privacy |
| POST | `/me/push-token` | FCM |
| DELETE | `/me` | scheduled deletion job |
| GET | `/users/{username}` | public projection |

AuthZ: users cannot patch others; public profile respects privacy.

---

## B.3 Trips & telemetry
| Method | Path | Notes |
|--------|------|-------|
| POST | `/trips` | **Phase 4 (DEC-020):** monolithic upsert `{clientTripId, metrics, locations[], events[]}` — idempotent per user |
| POST | `/trips` | *(later)* create shell `{vehicle_id?, client_trip_id}` only |
| POST | `/trips/{id}/locations:batch` | gzip optional; max N points — deferred |
| POST | `/trips/{id}/events:batch` | deferred |
| POST | `/trips/{id}/complete` | triggers `trip.process` — deferred (client already processed locally) |
| GET | `/trips/{id}` | owner only — deferred |
| GET | `/trips` | paginated — deferred |
| DELETE | `/trips/{id}` | soft; removes from boards if policy |

Worker `trip.process`:
1. Dedupe points  
2. Jump filter  
3. Recompute distance/speeds/elevation  
4. Validate events  
5. Integrity score  
6. Provisional → final metrics  
7. Enqueue score/xp/achievements/exploration  

---

## B.4 Scores, XP, boards
| Method | Path | Notes |
|--------|------|-------|
| GET | `/scores/me` | components + overall |
| GET | `/scores/config` | public weights version |
| GET | `/leaderboards/{geoType}/{geoId}` | query: period, board, cursor |
| GET | `/seasons/current` | |
| GET | `/friends/leaderboard` | |

Worker `leaderboard.reindex`: recompute dirty users/geos; write `leaderboard_entries`; cache top N in Redis.

Eligibility: integrity gates; min activity; season membership; leaderboard opt-in.

---

## B.5 Challenges & achievements
- Admin CRUD configs.
- On trip finalize / daily tick: evaluate progress.
- Awards write `xp_ledger` + notifications.

---

## B.6 Garage
| Method | Path | Notes |
|--------|------|-------|
| GET/POST | `/vehicles` | enforce max vehicles entitlement |
| GET/PATCH/DELETE | `/vehicles/{id}` | owner |
| POST | `/vehicles/{id}/ownership-events` | |
| POST | `/vehicles/{id}/documents` | returns upload URL |
| GET | `/vehicles/{id}/timeline` | merged feed |
| POST | `/vehicles/{id}/services` | manual |
| GET | `/service-types` | taxonomy |
| POST | `/vehicles/{id}/history-shares` | workshop + scope + TTL |
| POST | `/billing/play/verify` | extra slot SKU |

---

## B.7 Workshops & bookings
| Method | Path | Notes |
|--------|------|-------|
| POST | `/workshops` | create + KYC pending |
| PATCH | `/workshops/{id}` | owner |
| GET | `/workshops` | search filters |
| GET | `/workshops/{id}` | public |
| CRUD | `/workshop-staff/services` | |
| CRUD | `/workshop-staff/parts` | |
| PUT | `/workshop-staff/hours` | |
| GET | `/workshops/{id}/slots` | available only |
| POST | `/bookings` | hold slot Redis lock |
| POST | `/bookings/{id}/cancel` | policy |
| POST | `/jobs/{id}/check-in` | staff |
| GET | `/jobs/{id}/shared-history` | staff + valid share |
| POST | `/jobs/{id}/extras` | staff |
| POST | `/jobs/{id}/extras/{eid}/decision` | owner |
| POST | `/invoices` | staff issue |
| POST | `/invoices/{id}/confirm` | owner → writeback |
| POST | `/invoices/{id}/dispute` | |
| POST | `/reviews` | completed jobs only |

### Slot concurrency
- `SET booking_hold:{slotId} NX EX 120`  
- Confirm within hold or release  
- DB unique constraint prevents double book  

### Invoice confirm writeback
Creates `service_records` source=`mechanic_issued_bill`, lines/parts, links `invoice_id`, sets `certified=true`.

---

## B.8 Admin
- Feature flags, weights, seasons, challenges, achievements, geo, workshop verify, disputes, user ban, integrity review.

All admin routes: role=admin + audit log.

---

## B.9 Error model
```json
{ "error": { "code": "SLOT_UNAVAILABLE", "message": "...", "details": {} } }
```
Standard codes: `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `VALIDATION`, `RATE_LIMITED`, `IDEMPOTENCY_CONFLICT`, `ENTITLEMENT_REQUIRED`, `INTEGRITY_BLOCKED`.

---

## B.10 Idempotency
Header `Idempotency-Key` on POST trips, bookings, invoice issue, service create.
