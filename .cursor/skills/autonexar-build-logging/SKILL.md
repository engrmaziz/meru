---
name: autonexar-build-logging
description: >-
  Append build progress to docs/BUILD-LOG.md and update docs/decisions.md,
  docs/flow.md, or docs/brainstorm.md when choices change. Use whenever
  implementing features, finishing a slice, changing architecture, or the user
  mentions build log / decisions / flow.
---

# AutoNexar Build Logging

After each meaningful implementation slice:

1. Append an entry to `docs/BUILD-LOG.md` using its template.
2. If a new architectural choice was made → add `DEC-XXX` to `docs/decisions.md`.
3. If a user/system flow changed → edit `docs/flow.md`.
4. Park speculative ideas in `docs/brainstorm.md` with tags; promote when accepted.

Never delete prior decision entries; supersede them.
