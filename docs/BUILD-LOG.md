# Meru — Build Log

> Append an entry **every time** an agent implements or significantly changes something.  
> Do not start entries until the human says to start building.

---

## Entry template

```
### YYYY-MM-DD — <short title>
- **Band / slice:** e.g. 0–10 Foundation / S1 Drive Live
- **Agent:** …
- **Ponytail rung used:** YAGNI | reuse | stdlib | platform | dep | one-liner | minimum
- **Context7 libs:** (if any) /nestjs/… /androidx/…
- **Stitch screens:** (if any)
- **DEC referenced:** DEC-XXX
- **What shipped:** …
- **What deferred (ponytail:):** …
- **Gamify/motion added:** … | n/a (Drive-safe)
- **Tests:** …
- **Strix:** skipped | scheduled | findings link
- **Follow-ups:** …
```

---

### 2026-09-15 — Phase 1 Bedrock foundation
- **Band / slice:** Phase 1 / Foundation
- **Agent:** Cursor agent (commits as engrmaziz)
- **Ponytail rung used:** minimum (scaffold) + YAGNI (in-memory auth)
- **Context7 libs:** n/a
- **Stitch screens:** manual Compose Welcome / Home / Drive Ready (Meru tokens)
- **DEC referenced:** DEC-016, DEC-017, DEC-018
- **What shipped:** Android Compose shell + Hilt/Room/DataStore/Retrofit; NestJS `/health` + `/v1/auth`; docker-compose; CI
- **What deferred (ponytail:):** Postgres users; real Google Sign-In; Android CI without SDK
- **Gamify/motion added:** Home XP bar placeholder
- **Tests:** backend AuthService unit tests
- **Strix:** skipped
- **Follow-ups:** Phase 2

### 2026-09-15 — Phase 2 Pulse driving engine
- **Band / slice:** Phase 2 / Drive engine
- **Agent:** Cursor agent (commits as engrmaziz)
- **Ponytail rung used:** minimum + platform (Fused Location / FG service)
- **Context7 libs:** n/a
- **Stitch screens:** n/a
- **DEC referenced:** DEC-017, DEC-018
- **What shipped:** JumpFilter + TelemetryAccumulator; Room trips; FG service; Trips list; DrivingMode
- **What deferred (ponytail:):** MapLibre, sensors, server sync
- **Gamify/motion added:** n/a (Drive-safe)
- **Tests:** TelemetryUnitTest
- **Strix:** skipped
- **Follow-ups:** Phase 3

### 2026-09-15 — Phase 3 Cockpit live drive UI
- **Band / slice:** Phase 3 / Cockpit
- **Agent:** Cursor agent (commits as engrmaziz only)
- **Ponytail rung used:** platform (MapLibre MapView) + minimum motion projection
- **Context7 libs:** n/a
- **Stitch screens:** Compose Drive modes hand-built to Meru tokens (calm)
- **DEC referenced:** DEC-008, DEC-017, DEC-018, DEC-019
- **What shipped:** MapLibre live route + bearing; phone calibration; MotionEngine G-gauge; Minimal/Detailed/Performance/HUD; weak GPS / permission banners; expand map; More → Calibrate
- **What deferred (ponytail:):** full IMU fusion, custom dark tiles, Stitch MCP iteration
- **Gamify/motion added:** none mid-drive
- **Tests:** MotionMathTest
- **Strix:** skipped
- **Follow-ups:** Phase 4 trip summary / replay / sync

### 2026-09-15 — Phase 4 Afterglow trip experience + sync
- **Band / slice:** Phase 4 / Afterglow
- **Agent:** Cursor agent (commits as engrmaziz only)
- **Ponytail rung used:** platform (WorkManager) + minimum (in-memory trip upsert) + YAGNI (single POST vs batch APIs)
- **Context7 libs:** n/a
- **Stitch screens:** Compose Processing / Summary / Detail hand-built (Meru tokens, post-drive juice)
- **DEC referenced:** DEC-015, DEC-019, DEC-020
- **What shipped:** TripProcessor + geohash exploration; Processing→Summary→Detail (timeline, graphs, replay); Home stats; TripSyncWorker; Nest `POST /v1/trips` idempotent ingest
- **What deferred (ponytail:):** Postgres trip tables; multipart location batches; server-final integrity worker; seasonal XP
- **Gamify/motion added:** Summary score count-up, XP bar, new-cells toast (post-drive only)
- **Tests:** GeoHashTest; TripsService unit; e2e trip upsert dedupe
- **Strix:** skipped
- **Follow-ups:** Phase 5 Engine of Want (server XP/achievements)

### 2026-09-15 — Phase 5 Engine of Want progression
- **Band / slice:** Phase 5 / Engine of Want
- **Agent:** Cursor agent (commits as engrmaziz only)
- **Ponytail rung used:** YAGNI (in-memory progression) + minimum server authority on trip upsert
- **Context7 libs:** n/a
- **Stitch screens:** Compose Home hub / Achievements / Challenges hand-built (Meru tokens, max post-drive juice)
- **DEC referenced:** DEC-011 (server authority), DEC-019, DEC-020, DEC-021
- **What shipped:** ProgressionService (scores, XP, titles, achievements, challenges); trip upsert awards; Android ProgressionStore; Home hub; Summary provisional→final; More notif toggles
- **What deferred (ponytail:):** Postgres xp_ledger; push delivery; admin CRUD UI; full season boards
- **Gamify/motion added:** XP bar animate, streak scale, achievement rarity frames, challenge progress
- **Tests:** ProgressionService spoof-ignore + dedupe; e2e scores/me after trip
- **Strix:** skipped
- **Follow-ups:** Phase 6 Arena (leaderboards / seasons)

