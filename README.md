# Meru

Android-first driving adventure — telemetry, ascent ranks, vehicle vault, workshops.

**Tagline:** Every drive becomes an ascent.

**Repo:** https://github.com/engrmaziz/meru

---

## Build status

| Phase | Codename | Status |
|------:|----------|--------|
| 1 | Bedrock | Done — app shell, auth, Nest health |
| 2 | Pulse | Done — GPS engine, Room trips, FG service |
| 3 | Cockpit | Done — MapLibre, calibration, G-gauge, drive modes |
| 4 | Afterglow | Done — summary, replay, WorkManager sync, trip ingest |
| 5 | Engine of Want | Done — server XP/scores, achievements, challenges, Home hub |
| 6–10 | … | Planned — see `plan/` |

---

## Phase 1 — Bedrock

- Android Compose shell: Welcome → Auth → Home / Drive / Trips / Garage / More
- NestJS `/health` + `/v1/auth/*` (in-memory)
- Docker Compose: Postgres + Redis (for later)
- Meru design tokens + Hilt / Room / DataStore / Retrofit

## Phase 2 — Pulse

- Fused Location + foreground drive service
- JumpFilter + TelemetryAccumulator → Room `trips` / `trip_locations`
- DrivingMode locks More mid-drive
- Trips list (local)

## Phase 3 — Cockpit

- MapLibre live route + bearing
- Phone calibration + MotionEngine G-gauge
- Drive modes: Minimal / Detailed / Performance / HUD
- Weak GPS / permission banners

## Phase 4 — Afterglow

- Local `TripProcessor`: events, elevation, provisional quality, geohash exploration XP
- Screens: Processing → Summary (score count-up) → Detail (timeline, speed/elev graphs, map replay)
- Home stats from completed trips
- WorkManager `TripSyncWorker` → idempotent `POST /v1/trips`
- NestJS in-memory trip store (dedupe by `userId:clientTripId`)

## Phase 5 — Engine of Want

- Server scoring: Quality / Exploration / Activity → Adventure Score + Driver Rating
- Versioned `GET /v1/scores/config` weights (not in APK)
- XP ledger, levels, titles; achievements + daily/weekly challenges on trip finalize
- Spoofed client `qualityScore` / `explorationXp` ignored for competitive awards
- Home game hub (XP bar motion, streak, next challenge); Summary provisional → server final
- More: post-drive notification opt-ins (level / challenge / streak)

---

## Run backend

```bash
cd backend
npm install
npm run start:dev
# GET http://localhost:3000/health
# POST http://localhost:3000/v1/trips  (Bearer meru_<userId>_…)
```

## Run Android

Open `android/` in Android Studio (SDK required). Emulator API base URL is `http://10.0.2.2:3000/`.

## Docs & plan

- `plan/` — 10 sequential phases
- `docs/` — agent bible, decisions, flow, BUILD-LOG
- `AGENTS.md` — agent entrypoint

## Author

Commits: **engrmaziz** only (`Musharraf Aziz` / `io@maziz.me`). No Cursor co-author trailers.
