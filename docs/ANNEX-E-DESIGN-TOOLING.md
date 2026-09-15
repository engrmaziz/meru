# Annex E — Design Agent Pack (Stitch + Figma + MCP)

---

## E.1 Tooling decision (final)
1. **Stitch MCP** (configured in `.cursor/mcp.json` → `https://stitch.googleapis.com/mcp`) — primary UI ideation for agents.  
2. Optional Figma refine for tokens/components if a designer joins.  
3. **Project skill** `.cursor/skills/autonexar-ui-anti-slop` — ban generic AI purple slop.  
4. **Project skill** `.cursor/skills/autonexar-gamify-motion` — max juice on non-Drive screens.  
5. **Context7** — up-to-date Compose/Maps docs while implementing.  
6. Production UI = **Jetpack Compose only** (Stitch HTML is reference, never the runtime).

See DEC-006, DEC-009.

---

## E.2 Stitch prompt library (copy)

### Global theme prefix (prepend to every prompt)
```
Meru Android mobile UI. Dark adventure driving dashboard.

Colors: bg #0B0F14, panel #121821, accent teal #2EE6A6, cyan #3DB9FF,
amber #F5A524, text #F4F7FB muted #8B97A8. Large tabular speed numerals.
No purple gradients, no cream newspaper layout, no generic Material purple.
Night-driving contrast. Sparse chrome. Premium automotive telematics + adventure game.
```

### Screens to generate first (priority)
1. Welcome  
2. Home Dashboard  
3. Drive Detailed  
4. Drive Minimal  
5. Trip Summary  
6. Trip Replay  
7. Leaderboard City  
8. Garage Vehicle Timeline  
9. Workshop Profile  
10. Invoice Review  

### Variant rule
For Drive screens generate 3 variants; pick calmest for production.

---

## E.3 Figma structure
```
Foundations/Tokens
Components/Buttons|Meters|RankRow|VehicleCard|WorkshopCard|TimelineNode
Patterns/Home|Drive|Trip|Garage|Booking
Screens/...
```

Export tokens JSON → `android/.../designsystem/Theme.kt`.

---

## E.4 Attraction-themed motion list (ship 2–3 early)
1. Route polyline draw on trip complete  
2. XP bar fill + level-up burst  
3. Rank delta arrow bounce on Home  
Later: cinematic ken-burns map, discovery pulse, invoice stamp.

---

## E.5 Customer attraction vs theme fit
| Moment | Visual | Retention job |
|--------|--------|----------------|
| First open | Hero brand + adventure map teaser | Desire |
| First drive | Huge calm speed | Trust/safety |
| First summary | Quality score + roads XP | Dopamine without speed cult |
| First rank | City board YOU row | Social hook |
| First garage timeline | Lifetime car story | Switching cost |
| First booking writeback | Certified stamp in timeline | Marketplace trust |

Theme consistency (teal-on-void adventure) makes these moments feel like one product, not five apps glued together.
