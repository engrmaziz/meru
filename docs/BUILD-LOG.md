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
