# Meru

Android-first driving adventure — telemetry, ascent ranks, vehicle vault, workshops.

**Tagline:** Every drive becomes an ascent.

## Phase 1 (Bedrock) — current

- `android/` — Kotlin + Jetpack Compose shell (Welcome → Auth → Home/Drive/Trips/Garage/More)
- `backend/` — NestJS `/health` + `/v1/auth/*` (in-memory for Phase 1)
- `docker-compose.yml` — Postgres + Redis (for later phases)
- `plan/` — 10 sequential build phases
- `docs/` — agent bible + decisions/flow/brainstorm

## Run backend

```bash
cd backend
npm install
npm run start:dev
# GET http://localhost:3000/health
```

## Run Android

Open `android/` in Android Studio (SDK required). Emulator API base URL is `http://10.0.2.2:3000/`.

## Author

Commits: **engrmaziz** only (`Musharraf Aziz` / `io@maziz.me`).
