---
summary: "Known build/runtime issues and their workarounds."
read_when:
  - "A build or runtime problem appears"
---

# Troubleshooting

## Build

**Native build fails / `libnomad_llm.so` missing**
- llama.cpp submodule not initialized → `git submodule update --init --recursive`.
- NDK/CMake not installed → add NDK r27c + CMake via SDK Manager.

**`assembleDebug` very slow**
- First native build compiles llama.cpp; expect minutes. Subsequent builds are cached. Avoid full clean builds unless necessary.

**Wrong/no ABI on device**
- Only arm64-v8a is built. Use a physical arm64 device, not an x86 emulator.

## Runtime

**AI chat gives generic answers**
- No model installed → `FallbackEngine` is active. Download MiniCPM5-1B in Settings.

**App fails to load native library**
- `LlamaBridge` static init catches `Throwable` and degrades to fallback; check logcat tag `nomad-llm` for the underlying cause.

**Malformed GGUF**
- `GgufMetadata` rejects oversized arrays / overflowing offsets before load; re-download the model from the official GGUF repo.

**Data inconsistency after backup/restore**
- `allowBackup` is disabled by design; restoring across installs is not supported.

## Logs

- Native JNI logcat tag: `nomad-llm`.
- If `adb logcat` is blocked on a vendor device, see personal memory note on unlocking logcat.
