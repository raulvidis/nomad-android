---
summary: "Semver release checklist: changelog, version bump, tag, GitHub release, verify."
read_when:
  - "Cutting a release"
  - "Establishing or bumping the app version"
---

# Releasing

## Versioning policy

- Semantic Versioning, established from `versionName` **1.0.0** (the first semver release).
- "Shipped" = a release **git tag** (e.g. `v1.0.0`), not a `main` merge.
- The CI `latest-build` release is a rolling artifact, NOT a versioned release — do not treat it as one.
- APK naming: `nomad-android-<versionName>.apk` (e.g. `nomad-android-1.0.0.apk`), set in `app/build.gradle.kts`.

## Checklist

1. **CHANGELOG** — move items from `## [Unreleased]` into a new `## [X.Y.Z] - YYYY-MM-DD` section.
2. **Bump version** in `app/build.gradle.kts`: `versionName` (semver) and `versionCode` (monotonic +1).
3. **Commit** the version bump + changelog: `chore(release): vX.Y.Z`.
4. **Tag**: `git tag vX.Y.Z` (annotated preferred).
5. **GitHub Release** for the tag, body = that version's changelog section. Attach `nomad-android-X.Y.Z.apk`.
6. **Verify**: tag exists, release exists, APK attached and installs (`adb install`).
7. **Reopen** an empty `## [Unreleased]` section at the top of CHANGELOG.

## Guardrails

- Do NOT push or publish without explicit confirmation (AGENTS.md Git rules).
- Run quality gates first (see `.agents/commands/fix.md`): lint + unit tests + `assembleDebug`.
- Contributor PRs in the release: keep `#PR` + `@contributor` credit in the changelog.
