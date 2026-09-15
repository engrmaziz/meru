# Meru — Decisions Log (ADR-style)

> **Rule for agents:** When a meaningful product/tech/budget choice is made or changed during planning or building, **append an entry here** (never silently override).  
> Format: `DEC-XXX` · date · status · context · decision · consequences · links.

---

## How to use
1. Before inventing a new stack/pattern, search this file.
2. After choosing something non-obvious, add `DEC-XXX`.
3. If reversing a decision, mark old one `Superseded by DEC-YYY` and add the new entry.
4. Keep entries short; deep rationale can live in `brainstorm.md`.

---

## DEC-021 — Phase 5 server progression on trip finalize
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** Competitive Quality / Exploration / Activity, Adventure Score, Driver Rating, XP, achievements, and challenge ticks are computed in `ProgressionService` when `POST /v1/trips` first succeeds. Client provisional values are UX-only; spoofed `qualityScore`/`explorationXp` do not raise awards. Weights served via `GET /v1/scores/config` (versioned). Integrity below threshold sets `competitiveEligible=false` (boards Phase 6).
- **Why:** DEC server authority + Phase 5 exit criteria (S2-01).
- **Consequences:** In-memory until Postgres `xp_ledger` / `user_scores`; Home caches via `ProgressionStore`.

## DEC-020 — Phase 4 trip sync = single idempotent upsert
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** Client posts one `POST /v1/trips` body (metrics + downsampled locations + events) keyed by `clientTripId`. Server dedupes per `userId:clientTripId`. Location/event batch endpoints from ANNEX-B deferred until volume requires them.
- **Why:** Ponytail for S1 Afterglow; offline queue is WorkManager + `pending_sync` payload JSON.
- **Consequences:** ANNEX-B batch paths remain the upgrade path; integrity is a stub (+2 from client quality) until a real process worker.

## DEC-019 — Commit & push after every phase
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** On completion of each `plan/N` phase, create a git commit and push to `origin` immediately. Author/committer must remain **engrmaziz** / Musharraf Aziz `<io@maziz.me>` with **no** Cursor co-author trailers (extends DEC-018).

## DEC-018 — GitHub commits as engrmaziz only
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** All commits/pushes must be authored as **engrmaziz** (`Musharraf Aziz` `<io@maziz.me>`). Never attribute Cursor/AI as co-author or contributor. No `Co-authored-by: Cursor` trailers. Local repo git config only (not global unless owner asks).
- **Rule file:** `.cursor/rules/git-author-engrmaziz.mdc`

## DEC-016 — Product name: Meru
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** Public product name is **Meru** (package `app.meru.android`). Former working name AutoNexar retired in docs/UI copy. Repo folder may remain `auto-nexar` until explicitly renamed.
- **Why:** Short, classy, modern, techy; ascent/adventure metaphor fits exploration + elevation without speed-cult branding. Avoids clash with major “Nexar” dashcam brand.
- **Tagline:** *Every drive becomes an ascent.*
- **Consequences:** All Stitch prompts, store listing, and Compose strings use Meru.

## DEC-017 — Ten sequential build phases
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** Build via `plan/1.md` … `plan/10.md` in order. Start only when human says `start phase N`.
- **Map:** 1–4 S1 · 5–6 S2 · 7 S3 · 8–9 S4 · 10 production.

## DEC-001 — Product stages S1→S4
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** Ship in four stages: S1 Driving Engine → S2 Gamification/Leaderboards → S3 Vehicle Vault → S4 Workshop Marketplace.
- **Why:** Engine + data first; social/competition second; car history third; B2B marketplace last (depends on vault).
- **Consequences:** Do not build S4 booking before S3 timeline writeback contract exists.

## DEC-002 — Android-first
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** Android (Kotlin + Jetpack Compose) only for now. No iOS until post-MVP.
- **Consequences:** Skip KMP unless explicitly requested later.

