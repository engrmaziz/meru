---
name: autonexar-tooling-stack
description: >-
  Reminds agents to use Ponytail for minimal code, Context7 for up-to-date library
  docs, Stitch MCP for UI, and Strix as a security QA subset. Use when coding
  libraries, designing UI, starting a build slice, or running security checks.
---

# AutoNexar Tooling Stack

| Tool | Use for | Not for |
|------|---------|---------|
| [Ponytail](https://github.com/DietrichGebert/ponytail) | Minimal correct code, less over-build | Removing requested gamify/motion |
| [Context7](https://github.com/upstash/context7) | Current Compose/Room/Hilt/Nest/Postgres/Redis docs | Replacing product bible |
| Stitch MCP | Screen ideation | Production Android UI runtime |
| [Strix](https://github.com/usestrix/strix) | Staging pentest (IDOR, auth, injection) | Replacing unit/UI/car/E2E QA |

## Context7 habit
Before unfamiliar API usage: resolve library id → query-docs. Example libraries: Jetpack Compose, Android Room, Hilt, NestJS, PostgreSQL, BullMQ, MapLibre.

## Strix habit
`npx skills add usestrix/strix` when human requests security pass. Target staging API/URLs only. Record findings in BUILD-LOG.

## Budget
Prefer decisions DEC-007/008/014/015 free-tier / single-box production path.
