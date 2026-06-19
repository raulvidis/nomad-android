---
summary: "Index of all docs — what exists and when to read each."
read_when:
  - "Starting a session, to learn what documentation exists"
  - "Deciding which doc covers a given topic"
---

# Docs Index

Routing table for agents and humans. Each doc carries `summary` + `read_when` frontmatter.

| Doc | Summary | Read when |
|---|---|---|
| [architecture.md](architecture.md) | Modules, data flow, AI engine stack, native llama.cpp JNI. | Understanding or changing app structure. |
| [spec.md](spec.md) | Offline-first goals, non-goals, compatibility commitments. | Evaluating scope or new dependencies. |
| [design.md](design.md) | Pip-Boy CRT terminal design system (colors, type, components). | Building or changing UI. |
| [quickstart.md](quickstart.md) | Build and run in ~5 minutes. | First time building the app. |
| [install.md](install.md) | Full toolchain + submodule + NDK install. | Setting up a fresh dev environment. |
| [configuration.md](configuration.md) | Build/runtime config: SDK, ABI, model, DB. | Changing build or runtime settings. |
| [RELEASING.md](RELEASING.md) | Semver release checklist, APK naming, tagging. | Cutting a release. |
| [manual-tests.md](manual-tests.md) | Manual smoke-test procedures per feature. | Verifying a build before release. |
| [troubleshooting.md](troubleshooting.md) | Known issues + workarounds. | A build or runtime problem appears. |
| [refactor/](refactor/) | Ephemeral work-tracking docs (YYYY-MM-DD-topic.md). | Starting/tracking a large refactor. |
