---
summary: "Build and run the app in about five minutes."
read_when:
  - "Building the app for the first time"
---

# Quickstart

## Prerequisites (short)

- JDK 17 (Temurin)
- Android SDK API 35, Build Tools 35.0.0
- CMake + Android NDK (r27c) for the native llama.cpp build
- Android Studio (latest stable) recommended

Full setup: [install.md](install.md).

## Get the code (with submodule)

```bash
git clone https://github.com/raulvidis/nomad-android.git
cd nomad-android
git submodule update --init --recursive   # pulls llama.cpp
```

## Build & run

```bash
./gradlew assembleDebug          # debug APK (arm64-v8a)
./gradlew testDebugUnitTest      # unit tests
./gradlew lint                   # lint
```

Output APK: `app/build/outputs/apk/debug/nomad-android-<versionName>.apk`

```bash
adb install app/build/outputs/apk/debug/nomad-android-*.apk
```

## Notes

- Target is a **physical arm64-v8a device**; x86/x86_64 emulators are not built (single-ABI policy).
- The AI model (MiniCPM5-1B GGUF, ~656 MB) is downloaded in-app, not bundled. Without it, the app uses the rule-based `FallbackEngine`.
