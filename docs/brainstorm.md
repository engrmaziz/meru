# Meru — Brainstorm

> Parking lot for ideas. **Nothing here is committed** until promoted to `decisions.md` + `flow.md` / bible.  
> Tags: `[idea]` `[ready]` `[later]` `[budget]` `[gamify]` `[risk]` `[kill]`

---

## Operating principles (locked inspiration)
- Adventure before/after drive; calm during drive.
- Responsible driving > raw speed cult.
- Max game juice where safe; zero distraction mid-drive.
- Ship production on **minimum budget** (free tiers → one cheap VPS → scale only when metrics demand).
- Agents: Ponytail for code mass; Context7 for API truth; Stitch for UI; Strix for security slice.

---

## Max gamification backlog

### Post-drive juice `[ready]` `[gamify]`
- [ ] Trip complete “seal stamp” animation + haptic
- [ ] XP fountain into level bar
- [ ] Quality score count-up with color grade (never red-shame for trying)
- [ ] New roads: map cells light up one-by-one
- [ ] Streak flame grow/shrink
- [ ] Personal record confetti (longest / smoothest / most ascent) — not max speed podium

### Home as game hub `[ready]` `[gamify]`
- [ ] Rank delta arrow bounce when changed since last open
- [ ] Challenge card with progress ring + “+2 roads to go”
- [ ] Season countdown ribbon
- [ ] Daily quest strip (3 tiny tasks)
- [ ] “Rival nearby” soft card (friend passed you) — opt-in only

### Leaderboards `[ready]` `[gamify]`
- [ ] Podium top-3 with title flair
- [ ] YOU row sticky with pulse on improvement
- [ ] City crest / province banner seasonal skins
- [ ] Ghost silhouette compare on repeat routes (smoothness first)

### Garage as RPG inventory `[ready]` `[gamify]`
- [ ] Vehicle “trust level” from certified services count
- [ ] Service milestone badges (10 oil changes, first mountain trip linked)
- [ ] Timeline nodes with rarity frames when workshop-certified
- [ ] “Car health” meter derived from maintenance due (careful: not medical claims)

### Workshops `[later]` `[gamify]`
- [ ] Workshop reputation seasons
- [ ] “Verified fix” stamps in vault
- [ ] Loyalty stamps per workshop (careful with lock-in ethics)

### Explicit anti-gamify `[risk]`
- Mid-drive rank popups
- Speed challenge races on public roads
- Punitive public shaming for hard brakes
- Pay-to-win leaderboard boosts

---

## Animation system ideas `[ready]` `[gamify]`
- Shared `Motion` tokens: duration XS/S/M/L, easing emphasize, reduce-motion respect
- Lottie only for rare celebrations (level-up, season badge) — keep APK lean (`[budget]`)
- Compose `Animatable` / `SharedTransition` for trip → replay continuity
- Map route draw path effect on summary

---

## Budget / max savings ideas

### Infra `[ready]` `[budget]`
- Neon or Supabase free Postgres until paid needed
- Redis: Upstash free / local redis on same VPS
- One Fly.io / Railway / Hetzner CX22 instead of K8s
- Cloudflare R2 for docs/invoices (egress-friendly)
- Firebase Spark: Crashlytics + FCM while free limits OK
- No Kafka; BullMQ on Redis enough (`DEC-007`)
- MapLibre + OSM (`DEC-008`)
- Workshop = web first (`DEC-014`)
- Exploration geohash v1 (`DEC-015`)
- Self-host admin as same NestJS static + simple Next later

### Product scope cuts for MVP money `[ready]` `[budget]`
- Defer: country-vs-country, cinematic v2, phone auth, in-app workshop payments, ghost multi-ghost races
- Keep: drive loop, basic score/XP, city season board, 1-car vault, booking→invoice→writeback pilot in 1 city

### Agent cost savings `[ready]` `[budget]`
- Ponytail always on (`DEC-003`)
- Context7 only when coding against libs (don’t spam)
- Stitch for key screens only; reuse Compose components
- Strix on staging milestones, not every commit

---

## UI anti-slop checklist `[ready]`
Promoted visual rules (also in skills):
- No purple-on-white AI gradient default
- No cream editorial newspaper look
- No dashboard-of-everything on first viewport of Welcome
- Brand wordmark hero on Welcome
- Teal/cyan energy on void charcoal
- Large numerals; sparse Drive chrome
- Cards only when they hold an interaction

Stitch prompt always includes theme prefix from `ANNEX-E-DESIGN-TOOLING.md`.

---

## Tooling wishlist `[ready]`
| Tool | Role |
|------|------|
| [Ponytail](https://github.com/DietrichGebert/ponytail) | Efficient coding |
| [Context7](https://github.com/upstash/context7) | Up-to-date lib docs |
| [Strix](https://github.com/usestrix/strix) | Security QA slice |
| Stitch MCP | UI ideation |
| Project skills | UI + gamify + build-log |

---

## Open questions (need human later)
1. ~~Final public app name / package id?~~ → **Meru** / `app.meru.android` (DEC-016)
2. Launch city (Lahore assumed)? `[idea]`
3. Accept MapLibre vs insist Google Maps? (provisional MapLibre) `[ready]`
4. Context7 API key added to MCP? `[ready]` human action
5. Rotate Stitch key if leaked in git? `[risk]` human action

---

## Promotion log
| Date | Item | Promoted to |
|------|------|-------------|
| 2026-09-15 | S1–S4 staging | decisions DEC-001 |
| 2026-09-15 | Ponytail / Context7 / Strix / Stitch | DEC-003–006 |
| 2026-09-15 | Budget stack + MapLibre + workshop web | DEC-007,008,014,015 |
| 2026-09-15 | Max gamify non-drive | DEC-009 |
| 2026-09-15 | Living docs triad | DEC-012 |
