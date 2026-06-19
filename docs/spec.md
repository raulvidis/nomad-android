---
summary: "Offline-first design constraints: goals, non-goals, compatibility commitments."
read_when:
  - "Evaluating scope, new dependencies, or product direction"
  - "Deciding whether a feature fits the offline-first mission"
---

# Spec — Offline-First Constraints

## Goals (what NOMAD Android IS)

- A self-contained survival knowledge app that works with **zero internet connectivity**.
- On-device AI chat (llama.cpp + MiniCPM5-1B) with retrieval-augmented answers from local content.
- Offline maps (MapLibre + MBTiles), GPS, waypoints, route recording.
- Offline knowledge: Kiwix ZIM (Wikipedia) + bundled survival content + content packs.
- Emergency tools and markdown notes, fully local.
- A deliberate retro Pip-Boy CRT terminal aesthetic.

## Non-goals (what it is NOT)

- NOT a cloud/online app. No telemetry, analytics, accounts, or sync.
- NOT multi-model. Only OpenBMB MiniCPM5-1B (Q4_K_M GGUF). No other LLM, no vision/"-V" model.
- NOT multi-ABI. arm64-v8a only.
- NOT a server/desktop port. Android, physical arm64 devices.

## Compatibility commitments

- **Offline-first is permanent.** Network code is allowed ONLY for explicit, user-initiated downloads (model, ZIM, tiles) via OkHttp. Anything that phones home is rejected.
- minSdk 26 — do not raise without approval.
- Room schema is forward-migrated; every schema change ships a migration (no destructive recreate).
- Single-model and single-ABI policies hold across versions unless explicitly revised here.

## Bundling / build constraints

- Deterministic native build: llama.cpp pinned via submodule; tests/examples/tools/server OFF; `LLAMA_CURL=OFF`.
- AI model is downloaded at runtime, not bundled in the APK (size).
- APK named `nomad-android-<versionName>.apk`.