## DEC-003 — Agent efficiency: Ponytail
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** All coding agents follow [Ponytail](https://github.com/DietrichGebert/ponytail) ladder (YAGNI → reuse → stdlib → platform → dep → one line → minimum). Project ships `AGENTS.md` + `.cursor/rules/ponytail.mdc`.
- **Why:** ~fewer LOC/tokens/cost; prevents over-engineering on a budget.
- **Not lazy about:** trust-boundary validation, data-loss handling, security, a11y, sensor calibration, anything explicitly requested (incl. max gamification/animation on product surfaces).
- **Conflict resolution:** Ponytail wins on *infrastructure/boilerplate*. Product wins on *gamification/motion/UI polish* when explicitly in scope for that screen.

## DEC-004 — Docs freshness: Context7
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** Use [Context7](https://github.com/upstash/context7) MCP/CLI whenever implementing against libraries (Compose, Hilt, Room, NestJS, Postgres, Redis, Maps, Play Billing, etc.). Prefer library IDs in prompts.
- **Why:** Avoid hallucinated/outdated APIs.
- **Setup:** MCP `https://mcp.context7.com/mcp` + API key from context7.com/dashboard. Rule: always fetch docs for unfamiliar APIs before coding.

## DEC-005 — Security QA subset: Strix
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** [Strix](https://github.com/usestrix/strix) is **part of QA**, not whole QA. Use for API/auth/IDOR/injection pentest on staging backends and critical flows (bookings, invoices, vault shares). Keep unit/UI/real-car/marketplace E2E as primary functional QA.
- **Budget:** Prefer open-source local Strix + own LLM key on staging only; Cloud only if needed later.
- **Install for agents:** `npx skills add usestrix/strix` when security pass is requested.

## DEC-006 — UI design: Stitch MCP → Figma → Compose
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** Design flow = **Stitch MCP** (`.cursor/mcp.json`) for screen ideation → optional Figma refine → Jetpack Compose implementation. Never ship Stitch HTML as production Android UI.
- **Anti-slop:** Enforce AutoNexar tokens (teal/void adventure); ban purple-gradient AI defaults; Drive screens stay calm; post-drive/home/garage/boards get max motion + game juice.

## DEC-007 — Backend stack (budget production)
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** NestJS + PostgreSQL (+ PostGIS when needed) + Redis + BullMQ. Single modular monolith first. No Kafka/K8s at start.
- **Budget hosting target:** Neon/Supabase free Postgres → cheap single VPS or Fly/Railway when needed; Redis free/cheap tier; Cloudflare R2/Backblaze for objects; Firebase Spark for Crashlytics/FCM if free tier fits.
- **ponytail:** Mark scale shortcuts with `ponytail:` comments (e.g. geohash exploration before full OSM graph).

## DEC-008 — Maps cost control
- **Date:** 2026-09-15
- **Status:** Accepted (provisional)
- **Decision:** Prefer **MapLibre + OSM tiles** (or free-tier friendly provider) for MVP adventure/drive maps to avoid Google Maps bill shock. Re-evaluate Google Maps only if UX requires it and budget allows.
- **Consequences:** Design for MapLibre Compose wrapper; keep provider behind interface.

## DEC-009 — Gamification intensity
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** Maximize gamification + animation on **non-driving** surfaces (Home, Summary, Replay, Boards, Challenges, Achievements, Garage milestones, Booking complete). Live Drive Mode remains minimal/safe (no leaderboard browsing, no heavy celebration spam).
- **Never gamify:** raw top speed as competitive primary metric; mid-drive social feeds.

## DEC-010 — Extra cars = paid; core drive free
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** 1 vehicle free; additional vehicles via Play Billing. Do not paywall basic safe driving telemetry.

## DEC-011 — Server authority
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** Final scores, integrity, ranks, invoice finalization, vault certified writeback are server-side. Client may show provisional values.

## DEC-012 — Living docs triad
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** Maintain `docs/decisions.md`, `docs/flow.md`, `docs/brainstorm.md` continuously. While building, also append to `docs/BUILD-LOG.md`.
- **Why:** Agents and humans share one decision trail; brainstorm captures max-gamify ideas without polluting accepted architecture.

## DEC-013 — Secrets handling
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** Do not commit live API keys. Use `.cursor/mcp.json.example`; keep real `.cursor/mcp.json` local/gitignored when possible. Rotate any key that was pasted into chat/repo.
- **Action for human:** Rotate Stitch key if this repo is or will be public; put Context7 key via env/dashboard.

## DEC-014 — Workshop app delivery
- **Date:** 2026-09-15
- **Status:** Accepted (budget)
- **Decision:** Customer Android app first. Workshop side = **responsive web (Next.js) on same API** for MVP (cheaper than second native app). Native workshop app later if demand proves it.

## DEC-015 — Exploration v1 = geohash cells
- **Date:** 2026-09-15
- **Status:** Accepted
- **Decision:** Road discovery v1 uses geohash/cell exploration, not full OSM road graph. Upgrade path documented with `ponytail:` when implemented.
- **Why:** Huge cost/complexity savings; still supports “% explored” dopamine.

---

## Template for new entries

```
## DEC-XXX — Title
- **Date:** YYYY-MM-DD
- **Status:** Proposed | Accepted | Superseded by DEC-YYY | Rejected
- **Decision:** …
- **Why:** …
- **Consequences:** …
- **Links:** brainstorm / PR / issue
```
