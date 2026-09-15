# Soft Launch Checklist — Lahore (Summit)

**City:** Lahore (`pk-pb-lhr`)  
**Product:** Meru  
**Decision:** DEC-026  

## Sign-off targets (beta)
| Metric | Target | Source |
|--------|--------|--------|
| Crash-free sessions | ≥ 99% | Play Console / Crashlytics |
| Trip sync success | ≥ 95% | `GET /v1/admin/metrics` + WorkManager |
| Critical Strix / security e2e | 0 open | `npm run test:e2e` security suite |
| Booking confirm → vault stamp | Works on pilot workshops | Manual S4 |

## Pre-flight
- [ ] `MERU_ADMIN_KEY` set in prod (not `meru-dev-admin`)
- [ ] Feature flags reviewed (`GET /v1/flags`) — kill `bookingsEnabled` if incident
- [ ] Seed / KYC ≥ 3 verified workshops in Lahore
- [ ] Play Data Safety form filled (location, sensors, account deletion path)
- [ ] Store listing screenshots (void/teal Meru theme — no purple slop)
- [ ] Support playbook linked in Play console

## Field battery pass
- [ ] 30 min city drive
- [ ] 60 min mixed
- [ ] 180 min highway/intercity (downclock when stopped)

## Launch day
- [ ] Monitor authFailures / bookingErrors / rateLimited
- [ ] Pilot invite list only (closed testing track)
- [ ] Workshop staff trained on `workshop-web`

## Cost review (cut until earning)
| Item | Soft-launch stance |
|------|--------------------|
| Maps | MapLibre demotiles / self-host; no Google Maps billing |
| FCM | Optional; local notifs first |
| DB | Single Postgres when leaving in-memory |
| R2 | Stub PDFs until invoice volume justifies |
| Hosting | Single cheap box (DEC-007/015) |

**Signed:** _________________ Date: _________
