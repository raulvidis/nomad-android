---
summary: "Full toolchain, submodule, and NDK installation for a fresh dev environment."
read_when:
  - "Setting up a new development machine"
  - "CMake/NDK or submodule build errors appear"
---

# Install

## Toolchain

| Tool | Version |
|---|---|
| JDK | 17 (Temurin recommended) |
| Android SDK | API 35 |
| Build Tools | 35.0.0 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |
| Gradle | 8.13 (wrapper) |
| AGP | 8.13.2 |
| Kotlin | 2.0.21 |
| NDK | r27c |
| CMake | bundled with NDK (via SDK Manager) |

Install Android Studio (latest stable) and add API 35 + Build Tools 35.0.0 + NDK + CMake through the SDK Manager.

## Native submodule (required)

llama.cpp is vendored as a git submodule at `app/src/main/cpp/llama.cpp`:

```bash
git submodule update --init --recursive
```

Without it the native build (`libnomad_llm.so`) fails. CI checks out the submodule and sets up NDK r27c before building.

## ABI

Native code is built for **arm64-v8a only** (`abiFilters`). Use a physical arm64 device. To add another ABI you must change `app/build.gradle.kts` and get approval (see AGENTS.md).

## Verify

```bash
./gradlew assembleDebug
```

Troubleshooting: [troubleshooting.md](troubleshooting.md).
