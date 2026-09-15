# Defect Ledger

| Defect | Severity | Repro Steps | Expected vs Actual | File/Symbol | Fix Status |
|---|---|---|---|---|---|
| Map/Method shadowing | S0 | Check `vault.controller.ts` methods vs fields | No shadowing. Was shadowing `ownership`, `documents`, `services` | `backend/src/vault.controller.ts` | Fixed |
| Web syntax error | S0 | `cd workshop-web && npm run build` | Should build. Fails on unescaped quote | `workshop-web/app/page.tsx` | Fixed |
| Android Gradle build | S2 | `cd android && ./gradlew build` | Should compile. Gradle error | Android gradle config | Blocked |
