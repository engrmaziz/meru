---
name: autonexar-ui-anti-slop
description: >-
  Design and implement AutoNexar UI that looks intentional, branded, and premium —
  not generic AI purple gradients. Use when generating Stitch screens, building
  Compose UI, theming, Welcome/Home/Drive/Trip/Garage/Workshop visuals, or when
  the user mentions Stitch, design system, or anti-slop.
---

# AutoNexar UI Anti-Slop

## Always
1. Use **Stitch MCP** (`.cursor/mcp.json` server `stitch`) for new screen ideation.
2. Prepend the theme prefix from `docs/ANNEX-E-DESIGN-TOOLING.md`.
3. Implement production UI in **Jetpack Compose** — never ship Stitch HTML/WebView as main UI.
4. Tokens: void `#0B0F14`, panel `#121821`, teal `#2EE6A6`, cyan `#3DB9FF`, amber `#F5A524`, text `#F4F7FB`.
5. Welcome: brand wordmark is hero-level; one headline; one support line; one CTA group; full-bleed atmosphere.
6. Drive: large speed numerals, sparse chrome, night contrast, glanceable.
7. Prefer expressive display font for speed/score — avoid Inter/Roboto/Arial as hero.

## Never
- Purple-on-white / purple-indigo AI gradient defaults
- Cream editorial + terracotta newspaper layouts
- Card-soup dashboards on marketing/first viewport
- Floating badge clutter on hero/drive
- Mid-drive social/leaderboard chrome

## Checklist before marking UI done
- [ ] Looks like AutoNexar if nav removed (brand test on Welcome)
- [ ] One job per section
- [ ] Motion: at least one intentional animation on non-Drive polished screens
- [ ] 48dp touch targets; contrast OK at night
