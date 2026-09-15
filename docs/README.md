# Meru Docs Index — AI Agent Entry Point

**Product:** Meru · Phases: [`../plan/README.md`](../plan/README.md)

Read in this order:

1. **[decisions.md](./decisions.md)** — accepted architecture / tooling / budget decisions (append while building)  
2. **[flow.md](./flow.md)** — user & system flows  
3. **[brainstorm.md](./brainstorm.md)** — ideas; only ship items tagged/promoted  
4. **[00-EXECUTIVE-SUMMARY.md](./00-EXECUTIVE-SUMMARY.md)** — 150–200 word product snapshot  
5. **[AI-AGENT-BUILD-BIBLE.md](./AI-AGENT-BUILD-BIBLE.md)** — master A–Z / 0–100 build guide  
6. **[ANNEX-A-SCREEN-SPECS.md](./ANNEX-A-SCREEN-SPECS.md)** — every screen detailed behavior  
7. **[ANNEX-B-BACKEND-API.md](./ANNEX-B-BACKEND-API.md)** — APIs, workers, AuthZ  
8. **[ANNEX-C-DATABASE.md](./ANNEX-C-DATABASE.md)** — schema/indexes/Room  
9. **[ANNEX-D-QA.md](./ANNEX-D-QA.md)** — test protocols (+ Strix security subset)  
10. **[ANNEX-E-DESIGN-TOOLING.md](./ANNEX-E-DESIGN-TOOLING.md)** — Stitch MCP / anti-slop / gamify  
11. **[BUILD-LOG.md](./BUILD-LOG.md)** — append every build slice  

Also: repo root `AGENTS.md`, `.cursor/rules/`, `.cursor/skills/`.

### Tooling (mandatory habits)
| Tool | Link | Role |
|------|------|------|
| Ponytail | https://github.com/DietrichGebert/ponytail | Efficient coding |
| Context7 | https://github.com/upstash/context7 | Up-to-date lib docs (MCP in `.cursor/mcp.json`) |
| Stitch MCP | `.cursor/mcp.json` | UI designing |
| Strix | https://github.com/usestrix/strix | Security QA **subset** only |

**Do not implement app code until the user explicitly says to start building.**
