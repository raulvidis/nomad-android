---
summary: "Manual smoke-test procedures per feature, for pre-release verification."
read_when:
  - "Verifying a build before a release"
  - "Confirming a feature still works end to end"
---

# Manual Tests

Run on a physical arm64-v8a device, fully offline (airplane mode) unless a step is a download.

## Onboarding / boot
- First launch shows the Pip-Boy boot sequence; completes to Dashboard.

## AI chat
- Without model present: chat answers via `FallbackEngine` (rule-based), no crash.
- Download MiniCPM5-1B in Settings; chat streams a response grounded in knowledge base (RAG).
- Message > 10K chars shows an error, not a silent drop.

## Maps
- Download an offline region; failed-tile count is surfaced on completion.
- GPS tracking, waypoints, and route recording work offline.
- Stored MBTiles render with no network.

## Knowledge
- Browse a bundled survival pack and an imported Kiwix ZIM offline.
- Content pack add/remove works; `packId` with path-traversal chars is rejected.

## Notes
- Create/edit a markdown note with live preview; search finds it.
- Save/delete errors surface to the user (not silently dropped).

## Emergency
- First aid guides and checklists open and render offline.

## Persistence
- Kill and relaunch app: notes, waypoints, chat history persist (Room DB v6).

## Downloads / services
- Background download holds WakeLock until complete (≤ 2h safety timeout); leaving the app does not corrupt DB state.