### 2026-09-15 — Phase 6 Arena boards & adventure map
- **Band / slice:** Phase 6 / Arena
- **Agent:** Cursor agent (commits as engrmaziz only)
- **Ponytail rung used:** YAGNI (in-memory boards + bot seed) + platform (Driving Mode lock)
- **Context7 libs:** n/a
- **Stitch screens:** Compose Arena / Adventure map / Share card hand-built
- **DEC referenced:** DEC-011, DEC-019, DEC-022
- **What shipped:** Geo seed PK/PB/LHR; season boards reindex on trip; friends follow; ranks/me; exploration map; ghost compare; share card; opt-out; Driving Mode board lock; admin flags stub
- **What deferred (ponytail:):** Redis top-N; Postgres leaderboard_entries; push rank deltas; MapLibre exploration polygons
- **Gamify/motion added:** Podium scale-in, rank delta chips, Home rank strip
- **Tests:** ArenaService ranks + opt-out
- **Strix:** skipped
- **Follow-ups:** Phase 7 Vault (garage)

### 2026-09-15 — Phase 10 Summit soft launch harden
- **Band / slice:** Phase 10 / Summit
- **Agent:** Cursor agent (commits as engrmaziz only)
- **Ponytail rung used:** YAGNI (in-memory launch flags/metrics/rate limits) + reuse (admin over Arena flags)
- **Context7 libs:** n/a
- **Stitch screens:** n/a (store listing checklist only)
- **DEC referenced:** DEC-005, DEC-007, DEC-019, DEC-026
- **What shipped:** LaunchService flags + Lahore soft launch; admin key; metrics; auth/booking rate limits; stricter integrity; account deletion; Android remote flags + delete account; security e2e (IDOR/admin/delete); SOFT-LAUNCH + SUPPORT docs
- **What deferred (ponytail:):** Live Strix against public staging; Crashlytics wiring; Postgres persistence; FCM
- **Gamify/motion added:** none (harden phase)
- **Tests:** LaunchService unit; security.e2e-spec; full suite green
- **Strix:** Local security regression suite = pass #2 stand-in. Findings: 0 criticals in IDOR/admin/share paths covered. Full Strix when staging URL available.
- **Follow-ups:** Public expansion after soft-launch checklist signed

### 2026-09-15 — Phase 9 Stamp jobs & vault writeback
- **Band / slice:** Phase 9 / Stamp
- **Agent:** Cursor agent (commits as engrmaziz only)
- **Ponytail rung used:** YAGNI (in-memory jobs/invoices) + reuse (Vault writeback + Bay bookings) + Next.js staff MVP
- **Context7 libs:** n/a
- **Stitch screens:** Compose Stamp / invoice / certified stamp hand-built
- **DEC referenced:** DEC-011, DEC-014, DEC-019, DEC-025
- **What shipped:** Job lifecycle; staff check-in + shared history (share expiry denies); extras approve/deny; invoice issue/confirm/dispute; certified timeline writeback; reviews; Android Stamp UI + CERTIFIED chip; `workshop-web` staff portal
- **What deferred (ponytail:):** Real R2 PDF bytes; staff JWT roles; payments; Postgres unique constraints
- **Gamify/motion added:** Certified stamp scale celebration + timeline CERTIFIED chip
- **Tests:** JobsService writeback / no-confirm / share expiry / review gate; e2e confirm→timeline
- **Strix:** pass #1 notes (manual checklist, no staging deploy): verify IDOR on `GET/POST /v1/invoices/*` (foreign user 403), share expiry on `GET /v1/jobs/:id/shared-history`, workshop header mismatch 403, confirm required before certified. Full Strix deferred to Phase 10 staging URL.
- **Follow-ups:** Phase 10 Summit harden + soft launch

### 2026-09-15 — Phase 8 Bay discovery & booking
- **Band / slice:** Phase 8 / Bay
- **Agent:** Cursor agent (commits as engrmaziz only)
- **Ponytail rung used:** YAGNI (in-memory workshops + holds) + reuse (Vault history share + MapLibre)
- **Context7 libs:** n/a
- **Stitch screens:** Compose Bay / Workshop detail / Bookings hand-built (Stitch MCP unavailable)
- **DEC referenced:** DEC-019, DEC-023, DEC-024
- **What shipped:** Lahore workshop seed; brand-fit list; slots; NX-style hold + confirm booking; history share attach; Android Bay UI + MapLibre pin; My Bookings; Driving Mode lock; booking confirmed scale-in
- **What deferred (ponytail:):** Redis holds; Postgres bookings; staff web; invoices/payments; push reminders
- **Gamify/motion added:** Booking confirmed scale celebration
- **Tests:** WorkshopsService brand-fit rank + double-book + hold expiry
- **Strix:** skipped
- **Follow-ups:** Phase 9 Invoice → vault writeback

### 2026-09-15 — Phase 7 Vault garage & timeline
- **Band / slice:** Phase 7 / Vault
- **Agent:** Cursor agent (commits as engrmaziz only)
- **Ponytail rung used:** YAGNI (in-memory vault + Play verify stub) + platform (Room + WorkManager)
- **Context7 libs:** n/a
- **Stitch screens:** Compose Garage / Timeline / Add Vehicle / Paywall hand-built
- **DEC referenced:** DEC-010 (1 free car), DEC-019, DEC-023
- **What shipped:** Vehicles CRUD + entitlement; timeline merge; docs upload stub; offline services sync; history share; Drive links active vehicle
- **What deferred (ponytail:):** Real R2/S3; Google Play Billing Library; certified stamp juice; PDF export
- **Gamify/motion added:** light (timeline stamps deferred to Phase 9 writeback)
- **Tests:** VaultService slot gate + timeline idempotent service
- **Strix:** skipped
- **Follow-ups:** Phase 8 Bay (workshop discovery/booking)
