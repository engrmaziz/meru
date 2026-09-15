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
| GET | `/scores/me` | **Phase 5:** components + Adventure + Driver Rating + XP/level/title/streak |
| GET | `/scores/config` | **Phase 5:** public weights version |
| GET | `/leaderboards/{geoType}/{geoId}` | **Phase 6:** season/week boards; query `period` |
| GET | `/ranks/me` | **Phase 6:** 4 geo ranks for Home chips |
| GET | `/seasons/current` | **Phase 6** |
| GET | `/friends/leaderboard` | **Phase 6** |
| POST/DELETE | `/friends/{id}/follow` | **Phase 6** |
| GET | `/exploration/map` | **Phase 6** city % |
| GET | `/share/card` | **Phase 6** privacy-safe |
| GET/PATCH | `/privacy/me` | **Phase 6** board opt-in |
| GET/PATCH | `/admin/flags` | **Phase 6** thin local admin |

## B.5 Challenges & achievements
- **Phase 5:** `GET /achievements/me`, `GET /challenges`; evaluate on trip finalize (in-memory catalog).
- Admin CRUD configs deferred.
- Awards write xp ledger (in-memory) + notification prefs on client (no mid-drive push yet).

---

## B.6 Garage
| Method | Path | Notes |
|--------|------|-------|
| GET/POST | `/vehicles` | **Phase 7:** enforce max vehicles entitlement |
| GET/PATCH/DELETE | `/vehicles/{id}` | owner |
| POST | `/vehicles/{id}/ownership-events` | |
| POST | `/vehicles/{id}/documents` | returns upload URL stub |
| POST | `/vehicles/{id}/documents/{docId}/confirm` | marks uploaded |
| GET | `/vehicles/{id}/timeline` | merged feed |
| GET | `/vehicles/{id}/costs` | simple totals |
| POST | `/vehicles/{id}/services` | manual; idempotent `clientServiceId` |
| GET | `/service-types` | taxonomy |
| POST | `/vehicles/{id}/history-shares` | workshop + scope + TTL |
| GET | `/maintenance/due` | reminders |
| GET | `/billing/entitlement` | maxVehicles |
| POST | `/billing/play/verify` | stub increments slot |
| POST | `/billing/play/verify` | extra slot SKU |

---

## B.7 Workshops & bookings
| Method | Path | Notes |
|--------|------|-------|
| GET | `/workshops` | **Phase 8:** `make/lat/lon/q/verifiedOnly` — rank brandFit→distance→rating |
| GET | `/workshops/{id}` | public + services/parts/hours |
| GET | `/workshops/{id}/slots` | available only (hold map filtered) |
| POST | `/bookings` | **Phase 8:** NX hold then confirm; attach/create history share |
| GET | `/bookings` | my bookings |
| POST | `/bookings/{id}/cancel` | frees slot |
| GET | `/notifications` | booking confirmed/reminder stubs |
| POST | `/workshops` | create + KYC pending (Phase 9 staff) |
| PATCH | `/workshops/{id}` | owner |
| CRUD | `/workshop-staff/services` | Phase 9 |
| CRUD | `/workshop-staff/parts` | Phase 9 |
| PUT | `/workshop-staff/hours` | Phase 9 |
| POST | `/jobs/{id}/check-in` | staff |
| GET | `/jobs/{id}/shared-history` | staff + valid share |
| POST | `/jobs/{id}/extras` | staff |
| POST | `/jobs/{id}/extras/{eid}/decision` | owner |
| POST | `/invoices` | staff issue |
| POST | `/invoices/{id}/confirm` | owner → writeback |
| POST | `/invoices/{id}/dispute` | |
| POST | `/reviews` | completed jobs only |

### Slot concurrency
- **Phase 8:** in-memory hold map `slotId → {userId, expiresAtMs}` TTL 120s (Redis `SET NX EX 120` upgrade path)  
- Confirm within hold or release on expiry  
- DB unique constraint prevents double book (Postgres — Phase 9+)  

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
