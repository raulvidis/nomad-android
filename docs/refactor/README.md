---
summary: "Convention for ephemeral refactor-tracking docs in this folder."
read_when:
  - "Starting or tracking a large refactor"
---

# Refactor Tracking

Ephemeral working scratchpads for in-progress refactors. NOT permanent documentation.

## Convention

- One file per refactor: `YYYY-MM-DD-topic.md` (e.g. `2026-06-19-room-v7-migration.md`).
- **Created** when starting a significant refactor.
- **Updated** as work progresses (scope, decisions, remaining steps).
- **Deleted** when the work lands — do not let these accumulate.

Keep durable design decisions in `../architecture.md` or `../spec.md`, not here.
