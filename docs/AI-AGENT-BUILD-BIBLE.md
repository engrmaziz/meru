# Meru — AI Agent Build Bible (A–Z / 0–100)

> **PURPOSE:** Single source of truth for AI coding agents building **Meru**.  
> **PLATFORM (NOW):** Android only (Kotlin + Jetpack Compose).  
> **DO NOT BUILD** until human says `start phase N` (see `plan/`).  
> **RELATED:** `decisions.md`, `flow.md`, `brainstorm.md`, conversation stages S1–S4.

**Working product name:** Meru  
**Tagline:** *Every drive becomes an ascent.*  
**Package:** `app.meru.android`  
**Phases:** `plan/1.md` … `plan/10.md`

---

# TABLE OF CONTENTS

1. [Agent Operating Rules](#1-agent-operating-rules)
2. [Product Vision & Customer Attraction](#2-product-vision--customer-attraction)
3. [Recommended Design Tooling (Stitch, Figma, MCP, Skills)](#3-recommended-design-tooling)
4. [Technology Stack (Locked Recommendations)](#4-technology-stack)
5. [System Architecture](#5-system-architecture)
6. [Repository & Module Layout](#6-repository--module-layout)
7. [Design System & Theming](#7-design-system--theming)
8. [Complete Screen Inventory (Every Screen + Widgets)](#8-complete-screen-inventory)
9. [Frontend Feature Matrix (Android)](#9-frontend-feature-matrix)
10. [Backend Feature Matrix (APIs, Jobs, Admin)](#10-backend-feature-matrix)
11. [Database Schema (PostgreSQL + Room Mirror)](#11-database-schema)
12. [S1–S4 Domain Specs (Build Contracts)](#12-s1s4-domain-specs)
13. [Build Roadmap 0–100 (Phased Delivery)](#13-build-roadmap-0-100)
14. [Team Swimlanes: Frontend / Backend / DB / QA](#14-team-swimlanes)
15. [QA Strategy (Unit → Real Car → Marketplace)](#15-qa-strategy)
16. [Security, Privacy, Compliance](#16-security-privacy-compliance)
17. [Observability, Analytics, Crash Monitoring](#17-observability)
18. [Monetization & Feature Flags](#18-monetization--feature-flags)
19. [Launch Strategy](#19-launch-strategy)
20. [Agent Task Checklists (Copy/Paste)](#20-agent-task-checklists)
21. [Appendix: Event Taxonomy, Scoring Weights, Service Catalog](#21-appendix)

---

# 1. Agent Operating Rules

1. Read this bible before coding any feature.
2. Prefer **vertical slices** (one user journey end-to-end) over random screens.
3. Keep **Driving Engine** independent of UI (foreground service + repository).
4. **Never** trust client for final competitive score, invoice finalization, or leaderboard rank.
5. During an active drive: **no leaderboards, social feeds, long notifications, or booking UI**.
6. Privacy defaults: hide exact home/work; share workshop history only with explicit scope + expiry.
7. Do not gamify raw top speed as a primary competitive metric.
8. Offline-first for trips and manual garage entries; sync with idempotent IDs.
9. Match existing code style when the repo exists; until then follow this bible.
10. Update living docs while building: `decisions.md`, `flow.md`, `brainstorm.md`, `BUILD-LOG.md`.
11. **Ponytail** coding ladder ([repo](https://github.com/DietrichGebert/ponytail)) — see `AGENTS.md`.
12. **Context7** ([repo](https://github.com/upstash/context7)) before unfamiliar library APIs.
13. **Stitch MCP** for UI ideation; Compose for production; anti-slop skill required.
14. **Strix** ([repo](https://github.com/usestrix/strix)) = security QA subset on staging only.
15. **Budget production:** free tiers + single box; MapLibre-first; workshop web MVP; geohash exploration v1 (DEC-007/008/014/015).
16. **Max gamify** non-Drive surfaces; Drive stays calm (DEC-009).

### Definition of roles for “agents”
| Role | Owns |
|------|------|
| Design Agent | Stitch MCP screens, tokens, handoff notes |
| Android Agent | Compose UI, ViewModels, Room, services, maps |
| Backend Agent | NestJS modules, workers, PostGIS, Redis |
| DB Agent | Migrations, indexes, RLS policies if used |
| QA Agent | Test plans, fixtures, device matrix, car tests |
| Security Agent | Strix staging passes (subset of QA) |
| DevOps Agent | CI, secrets, staging, monitoring |

---

# 1b. Tooling & Budget Overlay (2026-09-15)

| Concern | Choice |
|---------|--------|
| Efficient code | Ponytail always-on rule |
| Fresh docs | Context7 MCP (`YOUR_CONTEXT7_API_KEY` in `.cursor/mcp.json`) |
| UI design | Stitch MCP already configured |
| Security QA slice | Strix on staging milestones |
| Maps | MapLibre + OSM first (cost) |
| Workshops UI | Web portal MVP, not second native app |
| Exploration | Geohash cells v1 |
| Hosting | Neon/Supabase free → one cheap VPS/PaaS; Redis cheap; R2 objects; Firebase Spark if fits |
| Secrets | `.cursor/mcp.json` gitignored; use `mcp.json.example` |

**Conflict rule:** Ponytail may strip boilerplate; it must **not** strip requested gamification/motion on post-drive product surfaces.

---

# 2. Product Vision & Customer Attraction

## 2.1 What the user feels
Not: “I’m opening a GPS speedometer.”  
Yes: “I’m going on a drive — and my car’s whole life lives here.”

## 2.2 Core retention loop
```
Before: challenge / rank / next service due
During: minimal live speed + map + discoveries (subtle)
After: score, cinematic, XP, rank delta, vault update
Service: book workshop → bill → certified history
Again: START DRIVE
```

## 2.3 Why customers install (hooks)
| Hook | Stage | Emotion |
|------|-------|---------|
| Beautiful live speed HUD | S1 | Instant utility |
| Trip replay + speed graph | S1 | “My drive was an experience” |
| City seasonal leaderboard | S2 | Local rivalry |
| Road discovery % | S2 | Completionism |
| Ghost driver (quality, not recklessness) | S2 | Self-improvement |
| One-tap car timeline | S3 | Ownership confidence |
| Book Audi specialist with history share | S4 | Trust + convenience |
| Invoice auto-saves to vault | S4 | “Never lose service records” |

## 2.4 Competitive positioning
- **vs speedometer apps:** adventure + vault + marketplace  
- **vs Strava:** driving-safe UX + vehicle maintenance graph  
- **vs workshop apps alone:** driving identity + exploration game  
- **vs OEM apps:** multi-brand, fun, social, workshop-agnostic  

## 2.5 Safety brand promise
Adventure lives **before/after** driving. Live mode is calm, large type, low interaction. This is both ethics and store-policy risk reduction.

---

# 3. Recommended Design Tooling

## 3.1 Verdict (best fit for AutoNexar)

| Layer | Tool | Why it fits AutoNexar |
|-------|------|------------------------|
| Rapid UI ideation | **Google Stitch** ([stitch.withgoogle.com](https://stitch.withgoogle.com/)) | Fast multi-screen exploration for Drive HUD, Trip Cinematic, Garage Timeline, Workshop Profile — thematic dark adventure UIs from prompts |
| Agent ↔ Stitch | **stitch-bridge MCP** or **stitch-pro-mcp** | Agents generate/edit screens, fetch HTML/screenshots, iterate without leaving Cursor |
| Design system source of truth | **Figma** | Tokens, components, variants; Paste from Stitch → refine |
| Design → Android code | **Figma Desktop MCP** + **figma-to-compose skill** / Code Connect | Translate frames to Jetpack Compose; extract variables → `Color`/`Typography`/`Dimens` |
| Production UI code | **Jetpack Compose + custom DesignSystem** | Stitch HTML is **not** production Android; always re-implement in Compose |
| Maps | Google Maps Compose / Mapbox | Live route, adventure map, workshop pins |
| Motion | Compose Animation + Lottie (achievements/cinematics) | Glow, route draw, rank-up moments |

### Recommended agent design workflow
```
1. Prompt Stitch (MOBILE) with AutoNexar theme tokens
2. Generate variants for Drive Minimal / Detailed / Performance / HUD
3. Paste winners → Figma
4. Build Figma component library (buttons, meters, rank rows, service cards)
5. Figma MCP → Compose scaffolding
6. Android agent hardens: state, accessibility, dark night driving contrast, 48dp targets
```

### Why Stitch suits *this* thematic
AutoNexar needs many **high-drama screens** (cinematic trip, season podium, road discovered, garage timeline) and many **calm utility screens** (booking, invoice, documents). Stitch excels at generating both families quickly; Figma locks the system so Compose stays consistent.

### What NOT to do
- Do not ship Stitch HTML/CSS inside Android WebViews as the main UI.
- Do not let Stitch invent a purple-default Material look — constrain prompts to AutoNexar tokens (see §7).
- Do not clutter Drive screen with marketplace widgets from Stitch mockups.

### Prompt seed for Stitch (use / adapt)
```
Mobile Android driving companion, dark night dashboard aesthetic,
deep charcoal (#0B0F14) background, electric teal (#2EE6A6) accents,
amber warning (#F5A524), large monospace-like speed numerals,
minimal chrome, adventure map glow, not purple gradient, not cream editorial.
Screen: live drive HUD with huge km/h, small avg/max, thin route map strip.
Safe for night driving, high contrast, sparse controls.
```

---

# 4. Technology Stack

## 4.1 Android (Frontend)
| Area | Choice | Notes |
|------|--------|-------|
| Language | Kotlin | |
| UI | Jetpack Compose + Material3 customized | Custom theme overrides MD3 |
| Architecture | Clean + MVVM | UI → VM → UseCase → Repository → DataSource |
| DI | Hilt | |
| Navigation | Navigation Compose | Typed routes |
| Local DB | Room | Trips, vault, pending sync |
| Preferences | DataStore | Units, HUD mode, privacy |
| Async | Coroutines + Flow | |
| Images | Coil | |
| Maps | **MapLibre** (Compose) first; Google Maps only if budget/UX forces | DEC-008 |
| Location | Fused Location Provider + Foreground Service | |
| Sensors | Accelerometer, Gyro, Rotation Vector | Calibration required |
| Auth client | Credential Manager + Google Sign-In; email/password | Phone optional later |
| Networking | Retrofit + OkHttp + Kotlin Serialization | |
| Sync | WorkManager | Idempotent uploads |
| Crash | Firebase Crashlytics | |
| Analytics | Firebase Analytics (privacy-minimized) | |
| Push | FCM | |
| Payments (later) | Play Billing (extra cars / premium) | |
| Testing | JUnit, Truth, Turbine, Compose UI Test, Espresso | |

## 4.2 Backend (API Platform)
**Recommended primary stack (marketplace + telematics + admin) — budget-aware:**
| Area | Choice | Why |
|------|--------|-----|
| API | **NestJS (TypeScript) + Fastify adapter** | Strong modules; one deployable |
| DB | **PostgreSQL** (Neon/Supabase free → paid) + **PostGIS when needed** | Start without PostGIS if only lat/lon + geohash |
| Cache / locks / jobs | **Redis + BullMQ** (Upstash free or same-VPS Redis) | No Kafka in MVP |
| Object storage | **Cloudflare R2** / Backblaze | Cheap egress |
| Auth | JWT + Google | |
| Workshop UI MVP | **Next.js web** on same API | DEC-014 — cheaper than 2nd native app |
| Admin | Nest static or thin Next | |
| Crash/Push | Firebase Spark while free limits OK | |

**Alternative (Kotlin-everywhere):** Ktor — only if team refuses TypeScript. Default remains NestJS.

## 4.3 Infra
- Docker Compose for local  
- GitHub Actions CI  
- Staging + Production  
- Cloudflare or AWS ALB  
- Secrets via environment / secret manager  
- OpenTelemetry + Prometheus/Grafana later  

---

# 5. System Architecture

```
┌──────────────────────────── ANDROID APP ────────────────────────────┐
│  UI (Compose)  │  ViewModels  │  UseCases  │  Repositories         │
│       ▲                │                                             │
│       │         DRIVING ENGINE (Foreground Service)                  │
│       │           GPS + Sensors + Session State                      │
│       ▼                                                              │
│  Room (offline)  ←→  Sync Engine  ←→  API                            │
└──────────────────────────────────┬───────────────────────────────────┘
                                   │ HTTPS
┌──────────────────────────────────▼───────────────────────────────────┐
│ API GATEWAY / NestJS                                                 │
│ Auth │ Users │ Trips │ Scores │ Geo │ Garage │ Workshops │ Bookings  │
│ Invoices │ Social │ Challenges │ Admin │ Feature Flags               │
└─────┬───────────────┬───────────────┬───────────────┬────────────────┘
      ▼               ▼               ▼               ▼
  PostgreSQL+     Redis          BullMQ Workers    Object Storage
  PostGIS         caches         trip/score/pdf    docs/media
```

### Critical boundaries
- **Telemetry collection** stays on device during drive.
- **Competitive scoring / integrity / ranks** finalize on server.
- **Invoice → Vehicle History writeback** is server-mediated + owner-confirmed.
- **Leaderboards** are precomputed indexes, not live full-table sorts on open.

---

# 6. Repository & Module Layout

```
auto-nexar/
├── docs/                          # This bible + ADRs
├── android/
│   ├── app/
│   ├── core/common/
│   ├── core/designsystem/
│   ├── core/database/             # Room
│   ├── core/network/
│   ├── core/datastore/
│   ├── feature/auth/
│   ├── feature/home/
│   ├── feature/drive/
│   ├── feature/trips/
│   ├── feature/map/
│   ├── feature/profile/
│   ├── feature/garage/            # S3
│   ├── feature/workshops/         # S4 customer side
│   ├── feature/leaderboards/      # S2
│   ├── feature/challenges/
│   ├── engine/telemetry/          # Pure driving engine
│   ├── engine/scoring/            # Local provisional scores only
│   └── engine/sync/
├── backend/
│   ├── apps/api/
│   ├── apps/worker/
│   ├── apps/admin-web/
│   └── packages/shared-dto/
├── design/
│   ├── stitch-prompts/
│   └── figma-tokens-export/
└── qa/
    ├── test-plans/
    ├── fixtures/
    └── car-test-protocols/
```

---

# 7. Design System & Theming

## 7.1 Visual pillars
1. **Adventure night dashboard** (not generic Google Maps clone)
2. **Large expressive numerals** for speed / score / rank
3. **Teal energy + amber caution + red critical** (never purple-default AI look)
4. **Motion with purpose:** route draw, XP fill, rank arrow, discovery pulse
5. **Drive mode sparsity** vs **post-drive richness**

## 7.2 Token starter
```
--bg-void:        #0B0F14
--bg-panel:       #121821
--bg-elevated:    #1A2330
--accent-teal:    #2EE6A6
--accent-cyan:    #3DB9FF
--warn-amber:     #F5A524
--danger-red:     #FF4D4F
--text-primary:   #F4F7FB
--text-muted:     #8B97A8
--success:        #3DDC97
--rank-gold:      #FFC857
--explore-glow:   #7CFFB2
```

Typography:
- Display/speed: distinctive condensed or tabular figures (e.g. *Space Grotesk* / *IBM Plex Sans* / custom — avoid Inter/Roboto as hero)
- Body: readable sans for forms (booking/invoice)

## 7.3 Dashboard modes (Drive)
| Mode | Shows |
|------|-------|
| Minimal | Huge speed only + end button |
| Detailed | Speed, avg, max, distance, duration |
| Performance | + G-force, accel, altitude |
| HUD | Mirrored large speed for windshield mount |

## 7.4 Navigation (Driver app)
Bottom nav (post-auth):
`Home | Drive | Trips | Garage | More`

`More` contains: Map/Adventure, Leaderboards, Challenges, Workshops, Profile, Settings.

During active drive: full-screen Drive UI; nav hidden or locked.

---

# 8. Complete Screen Inventory

> Agents must implement screens as listed. Each bullet under a screen is a **required UI element or state** unless marked Optional/Later.

## 8.0 Global / System
1. **Splash** — logo, version, boot sync indicator  
2. **Force Update** — store link  
3. **Maintenance Mode** — message  
4. **No Network Banner** — offline capable notice  
5. **Permission Primer** — why location/sensors/notifications  
6. **Permission Denied Recovery** — settings deep link  
7. **Error Bottom Sheet** — retry/report  
8. **Session Expired** — re-auth  

## 8.1 Auth & Onboarding
9. **Welcome** — value props (Drive / Explore / Garage / Service)  
10. **Sign Up Email** — email, password, username  
11. **Sign In Email**  
12. **Google Sign-In**  
13. **Phone Auth** (Optional/Later)  
14. **Verify Email**  
15. **Forgot Password**  
16. **Reset Password**  
17. **Username Setup** — uniqueness check  
18. **Profile Basics** — display name, avatar, country/province/city (home region)  
19. **Units Preference** — km/h|mph, km|mi  
20. **Privacy Defaults** — public/friends/private profile; leaderboard opt-in  
21. **Phone Calibration Intro**  
22. **Phone Calibration Capture** — place phone, detect orientation baseline  
23. **Calibration Success / Recalibrate**  
24. **Feature Tour** (skippable)  
25. **First Vehicle Prompt** — add car or skip (S3)  

## 8.2 Home
26. **Home Dashboard**
   - Greeting + driver title
   - Level + XP bar
   - Adventure Score
   - Driver Rating
   - Rank chips: City / Province / Country / Global (S2)
   - Streak
   - Next challenge CTA
   - Recent trip card
   - Next maintenance due (S3)
   - Primary CTA: Start Drive
   - Secondary: Book Service (S4)

## 8.3 Drive (Live)
27. **Drive Ready** — select vehicle, dashboard mode, calibrate reminder, Start  
28. **Drive Live Minimal**  
29. **Drive Live Detailed**  
30. **Drive Live Performance**  
31. **Drive HUD**  
32. **Drive Map Expanded** — route polyline, start pin, bearing  
33. **Drive Pause / Confirm End**  
34. **Weak GPS Overlay** — degraded accuracy notice  
35. **Permission Lost Mid-Drive** — safe stop guidance  
36. **Driving Mode Lock** — blocks social/leaderboard  

Live widgets (compose into modes): current speed, avg, max, distance, duration, altitude, g-force meter, event toast (new road — subtle), battery warning.

## 8.4 Trip Processing & Experience
37. **Processing Trip** — progress steps (route, events, metrics, provisional score)  
38. **Trip Summary** — distance, duration, avg/max, ascent, stops, quality, exploration XP, View Replay  
39. **Trip Timeline** — event list  
40. **Speed-Time Graph** — scrubber, point tooltip  
41. **Elevation Graph**  
42. **Trip Replay** — marker along route synced to graphs/events  
43. **Trip Cinematic** (S2) — auto highlight reel  
44. **Trip Share Card Generator** — privacy-safe card  
45. **Trip Detail** — full stats, integrity badge (user-facing soft), vehicle tag  
46. **Trip Privacy Controls** — private / share / hide exact route  

## 8.5 Trips History
47. **Trips List** — filters: date, vehicle, distance, score  
48. **Trips Empty State**  
49. **Trip Search**  
50. **Bulk Export** (Later)  

## 8.6 Adventure Map
51. **Adventure Map** — explored vs unexplored, discoveries  
52. **City Exploration %**  
53. **Region Browser** — city/province/country  
54. **Discovery Detail** — road/area unlocked  

## 8.7 Leaderboards (S2)
55. **Leaderboard Hub** — City/Province/Country/Global tabs  
56. **Leaderboard List** — rank, avatar, score, level, rating, weekly delta  
57. **You Rank Sticky Row**  
58. **Period Selector** — Today / Week / Month / Season / All-time  
59. **Filter Individuals vs Cities vs Provinces**  
60. **Friends Leaderboard**  
61. **Season Hub** — theme, countdown, your ranks  
62. **Season Results / Badges**  
63. **City Battle Board**  
64. **Country Battle Board** (Later)  

## 8.8 Challenges, Achievements, XP
65. **Challenges Home** — Daily/Weekly/Monthly/Seasonal  
66. **Challenge Detail** — progress, rewards, CTA Start Drive  
67. **Achievements Grid** — rarity colors  
68. **Achievement Detail** — progress  
69. **Level Up Modal**  
70. **Title Unlocked Modal**  
71. **Personal Records**  
72. **Stats Dashboard** — totals, streaks, cities, roads  

## 8.9 Profile & Social
73. **Own Profile**  
74. **Public Profile** — respect privacy  
75. **Edit Profile**  
76. **Followers / Following**  
77. **Friend Compare**  
78. **Block / Report**  
79. **Privacy Settings**  
80. **Notification Settings**  
81. **Settings Hub**  
82. **Units & Dashboard Modes**  
83. **Calibration Settings**  
84. **Connected Accounts**  
85. **Delete Account**  
86. **About / Licenses**  

## 8.10 Garage / Vehicle Vault (S3)
87. **Garage List** — cars, active car, lock icon for paid slots  
88. **Add Vehicle Wizard** — make/model/year/variant/fuel/trans  
89. **Vehicle Identity Editor** — VIN (masked), plate (private), color, nickname, photo  
90. **Ownership Events** — purchase, registration, insurance, sold  
91. **Documents Vault** — upload, expiry badges  
92. **Document Viewer**  
93. **Service History Timeline (One-Tap History)** — unified chronology  
94. **Add Manual Service** — taxonomy picker, parts, costs, odometer, attachments  
95. **Service Detail** — lines, parts, warranty, invoice link  
96. **Parts History**  
97. **Cost Analytics** — lifetime, /km, categories  
98. **Maintenance Schedule** — due items  
99. **Vehicle ↔ Trips** — linked drives  
100. **Export History PDF**  
101. **Share History Scope Picker** (for workshops)  
102. **Paywall: Extra Vehicle Slot** — Play Billing  
103. **Archive / Sold Vehicle**  

## 8.11 Workshops Marketplace — Customer (S4)
104. **Workshops Home** — search, near me  
105. **Workshop Filters** — brand fit, service, open now, rating, distance, authorized  
106. **Workshop Map**  
107. **Workshop Profile** — brands, services, parts, hours, photos, badges  
108. **Service Menu**  
109. **Parts Menu**  
110. **Slot Picker** — calendar + times  
111. **Booking Create** — vehicle, services, symptoms, photos, history share scope  
112. **Booking Confirmation**  
113. **My Bookings List**  
114. **Booking Detail / Job Tracker** — status timeline  
115. **Extra Work Approval** — approve/decline  
116. **Invoice Review** — line items  
117. **Confirm History Writeback**  
118. **Payment Recorded** (cash/card at shop Phase A)  
119. **Leave Review**  
120. **Dispute Invoice**  

## 8.12 Workshop Mode / Portal (S4)
> Prefer separate Workshop Android app or role switch + web portal. Screens below for workshop users.

121. **Workshop Sign Up / KYC**  
122. **Workshop Onboarding** — brands, hours, bays  
123. **Workshop Dashboard** — today’s jobs, requests  
124. **Services Catalog Manager**  
125. **Parts Catalog Manager**  
126. **Calendar / Slot Manager** — blockouts, buffers  
127. **Booking Request Detail** — accept/decline  
128. **Check-In** — scan/select vehicle, view shared history  
129. **Shared Vehicle History Viewer**  
130. **Job Workspace** — findings, photos, ETA  
131. **Request Extra Approval**  
132. **Invoice Builder** — add labor/parts/fees/discounts  
133. **Issue Invoice**  
134. **Staff Management** — roles  
135. **Workshop Reviews**  
136. **Workshop Analytics**  
137. **Workshop Settings / Payout (Later)**  

## 8.13 Admin Web (Ops)
138. **Admin Login**  
139. **Users**  
140. **Trips / Integrity Queue**  
141. **Scores Config** — weights feature flags  
142. **Seasons Manager**  
143. **Challenges Manager**  
144. **Achievements Manager**  
145. **Geo Hierarchy Manager**  
146. **Workshop Verification Queue**  
147. **Disputes**  
148. **Reports / Moderation**  
149. **Feature Flags**  
150. **System Health**  

---

# 9. Frontend Feature Matrix (Android)

## 9.1 Cross-cutting
- Theming, i18n-ready strings (EN first; UR later)
- Dark-first (optional light for forms)
- Accessibility: content descriptions, 48dp targets, contrast for night
- Localization of numbers/units
- Deep links: trip, booking, challenge
- App shortcuts: Start Drive, Garage, Bookings

## 9.2 Engine features (non-UI)
| Feature | Requirements |
|---------|--------------|
| Session start/end | Trip UUID, vehicle_id, timestamps |
| Adaptive GPS sampling | Balance accuracy/battery; filter jumps |
| Distance accumulator | Haversine + outlier rejection |
| Speed stream | For UI + graph + events |
| Sensor pipeline | After calibration; longitudinal/lateral |
| Event detector | START/STOP/ACCEL/BRAKE/TURN/ELEVATION/NEW_ROAD… |
| Route polyline builder | Encode on process |
| Provisional metrics | Local only until server confirms |
| Foreground notification | “Drive in progress” |
| Battery policies | Stationary downclock; stop sensors on end |
| Integrity signals | Collect features; score on server |

## 9.3 Compose component library (build these once)
- `SpeedHero`, `MetricTile`, `GForceGauge`, `XpBar`, `RankRow`, `DeltaChip`
- `TripCard`, `EventTimelineItem`, `GraphCanvas` (speed/elevation)
- `MapRoute`, `ExploreHeatOverlay`
- `VehicleCard`, `TimelineNode`, `DocumentTile`
- `WorkshopCard`, `SlotChip`, `InvoiceLineEditor`
- `PrimaryButton`, `DangerButton`, `SheetHeader`, `EmptyState`

---

# 10. Backend Feature Matrix

## 10.1 Service modules
1. **Auth** — register, login, refresh, Google OAuth, password reset, session revoke  
2. **Users** — profile, privacy, geo home region, device push tokens  
3. **Media** — signed upload URLs  
4. **Trips** — create meta, upload points/events (batched), processing status  
5. **Trip Processor Worker** — clean GPS, metrics, events validation  
6. **Integrity** — trip integrity score, eligibility gates  
7. **Scoring** — driving quality, exploration, consistency, activity; overall weighted  
8. **XP/Levels** — award, level-up, titles  
9. **Achievements** — progress evaluation engine  
10. **Challenges** — CRUD config, progress, completion  
11. **Exploration** — road segments, discoveries, city %  
12. **Leaderboards** — precompute ranks by geo × period × board type  
13. **Social** — follow, friends, block, report  
14. **Notifications** — fanout templates + preferences  
15. **Garage Vehicles** — CRUD, slot entitlements  
16. **Garage Services** — manual + certified entries  
17. **Documents** — metadata + storage keys  
18. **Maintenance Rules** — due calculation  
19. **Workshops** — profile, verification, catalogs  
20. **Inventory Parts** — workshop parts  
21. **Slots & Bookings** — hold/confirm/cancel/no-show  
22. **Jobs** — lifecycle, extras approval  
23. **Invoices** — issue, confirm, dispute, PDF  
24. **History Writeback** — invoice → service_record  
25. **Reviews** — job-gated  
26. **Billing Entitlements** — Play purchase verification for extra cars/premium  
27. **Admin** — configs, flags, KYC  
28. **Audit Log** — sensitive actions  

## 10.2 Example API surface (v1)
```
POST   /v1/auth/register
POST   /v1/auth/login
POST   /v1/auth/google
POST   /v1/auth/refresh
GET    /v1/me
PATCH  /v1/me
POST   /v1/me/push-token

POST   /v1/trips
POST   /v1/trips/{id}/locations:batch
POST   /v1/trips/{id}/events:batch
POST   /v1/trips/{id}/complete
GET    /v1/trips/{id}
GET    /v1/trips

GET    /v1/scores/me
GET    /v1/leaderboards/{geoType}/{geoId}
GET    /v1/seasons/current

GET    /v1/challenges
GET    /v1/achievements/me

GET/POST /v1/vehicles
GET      /v1/vehicles/{id}/timeline
POST     /v1/vehicles/{id}/services
POST     /v1/vehicles/{id}/documents
POST     /v1/vehicles/{id}/history-shares

GET    /v1/workshops
GET    /v1/workshops/{id}
GET    /v1/workshops/{id}/slots
POST   /v1/bookings
GET    /v1/bookings/{id}
POST   /v1/bookings/{id}/cancel
POST   /v1/jobs/{id}/check-in
POST   /v1/jobs/{id}/extras
POST   /v1/jobs/{id}/extras/{extraId}/decision
POST   /v1/invoices
POST   /v1/invoices/{id}/confirm
POST   /v1/invoices/{id}/dispute
POST   /v1/reviews

# Workshop staff
POST   /v1/workshop-staff/services
POST   /v1/workshop-staff/parts
POST   /v1/workshop-staff/slots/block
POST   /v1/workshop-staff/invoices

# Admin
CRUD   /v1/admin/*
```

## 10.3 Workers
- `trip.process`  
- `score.recompute`  
- `leaderboard.reindex`  
- `achievement.evaluate`  
- `challenge.tick`  
- `notification.dispatch`  
- `invoice.pdf`  
- `exploration.recompute`  
- `booking.reminders`  
- `integrity.batch`  

---

# 11. Database Schema

> Logical model. DB Agent produces migrations. Use UUIDs. Soft-delete where needed.

## 11.1 Identity & geo
- `users` (id, email, username, display_name, avatar_url, home_country_id, home_province_id, home_city_id, privacy, created_at)
- `countries`, `provinces`, `cities` (hierarchy + PostGIS bounds optional)
- `user_devices`, `push_tokens`
- `oauth_accounts`

## 11.2 Trips & telemetry
- `trips` (id, user_id, vehicle_id, start_at, end_at, distance_m, duration_s, avg/max speed, elev stats, quality, exploration_points, integrity, sync status, polyline, provisional flags)
- `trip_locations` (trip_id, ts, lat, lon, alt, speed, bearing, accuracy) — partition/hypertable later
- `trip_events` (type, severity, lat/lon, metadata jsonb)
- `trip_metrics` (jsonb structured metrics)

## 11.3 Scoring & game
- `score_weights_config` (versioned)
- `user_scores` (overall, components, period keys)
- `driver_ratings`
- `xp_ledger`, `user_levels`, `titles`
- `achievements`, `user_achievements`
- `challenges`, `user_challenge_progress`
- `seasons`, `season_scores`
- `leaderboard_entries` (board_type, geo_id, period, user_id, rank, score, delta)
- `personal_records`
- `explored_segments` (geometry, geo refs, user_id)
- `follows`, `blocks`, `reports`

## 11.4 Garage
- `vehicles`
- `vehicle_ownership_events`
- `vehicle_documents`
- `service_types` (global taxonomy)
- `service_records`
- `service_lines`
- `part_records`
- `maintenance_rules`
- `vehicle_entitlements` (max_vehicles from purchases)
- `history_shares`

## 11.5 Marketplace
- `workshops`
- `workshop_brands`
- `workshop_services`
- `workshop_parts`
- `workshop_hours`, `workshop_bays`
- `staff_users`, `staff_roles`
- `slots`
- `bookings`
- `jobs`
- `job_extras`
- `invoices`, `invoice_lines`
- `reviews`
- `disputes`
- `workshop_verifications`

## 11.6 Room (Android mirror — subset)
Mirror: user profile cache, vehicles, trips (+ locations/events for active/recent), pending_sync queue, challenges cache, bookings cache, feature flags.

---

# 12. S1–S4 Domain Specs

## 12.1 S1 — Driving Engine Done When
Install → account → permissions → calibrate → start → live speed/map/distance/route/sensors/events → end → process → score foundation → graphs → replay → summary → stats → sync.

## 12.2 S2 — Gamification Done When
Configurable multi-score + driver rating; XP/levels/titles; achievements; challenges; adventure map discoveries; 4-level seasonal leaderboards; friends board; ghost driver quality compare; anti-cheat integrity gates; server-side final scores; notifications with controls; safety Driving Mode.

## 12.3 S3 — Vault Done When
Add car; identity/ownership/docs; manual service+parts+cost; one-tap timeline; drive link; maintenance reminders; 2nd car gated by payment; export; offline manual entry sync.

## 12.4 S4 — Marketplace Done When
Workshop signup + brands/services/parts/hours; discovery by brand fit; book slot; share history; check-in; extras approval; invoice; owner confirm → certified vault entry; review; disputes basic.

### Scoring weights (default, backend-configurable)
| Component | Weight |
|-----------|--------|
| Driving Quality | 30% |
| Exploration | 20% |
| Consistency | 15% |
| Challenges | 15% |
| Activity | 10% |
| Achievements | 10% |

Integrity: ≥90 full rank; 70–89 limited; <70 ineligible.

---

# 13. Build Roadmap 0–100

## Band 0–10 — Foundation
0. Monorepo bootstrap, CI empty green  
1. Android app shell + navigation + design tokens  
2. Backend hello + health + OpenAPI  
3. Postgres + Redis docker  
4. Auth email + Google (dev)  
5. Room + DataStore skeleton  
6. Permission flows  
7. Design: Stitch→Figma core 10 screens  
8. Observability stubs (crash)  
9. Feature flag client  
10. QA harness smoke  

## Band 11–30 — S1 Engine MVP
11. Foreground location service  
12. GPS sampling + jump filter  
13. Speed/distance/duration  
14. Drive UI Minimal/Detailed  
15. Map polyline live  
16. End drive + local trip persist  
17. Trip processor local  
18. Speed graph + summary  
19. Replay v1  
20. Sensor calibration + G-force  
21. Accel/brake/turn events  
22. Elevation metrics  
23. Timeline UI  
24. Profile stats local  
25. Trip sync API + worker  
26. Offline pending sync  
27. Battery policy v1  
28. Personal records local  
29. Exploration data model stub  
30. Real-car test protocol #1  

## Band 31–50 — S2 Core Loop
31. Server scoring + weights config  
32. Driver rating separate  
33. XP/levels/titles  
34. Achievements engine  
35. Challenges engine  
36. Season object  
37. City leaderboard precompute  
38. Province/country/global boards  
39. Period selectors  
40. Home dashboard ranks  
41. Adventure map v1  
42. Road discovery awards  
43. Notifications FCM  
44. Friends + friend board  
45. Share cards  
46. Ghost driver v1  
47. Integrity scoring  
48. Anti-farming diminishing returns  
49. Admin score/season/challenge screens  
50. Closed beta city launch prep  

## Band 51–70 — S3 Vault
51. Garage list + add vehicle  
52. Identity + ownership events  
53. Documents uploads  
54. Service taxonomy seed  
55. Manual service logging  
56. Parts + costs  
57. One-tap timeline  
58. Maintenance due engine  
59. Trip↔vehicle linking  
60. Cost analytics  
61. Extra vehicle Play Billing  
62. Export PDF  
63. History share primitive  
64. Garage offline sync  
65–70. Hardening + UX polish  

## Band 71–90 — S4 Marketplace
71. Workshop entity + KYC  
72. Brand specialization  
73. Services/parts catalogs  
74. Hours + slots engine  
75. Customer discovery/ranking  
76. Booking flow  
77. Job lifecycle + check-in  
78. Shared history viewer  
79. Extra work approval  
80. Invoice builder + PDF  
81. Confirm → vault writeback  
82. Reviews  
83. Disputes  
84. Workshop staff RBAC  
85. Booking reminders  
86. Workshop analytics  
87. Featured listing flags  
88. Fraud basics  
89. City workshop pilot  
90. E2E marketplace QA gate  

## Band 91–100 — Production
91. Security audit  
92. Privacy review (Play Data Safety)  
93. Performance/battery pass  
94. Anti-cheat pass  
95. Store listing + screenshots (from Figma/Stitch)  
96. Staged rollout  
97. Monitoring SLOs  
98. Support playbooks  
99. Soft launch one city  
100. Expand country → global boards  

---

# 14. Team Swimlanes (Step-by-step collaboration)

## 14.1 Per feature template
```
1. Design Agent: Stitch variants → Figma final → tokens
2. DB Agent: migration + indexes
3. Backend Agent: API + worker + tests
4. Android Agent: UI + VM + Room + integration
5. QA Agent: test plan + automate + device/car run
6. Docs: update bible checklist
```

## 14.2 Frontend sequence (Android Agent)
1. Add navigation route  
2. Build state model (UiState)  
3. Wire ViewModel ↔ use cases  
4. Implement Compose using designsystem components  
5. Handle loading/empty/error/offline  
6. Add analytics events (minimal)  
7. Write Compose tests for critical interactions  
8. Manual dark-night readability check for Drive screens  

## 14.3 Backend sequence
1. DTO + OpenAPI  
2. Module service + repo  
3. AuthZ guards  
4. Validation + idempotency keys  
5. Worker if async  
6. Integration tests  
7. Seed data for geo/taxonomy  

## 14.4 DB sequence
1. Draft ER for feature  
2. Migration up/down  
3. Indexes for list/rank/geo queries  
4. Explain analyze on hot paths  
5. Backfill scripts if needed  

## 14.5 QA sequence
1. Acceptance criteria from this bible  
2. Happy path  
3. Permission denial / offline / weak GPS  
4. Fraud/abuse cases (S2/S4)  
5. Regression suite gate before merge  

---

# 15. QA Strategy

## 15.1 Test types
| Layer | Tools | Focus |
|-------|-------|-------|
| Unit | JUnit / Jest | Filters, scoring pure fns, slot conflicts |
| Flow | Turbine | ViewModel states |
| Compose UI | Compose Test | Buttons, forms |
| API | Supertest | AuthZ, idempotency |
| Contract | OpenAPI diff | Breaking changes |
| Instrumented | Emulator | Permissions, Room |
| Real car | Protocol sheets | GPS tunnels, mounts, battery |
| Marketplace E2E | Two accounts | Book→bill→vault |
| **Security subset** | **Strix** (staging) | IDOR, auth bypass, injection on API — [usestrix/strix](https://github.com/usestrix/strix) |
| Load | k6 (later) | Trip upload, leaderboard read |
| Privacy | Checklist | Play Data Safety |

## 15.2 Device matrix
- Android 10–14+  
- Low RAM devices  
- Different sensor phones  
- Landscape HUD  
- Battery saver / data saver modes  

## 15.3 Car test protocol (minimum)
Open sky; dense urban; tunnel; highway; stop-go; hills; 30/60/180 min; dashboard vs windshield mount; offline mountain drive → later sync; multi-car switch.

## 15.4 Marketplace protocol
Brand specialist match; general fallback; slot hold expiry; mid-job extra approve/deny; invoice dispute; history scope expiry; no writeback without confirm.

## 15.5 Exit criteria examples
- Crash-free rate target set pre-launch  
- Trip sync success ≥ 99% after retries  
- Booking double-book impossible under concurrency test  
- Drive UI interaction limited / Driving Mode enforced  

---

# 16. Security, Privacy, Compliance

- Encrypt sensitive fields (VIN) at rest  
- Signed URLs for docs; expire history shares  
- Rate limit auth, booking, upload  
- Server authority for scores/invoices/ranks  
- Privacy: private trips default option; no public exact home  
- Play Store Data Safety form aligned to actual SDK usage  
- Workshop KYC before trust badges  
- Audit admin and invoice overrides  
- GDPR/account deletion: cascade/anonymize policy documented  

---

# 17. Observability

Track technical: crashes, GPS failures, process failures, sync failures, battery impact estimates, booking errors.  
Track product: install→first drive, drives/user/week, challenge participation, rank views, bookings completed, vault writebacks.  
Do **not** collect unnecessary PII.

---

# 18. Monetization & Feature Flags

**Free:** 1 vehicle, core drive, basic stats, basic boards, basic achievements.  
**Paid:** extra vehicles, advanced analytics/cinematic, premium map themes, extended history.  
**Workshop (later):** featured listing, subscription.  
Flags: `s2_leaderboards`, `s3_garage`, `s4_marketplace`, `ghost_driver`, `cinematic`, `phone_auth`.

---

# 19. Launch Strategy

1. Internal dogfood  
2. Closed beta (hundreds) — engine + integrity  
3. One-city launch (boards + few workshops)  
4. Country expand  
5. Global boards when geo infra stable  

---

# 20. Agent Task Checklists (Copy/Paste)

## 20.1 “Implement Drive Live Detailed”
- [ ] Read §8.3, §7.3, §9.2  
- [ ] Confirm design tokens from Figma  
- [ ] Bind flows from TelemetryRepository  
- [ ] No navigation to social while `session.active`  
- [ ] Foreground service notification present  
- [ ] Unit test distance filter  
- [ ] Compose test Start/End  
- [ ] QA: weak GPS overlay  

## 20.2 “Implement One-Tap Vehicle Timeline”
- [ ] Merge ownership + services + docs + linked trips  
- [ ] Filters + search  
- [ ] Offline cache  
- [ ] Privacy masking VIN/plate  
- [ ] Performance with 200+ events  

## 20.3 “Implement Booking → Invoice → Vault”
- [ ] Slot hold race-safe (Redis lock)  
- [ ] History share scope  
- [ ] Extra approval  
- [ ] Invoice lines map to service taxonomy  
- [ ] Owner confirm required  
- [ ] Certified flag on service_record  
- [ ] E2E QA script green  

---

# 21. Appendix

## 21.1 TripEvent types
`START, STOP, ACCELERATION, BRAKING, TURN, ELEVATION_CHANGE, SPEED_CHANGE, NEW_ROAD, DESTINATION, GPS_DEGRADED, CALIBRATION_WARNING`

## 21.2 Service taxonomy (seed minimum)
oil_change, oil_filter, air_filter, cabin_filter, brake_pads, brake_discs, battery_replacement, tire_rotation, tire_replacement, wheel_alignment, suspension_repair, ac_service, spark_plugs, coolant_flush, transmission_service, diagnostic_scan, body_repair, detailing, inspection, custom

## 21.3 Driver titles (example)
1–5 Beginner; 6–10 Road Runner; 11–20 Explorer; 21–30 Adventurer; 31–50 Road Master; 51+ Legend

## 21.4 Notification examples (opt-in)
Rank up; XP to next level; roads to exploration %; weekly challenge; friend passed you; booking reminder; invoice ready; insurance expiry (garage)

## 21.5 Anti-goals
- Speedrun leaderboards by top speed  
- Distracting live social during drive  
- Silent extra workshop charges  
- Public exact home pins  
- Hard-coded score weights in APK  

---

# END OF BUILD BIBLE

**Next human action options (no build until asked):**
1. Approve stack (NestJS vs Ktor)  
2. Approve app display name / package id  
3. Ask agents to generate Stitch prompts pack  
4. Say **“start building”** with a phase (e.g. Band 0–10)

*Document version: 1.1 — tooling (Ponytail/Context7/Stitch/Strix), budget production, max gamify, living docs triad.*
