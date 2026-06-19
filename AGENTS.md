# AGENTS.md

Work style: telegraph; noun-phrases ok; drop grammar; min tokens. Hard rules only — workflows live in `.agents/commands/`.

## Core
- Repo: `nomad-android` — offline-first Android survival app. Kotlin 2.0.21 + Jetpack Compose, single module `app/`.
- Package: `com.nomad.android`. Source root: `app/src/main/java/com/nomad/android/`.
- Native: `app/src/main/cpp/` — llama.cpp submodule + JNI → `libnomad_llm.so`.
- Branch: `main`. Conventional Commits. versionName `1.0.0`.
- **OFFLINE-FIRST is the product.** NEVER add network/cloud/telemetry/analytics deps. App must work fully with no internet. OkHttp exists for user-initiated downloads (models, ZIM, tiles) ONLY.
- **Single-model policy:** only OpenBMB MiniCPM5-1B (Q4_K_M GGUF). No other LLM, no vision/"-V" model.
- "Make a note" = terse AGENTS.md edit, not a new doc.
- Release = git tag (semver), not a `main` merge. `latest-build` = CI rolling artifact, NOT a release.
- Contributing rules: see `CONTRIBUTING.md`.

## Routing
- Screenshots/assets: `stitch-assets/`.
- Docs index: `docs/index.md` — read at session start.
- Design system: `docs/design.md` (root `design.md` kept as-is, do not edit).
- Secrets: none in repo. Never echo env / signing keys. No keystore commits.
- Build needs: JDK 17, Android SDK API 35, Build Tools 35.0.0, CMake + NDK (r27c), llama.cpp submodule initialized.
- Native build: arm64-v8a ONLY (`abiFilters`). Do not add other ABIs without approval.

## Project Defaults
- Build tool: Gradle (`./gradlew`). Never swap build system without approval.
- UI: Jetpack Compose + Material 3. No XML layouts / Views.
- DI: Hilt (KSP), modules in `di/`. Persistence: Room (KSP), DB v6, migrations required for schema changes — never drop/recreate.
- Pattern: MVVM + Repository. ViewModels expose `StateFlow<XxxUiState>`; repos return `Result<T>` (sealed, `data/Result.kt`).
- UI aesthetic: retro Pip-Boy CRT terminal — phosphor green `#00FF41`, scanlines, monospace, no rounded Material corners, no multi-color icons. Follow `docs/design.md`.
- Bug fix → add regression test. Refactor → delete old paths by default.
- Read repo docs before coding; update docs + CHANGELOG for visible changes.
- Session start + before coding: run $docs-list (`python3 .agents/skills/docs-list/scripts/docs-list.py`); read docs whose read_when matches.
- New dep → health check + offline-first audit first (reject anything phoning home).

## PR / CI
- CI (GitHub Actions, on push/PR to `main`): lint → testDebugUnitTest → assembleDebug.
- PR flow: fix → test → changelog → review → merge.
- "fix ci" = consent to pull, commit, push, rerun until green.
- Cite fix + file/line in review comments.
- After landing: recap what landed (2-5 sentences).
- Contributor PRs: thank in CHANGELOG, include `#PR` + `@contributor`, preserve credit.

## Reviews
- Pre-commit / pre-land: run $autoreview until no actionable findings remain.
- $autoreview delegates to installed review skills (/code-review, superpowers) — don't hand-roll review.

## Git
- Safe by default (status/diff/log). Push only when asked.
- Stage explicit paths — never `git add .`.
- Destructive ops forbidden without explicit consent. No amend unless asked.
- Conventional Commits: `feat|fix|refactor|build|ci|chore|docs|style|perf|test`.
- Unrecognized working-tree changes → assume other agent, keep going.

## Runtime Safety
- zsh: quote globs/arrays; mind splitting.
- Never inline shell snippets in GitHub bodies — heredoc + file.
- Secrets: never run env/set/export or broad secret dumps.
- Don't run full gradle build casually (slow; needs NDK + submodule). Prefer targeted tasks.

## Workflows
Procedures in `.agents/commands/`. To run one, read the file and follow it.
- handoff · pickup · commit · fix · release
Shared skills in .agents/skills/: $docs-list, $autoreview.
