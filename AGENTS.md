# Meru Agent Rules

This repo builds **Meru** (Android-first driving adventure + vehicle vault + workshops). Former name: AutoNexar.

## Before any work
1. Read `docs/README.md` then `docs/decisions.md`, `docs/flow.md`, `docs/brainstorm.md`.
2. Build only when human says `start phase N` (see `plan/`).
3. While building: append `docs/BUILD-LOG.md` and update decisions/flow when choices change.
4. **Git:** commit/push only as **engrmaziz** (`Musharraf Aziz` `<io@maziz.me>`). Never Cursor co-author (DEC-018).

## Ponytail (efficient coding)
Follow [Ponytail](https://github.com/DietrichGebert/ponytail): YAGNI → reuse → stdlib → platform → installed dep → one line → minimum.

Not optional to skip: security, validation at trust boundaries, data-loss handling, accessibility, sensor calibration realism, and **explicitly requested** product juice (gamification/animation on non-drive screens).

## Context7 (fresh docs)
Use [Context7](https://github.com/upstash/context7) MCP/CLI before coding against libraries. Prefer library IDs in queries.

## Stitch MCP (UI)
Use Stitch MCP from `.cursor/mcp.json` for UI ideation. Implement in Jetpack Compose. Enforce AutoNexar theme — no generic purple AI slop.

## Strix (QA subset)
[Strix](https://github.com/usestrix/strix) = security pentest slice on staging, not whole QA. Functional + real-car tests remain primary.

## Product constraints
- Live Drive = calm/minimal; max gamify Home/Summary/Boards/Garage/post-booking.
- Never primary-compete on raw top speed.
- Production on minimal budget (see DEC-007/008/014/015).
- Server authority for scores, ranks, invoices, certified history.

## Skills
Use project skills under `.cursor/skills/` for UI, gamification, and build logging.
