---
name: autonexar-gamify-motion
description: >-
  Maximize gamification and animation on AutoNexar non-driving surfaces (Home,
  trip summary/replay, XP, achievements, leaderboards, challenges, garage
  milestones, booking complete). Use when building those screens, celebrations,
  streaks, ranks, or when the user asks to gamify. Never add distracting game UI
  during an active drive.
---

# AutoNexar Gamify + Motion

## Where to MAX gamify
Home, Trip Summary, Replay, Level-up, Achievements, Challenges, Leaderboards, Season results, Garage timeline milestones, Invoice confirmed → vault stamp.

## Where to MINIMIZE
Live Drive (all modes), Driving Mode lock, permission/system errors.

## Required juice patterns
- XP bar fill + level-up burst
- Score count-up
- Rank delta ▲▼ chip animation
- Route polyline draw on summary
- Discovery cell light-up (post-drive)
- Streak flame
- Certified service “stamp” on vault writeback
- Respect system reduce-motion when available

## Anti-patterns
- Raw top-speed podium as primary competition
- Mid-drive rank toasts / challenge spam
- Pay-to-win board boosts
- Shame UX for hard braking

## Implementation notes
- Prefer Compose animation APIs; Lottie only for rare celebrations (APK budget).
- Hook rewards after server-finalized trip when possible; provisional UI OK with subtle “pending” state.
- Log gamify work in `docs/BUILD-LOG.md`.
