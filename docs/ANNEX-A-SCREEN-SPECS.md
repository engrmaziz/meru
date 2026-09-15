# Annex A — Screen-by-Screen Detailed Specs (Driver App)

> Companion to `AI-AGENT-BUILD-BIBLE.md` §8.  
> Each screen lists: purpose, entry points, UI structure, states, actions, data deps, analytics, QA notes.

---

## A.AUTH

### Welcome
- **Purpose:** Convert install → signup interest.
- **Structure:** Full-bleed dark adventure visual; brand wordmark hero; one headline; one supporting line; CTA group (Create account / Sign in / Continue with Google).
- **States:** Default; offline (Google may fail).
- **QA:** Brand visible without relying on nav; first viewport not a dashboard.

### Sign Up Email
- Fields: email, password, confirm password, username (or defer username).
- Validation: email format, password strength, username availability debounce.
- Errors: inline + snackbar; duplicate email.
- Success → Verify Email or Profile Basics.

### Sign In / Google / Forgot / Reset
- Standard auth patterns; show biometric unlock later (flag).

### Profile Basics
- Display name, avatar picker, home country/province/city (searchable).
- Copy: “Home region is for leaderboards — not your exact address.”

### Privacy Defaults
- Profile visibility: Public / Friends / Private.
- Show on leaderboard: toggle.
- Private trips default: toggle.

### Calibration Intro → Capture → Success
- Instructions with illustration (phone on dash).
- Capture 3–5 seconds of rotation vector baseline.
- Store calibration profile per device.
- Fail: retry; skip with warning (sensor metrics reduced).

---

## A.HOME

### Home Dashboard
- **Header:** avatar, greeting, title chip.
- **XP:** level number + bar + XP fraction.
- **Scores:** Adventure Score (large), Driver Rating (secondary).
- **Ranks:** 4 chips linking to boards (disabled/hidden if S2 flag off).
- **Cards:** streak, next challenge, recent trip, maintenance due.
- **CTA:** Start Drive (primary), Book Service (secondary if S4).
- **States:** loading skeletons; empty recent trip; offline cached ranks stamped “as of”.

---

## A.DRIVE

### Drive Ready
- Vehicle selector (required if garage has cars).
- Mode selector: Minimal / Detailed / Performance / HUD.
- Checklist: location permission, GPS warm, calibration age.
- Button: START DRIVE → starts foreground service.

### Drive Live (all modes)
- **Always:** speed hero, end drive (confirm), GPS health indicator.
- **Detailed:** avg, max, distance, duration.
- **Performance:** g-gauge, accel, altitude.
- **HUD:** mirrored speed option toggle.
- **Map strip / expanded:** polyline, bearing.
- **Toasts:** subtle discovery only; no rank spam.
- **Locks:** block navigation to More/Leaderboards while active (Driving Mode).
- **Notification:** ongoing “Drive in progress — tap to return”.

### Confirm End
- Warn if duration < N seconds (accidental).
- END → Processing.

---

## A.TRIPS

### Processing Trip
- Steps checklist with spinners: Validate GPS → Build route → Detect events → Metrics → Provisional score → Upload (if online).
- Failures: retry upload; keep local.

### Trip Summary
- Hero distance + duration.
- Grid: avg/max/ascent/stops.
- Quality score, exploration XP.
- Buttons: Replay, Timeline, Share, Done.
- Integrity: soft label only if poor (“Limited competitive eligibility”).

### Speed Graph / Elevation / Timeline / Replay / Cinematic
- Shared trip session ViewModel.
- Replay syncs map marker + graph cursor + event popups.
- Share card: no precise home; optional route obscuring.

### Trips List
- Infinite scroll; filters; vehicle chip; sync status icon (pending/synced/failed).

---

## A.MAP / LEADERBOARDS / GAME

### Adventure Map
- Base map + explored overlay.
- City % header.
- Tap discovery → detail bottom sheet.

### Leaderboard Hub & List
- Tabs geo levels; period selector; sticky YOU row; delta chip ▲▼.
- Empty: “Season just started”.
- Opt-out: CTA to privacy settings.

### Challenges / Achievements / Records / Stats
- Progress bars; rarity frames; deep link Start Drive with challenge context (metadata only).

---

## A.PROFILE / SETTINGS

### Own vs Public Profile
- Public hides private trips, VIN, plate, docs, exact routes.
- Follow button if allowed.

### Settings
- Units, dashboard default mode, calibration, notifications granular toggles, privacy, delete account.

---

## A.GARAGE (S3)

### Garage List
- Vehicle cards; ACTIVE badge; lock overlay on unpaid slots → paywall.
- FAB Add Vehicle.

### Add Vehicle Wizard
- Steps: Make → Model → Year → Variant → Powertrain → Nickname/Photo → Confirm.
- Optional: purchase date, reg date, odometer.

### One-Tap History (Vehicle Timeline)
- **Hero:** photo, name, make/model/year, odometer, next due.
- **Summary strip:** lifetime cost, visits, last service.
- **Timeline:** mixed event types with icons; filter chips (All / Service / Docs / Ownership / Drives).
- Tap node → detail.
- CTA: Add Service, Book Workshop, Export.

### Manual Service Form
- Date, odometer, workshop name (text) or workshop_id, reason.
- Multi-select service types + custom lines.
- Parts lines (name, brand, qty, price, warranty).
- Costs; attachments; next due.
- Save offline-capable.

### Documents / Costs / Maintenance / Export / Paywall
- Expiry badges red/amber.
- Cost charts simple (no card spam).
- Play Billing purchase → refresh entitlement.

---

## A.WORKSHOPS CUSTOMER (S4)

### Discovery
- Search; filter sheet; sort (best match default = brand fit + distance + rating).
- List + map toggle.

### Workshop Profile
- Header rating/jobs/distance/hours.
- Brand chips; services; parts; photos; badges.
- CTA View Slots.

### Booking Create
- Vehicle → services → slot → notes/photos → history share scope → confirm.
- Slot hold timer UI.

### Job Tracker
- Status stepper; chat/call optional later; extras approval cards; invoice entry.

### Invoice Review → Confirm Writeback
- Line items immutable after issue (dispute instead).
- Confirm copies certified service into vault.

---

## A.WORKSHOP STAFF

### Dashboard
- Today’s schedule; incoming requests; metrics.

### Catalog Managers
- CRUD services/parts; brand applicability; price type.

### Slot Manager
- Week grid; bay capacity; buffers; holidays.

### Check-In + History Viewer
- Read-only shared scope; countdown to share expiry.

### Invoice Builder
- Add anything done; map to taxonomy when possible; preview PDF; issue.

---
