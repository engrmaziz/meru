# Annex D — QA Protocols & Test Cases

## D.0 QA stack (what is / isn’t)
| Layer | Tooling | Notes |
|-------|---------|-------|
| Functional | JUnit, Compose tests, API tests | Every PR |
| Device | Emulator instrumented | Permissions, Room |
| Field | Real-car protocols | S1/S2 telemetry |
| Marketplace | Two-account E2E | S4 |
| **Security subset** | **[Strix](https://github.com/usestrix/strix)** | Staging only; IDOR/auth/injection on API, bookings, invoices, vault shares. **Not** whole QA. Prefer OSS local + own LLM key for budget (`npx skills add usestrix/strix`). |
| Privacy | Play Data Safety checklist | Pre-release |

Log Strix runs in `BUILD-LOG.md`. Never replace car tests with pentests.

---

## D.1 Smoke (every PR)
- App launches; sign-in; open Home; open Drive Ready; no crash.
- API `/health` 200.
- Migrations apply on clean DB.

---

## D.2 S1 critical cases
| ID | Case | Expected |
|----|------|----------|
| S1-01 | Deny location | Primer + recovery; cannot start |
| S1-02 | Start drive | FG notification; speed updates |
| S1-03 | Lock screen 5 min | Tracking continues |
| S1-04 | GPS jump city→city | Distance not inflated |
| S1-05 | End drive offline | Local save pending sync |
| S1-06 | Reconnect | Upload once (idempotent) |
| S1-07 | Tunnel gap | Degraded UI; resume |
| S1-08 | Uncalibrated sensors | Warning; limited metrics |
| S1-09 | Replay scrub | Map/graph sync |
| S1-10 | 3h drive battery | Policy downclock when stopped |

---

## D.3 S2 critical cases
| ID | Case | Expected |
|----|------|----------|
| S2-01 | Spoofed client score | Server score wins |
| S2-02 | Integrity <70 | Not on competitive board |
| S2-03 | Season reset | Fresh ranks; all-time intact |
| S2-04 | Leaderboard opt-out | Hidden |
| S2-05 | Driving Mode | Cannot open boards mid-drive |
| S2-06 | Same road farm | Diminishing exploration XP |
| S2-07 | Friend pass notification | Only if enabled |

---

## D.4 S3 critical cases
| ID | Case | Expected |
|----|------|----------|
| S3-01 | Second car without pay | Paywall |
| S3-02 | Purchase slot | Can add car |
| S3-03 | Manual service offline | Queued; appears in timeline |
| S3-04 | VIN display | Masked by default |
| S3-05 | Timeline one-tap | All event types merged sorted |
| S3-06 | Doc expiry | Reminder scheduled |

---

## D.5 S4 critical cases
| ID | Case | Expected |
|----|------|----------|
| S4-01 | Audi car search | Audi specialists ranked up |
| S4-02 | Double book same slot | One success only |
| S4-03 | Hold expiry | Slot freed |
| S4-04 | History share expiry | Staff denied |
| S4-05 | Extra work deny | Not billed |
| S4-06 | Invoice confirm | Certified vault entry created |
| S4-07 | Invoice without confirm | No vault write |
| S4-08 | Review without job | Rejected |
| S4-09 | Unverified workshop | No trust badge |

---

## D.6 Real-car protocol sheet (print)
- Date / phone model / mount / weather / route / duration / battery start-end / notes GPS / screenshots trip id / bugs.

---

## D.7 Accessibility
- Speed readable at arm length; TalkBack labels; color not sole severity signal; reduce motion option later.
