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
- **Context7 libs:** n/a (session Stitch/Context7 MCP not loaded)
- **Stitch screens:** manual Compose Welcome / Home / Drive Ready (Meru tokens)
- **DEC referenced:** DEC-016, DEC-017, DEC-018
- **What shipped:** Android Compose shell + Hilt/Room/DataStore/Retrofit; NestJS `/health` + `/v1/auth`; docker-compose Postgres/Redis; CI backend job; git author rule
- **What deferred (ponytail:):** Postgres-backed users; real Google Sign-In SDK; Android CI job until SDK on runner; Stitch MCP generation when MCP available
- **Gamify/motion added:** Home XP bar shell placeholder
- **Tests:** backend AuthService unit tests + build
- **Strix:** skipped
- **Follow-ups:** Phase 2 driving engine; install Android SDK locally to assemble APK

### 2026-09-15 — Phase 2 Pulse driving engine
- **Band / slice:** Phase 2 / Drive engine
- **Agent:** Cursor agent (commits as engrmaziz)
- **Ponytail rung used:** minimum + platform (Fused Location / FG service)
- **Context7 libs:** n/a (MCP not loaded this session)
- **Stitch screens:** n/a (functional Drive UI only)
- **DEC referenced:** DEC-017, DEC-018
- **What shipped:** JumpFilter + GeoMath + TelemetryAccumulator; Room trips/locations; DriveSessionController; DriveForegroundService; permission primer; Drive start/end; local Trips list; DrivingMode locks More tab; unit tests for distance/jumps
- **What deferred (ponytail:):** MapLibre UI, sensors/G-force, server trip sync, adaptive interval when stationary
- **Gamify/motion added:** n/a (Drive-safe)
- **Tests:** JVM unit tests in `TelemetryUnitTest.kt` (run when Android SDK present)
- **Strix:** skipped
- **Follow-ups:** Phase 3 cockpit polish + maps + sensors
