# Meru — Flows

> Canonical user & system flows. Agents implement vertical slices matching these.  
> Update this file when a flow changes; note the `DEC-XXX` if architectural.

---

## 0. Agent build flow (meta)

```
Read docs/README.md
 → decisions.md (constraints)
 → flow.md (this file)
 → brainstorm.md (ideas; only ship what’s flagged Ready)
 → AI-AGENT-BUILD-BIBLE.md + annexes
 → Context7 for library APIs
 → Stitch MCP for UI frames (non-Drive-heavy screens get max game juice)
 → Ponytail ladder while coding
 → Append BUILD-LOG.md
 → QA (functional) → Strix (security subset on staging)
```

---

## 1. First-run onboarding

```
Install
 → Welcome
 → Sign up / Google
 → Profile basics + home region (city/province/country)
 → Privacy defaults (leaderboard opt-in)
 → Permissions primer (location / notifications)
 → Phone calibration
 → Optional: Add first vehicle (S3)
 → Home
```

---

## 2. Core drive loop (S1) — must work offline-capable

```
Home / Drive Ready
 → Select vehicle (if garage)
 → Select dashboard mode
 → START DRIVE
 → Foreground service: GPS + sensors
 → Live UI (Minimal|Detailed|Performance|HUD)  [SAFE / LOW DISTRACTION]
 → END DRIVE (confirm)
 → Local persist trip
 → TripProcessor (events, elevation, geohash cells, provisional quality + XP)
 → Processing screen → Trip Summary (score count-up) → Detail (timeline / graphs / map replay)
 → WorkManager sync → POST /v1/trips (idempotent clientTripId) when online
 → Server ProgressionService: integrity + Quality/Exploration/Activity + XP/achievements/challenges
 → Client Summary shows provisional then overlays awards; Home refreshes scores/me
 → Home hub: XP bar, Adventure vs Driver Rating, streak, next challenge, season rank chips
 → Arena boards (city→global) when not driving; adventure map city %
```

### Mid-drive safety lock
While `session.active`: block Leaderboards, Social, long Challenges UI, Workshop browsing. Allow End Drive, map expand, mode switch if already designed as glanceable.

---

## 3. Gamification loop (S2)

```
Trip finalized (server)
 → Integrity gate
 → Award XP / achievements progress / challenge progress
 → Update Adventure Score components + Driver Rating
 → Exploration cells unlocked → celebration (post-drive)
 → Dirty leaderboard keys → reindex worker
 → Push (if opted-in): rank delta / challenge / streak
 → Home shows animated rank/XP
 → User opens City board / Friends board / Season hub
 → Ghost drive compare on repeat route (quality-first)
 → START NEXT DRIVE
```

### Season loop
```
Season starts → fresh seasonal ranks
 → compete week/month within season
 → season ends → badges permanently awarded
 → next season
```

---

## 4. Vehicle vault loop (S3)

```
Garage → Add vehicle (slot check / paywall)
 → Identity + ownership events + docs (signed URL stub)
 → One-tap Timeline (services + ownership + docs + linked trips)
 → Manual service log (offline OK → WorkManager sync)
 → Maintenance due + history share token (Phase 8)
 → Extra slot via Play verify stub
```

### Monetization microflow
```
Add 2nd car → Entitlement check
 → If max_vehicles reached → Play Billing
 → Verify purchase server-side → increment entitlement → allow add
```

---

## 5. Workshop marketplace loop (S4)

```
Select vehicle
 → Discover workshops (brand fit → distance → rating)
 → Open profile (services/parts/hours)
 → Pick slot (Redis hold)
 → Booking + history share scope + symptoms
 → Workshop accept
 → Check-in → staff views shared history
 → Job in progress → optional extra-work approval
 → Invoice builder → Issue
 → Owner reviews → Confirm
 → Certified service_record writeback into vault timeline
 → Review
 → Maintenance schedule updates
```

Workshop staff path (MVP = web portal):
```
Signup/KYC → Catalog + hours + slots
 → Incoming bookings → Check-in → Job → Invoice → Done
```

---

## 6. Sync & offline

```
Any local write (trip complete, manual service, draft booking)
 → Room PendingSync queue
 → WorkManager when network
 → API with Idempotency-Key
 → Mark Synced / Failed+backoff
```

---

## 7. Scoring authority

```
Client provisional score (UX only)
 → Upload telemetry
 → Worker integrity
 → Worker metrics/score/XP
 → Client refresh /me scores + boards
```

---

## 8. Design → code flow

```
Stitch MCP: generate mobile screen (theme prompt from ANNEX-E)
 → Human/agent picks variant
 → (Optional) Figma token sync
 → Compose screen + designsystem components
 → Wire ViewModel
 → Motion/gamify layer (if non-Drive)
 → Context7 if API unsure
 → BUILD-LOG entry
```

---

## 9. QA flow (layered)

```
1. Unit / Compose / API tests (every PR)
2. Emulator instrumented (permissions, Room)
3. Real-car protocol (S1/S2 telemetry)
4. Marketplace two-account E2E (S4)
5. Strix on staging API (authZ, IDOR vehicles/invoices/bookings) — NOT a replacement for 1–4
6. Privacy / Play Data Safety checklist pre-release
```

---

## 10. Production deploy flow (budget)

```
Local docker compose
 → Staging (single cheap box / PaaS)
 → Migrate DB
 → Feature flags off for unfinished stages
 → Closed beta city
 → Soft launch
 → Monitor crashes/sync/booking errors
 → Expand
```
