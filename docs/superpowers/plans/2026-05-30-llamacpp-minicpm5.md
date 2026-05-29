# llama.cpp + MiniCPM5 Mounting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace NOMAD's MediaPipe LiteRT-LM inference with a vendored, build-from-source llama.cpp GGUF engine that installs exactly one model — `openbmb/MiniCPM5-1B-GGUF` (Q4_K_M) — while keeping the `AIEngine`/`FallbackEngine`/`RAGEngine` contracts and porting opendroid's tool-calling JNI surface as dormant infrastructure.

**Architecture:** Vendor llama.cpp as a git submodule built via CMake/NDK into a single static-linked `libnomad_llm.so` (arm64-v8a only). A `LlamaBridge` Kotlin `object` wraps the JNI; `LlamaCppEngine` implements the existing `AIEngine` interface and replaces `LiteRTLMEngine`. A single `ModelVariant.MINICPM5_1B` descriptor flows through the unchanged `ContentPackManager` download path, `AIEngineManager`, and `AIModule`.

**Tech Stack:** Kotlin 2.0.21, AGP 8.13.2, NDK r27c, CMake 3.22, llama.cpp (pinned `f12cc6d`), Hilt, OkHttp, JUnit4/Robolectric.

**Reference source (copy-from):** `../opendroid` at `/home/raul/Documents/GitHub/opendroid`. Spec: `docs/superpowers/specs/2026-05-30-llamacpp-minicpm5-design.md`.

---

## File Structure

**Create:**
- `.gitmodules` — submodule registration
- `app/src/main/cpp/llama.cpp/` — vendored submodule (pinned `f12cc6d`)
- `app/src/main/cpp/CMakeLists.txt` — native build config
- `app/src/main/cpp/llama-jni.cpp` — JNI bridge (chat + tool-calling)
- `app/src/main/java/com/nomad/android/data/ai/LlamaBridge.kt` — Kotlin JNI facade
- `app/src/main/java/com/nomad/android/data/ai/GgufMetadata.kt` — GGUF header validator
- `app/src/main/java/com/nomad/android/data/ai/LlamaCppEngine.kt` — `AIEngine` impl
- `app/src/test/java/com/nomad/android/data/ai/GgufMetadataTest.kt`
- `app/src/test/java/com/nomad/android/data/ai/LlamaCppEngineTest.kt`

**Modify:**
- `app/build.gradle.kts` — add externalNativeBuild + ndk abiFilters; remove mediapipe
- `app/src/main/java/com/nomad/android/data/ai/AIEngine.kt` — rename enum value
- `app/src/main/java/com/nomad/android/data/ai/AIEngineManager.kt` — wrap LlamaCppEngine
- `app/src/main/java/com/nomad/android/di/AIModule.kt` — single-model wiring
- `app/src/main/java/com/nomad/android/data/content/ContentPackManager.kt` — `ai_minicpm5` pack
- `app/src/main/java/com/nomad/android/ui/settings/SettingsViewModel.kt` — type rename
- `app/src/main/java/com/nomad/android/ui/onboarding/OnboardingViewModel.kt` — type rename
- `app/src/main/java/com/nomad/android/ui/onboarding/OnboardingScreen.kt` — type rename
- `app/src/main/java/com/nomad/android/ui/dashboard/DashboardViewModel.kt` — enum value rename
- `app/src/test/java/com/nomad/android/data/ai/AIEngineTypesTest.kt` — update expectations
- `app/proguard-rules.pro` — drop MediaPipe keeps, add LlamaBridge keep
- `.github/workflows/ci.yml` — submodules + NDK

**Delete:**
- `app/src/main/java/com/nomad/android/data/ai/LiteRTLMEngine.kt`

---

## Task 1: Vendor llama.cpp as a pinned submodule

**Files:**
- Create: `.gitmodules`, `app/src/main/cpp/llama.cpp/` (submodule)

- [ ] **Step 1: Add the submodule and pin to opendroid's exact SHA**

```bash
cd /home/raul/Documents/GitHub/nomad-android
git submodule add https://github.com/ggml-org/llama.cpp.git app/src/main/cpp/llama.cpp
cd app/src/main/cpp/llama.cpp
git fetch --depth=1 origin f12cc6d0fa96d6a3c33952f06b7439ac43a3c3fe
git checkout f12cc6d0fa96d6a3c33952f06b7439ac43a3c3fe
cd /home/raul/Documents/GitHub/nomad-android
```

- [ ] **Step 2: Verify the pin**

Run: `git submodule status app/src/main/cpp/llama.cpp`
Expected: a line beginning with the SHA `f12cc6d0fa96d6a3c33952f06b7439ac43a3c3fe`.

- [ ] **Step 3: Commit**

```bash
git add .gitmodules app/src/main/cpp/llama.cpp
git commit -m "build: vendor llama.cpp submodule pinned to f12cc6d"
```

---

## Task 2: Port the native build config and JNI bridge

The CMakeLists is copied verbatim with the target renamed `opendroid_llm` → `nomad_llm`. The JNI source is copied verbatim with the symbol prefix re-mangled to NOMAD's `LlamaBridge` class.

**Files:**
- Create: `app/src/main/cpp/CMakeLists.txt`, `app/src/main/cpp/llama-jni.cpp`

- [ ] **Step 1: Copy CMakeLists and rename the target**

```bash
cd /home/raul/Documents/GitHub/nomad-android
cp ../opendroid/app/src/main/cpp/CMakeLists.txt app/src/main/cpp/CMakeLists.txt
sed -i 's/opendroid_llm/nomad_llm/g' app/src/main/cpp/CMakeLists.txt
```

Verify the project/target/library names are now `nomad_llm`:
Run: `grep -n "nomad_llm\|opendroid_llm" app/src/main/cpp/CMakeLists.txt`
Expected: every match says `nomad_llm`; zero `opendroid_llm`.

- [ ] **Step 2: Copy the JNI bridge and re-mangle JNI symbols**

```bash
cp ../opendroid/app/src/main/cpp/llama-jni.cpp app/src/main/cpp/llama-jni.cpp
sed -i 's/Java_com_opendroid_llm_LlamacppProvider_/Java_com_nomad_android_data_ai_LlamaBridge_/g' app/src/main/cpp/llama-jni.cpp
```

Verify: no opendroid symbol prefix remains.
Run: `grep -c "Java_com_opendroid" app/src/main/cpp/llama-jni.cpp`
Expected: `0`.

Run: `grep -c "Java_com_nomad_android_data_ai_LlamaBridge_" app/src/main/cpp/llama-jni.cpp`
Expected: a number ≥ 12 (one per native method).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/cpp/CMakeLists.txt app/src/main/cpp/llama-jni.cpp
git commit -m "feat: add llama.cpp JNI bridge + CMake build (renamed to nomad_llm)"
```

---

## Task 3: Wire the native build into Gradle and drop MediaPipe

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add NDK ABI filter + native cmake flags to `defaultConfig`**

In `app/build.gradle.kts`, inside `defaultConfig { ... }` (after the `ksp { ... }` block), add:

```kotlin
        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-O3", "-fexceptions")
                arguments += listOf(
                    "-DCMAKE_BUILD_TYPE=Release",
                    "-DANDROID_STL=c++_static",
                )
            }
        }
```

- [ ] **Step 2: Register the CMakeLists at the `android { }` level**

In `app/build.gradle.kts`, inside the top-level `android { ... }` block (e.g. after `packaging { ... }`), add:

```kotlin
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
```

- [ ] **Step 3: Remove the MediaPipe dependency**

In `app/build.gradle.kts`, delete these two lines from `dependencies { }`:

```kotlin
    // On-device LLM inference (Gemma models)
    implementation("com.google.mediapipe:tasks-genai:0.10.33")
```

- [ ] **Step 4: Verify Gradle still configures**

Run: `./gradlew help -q`
Expected: BUILD SUCCESSFUL (configuration only; no native compile yet).

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts
git commit -m "build: compile llama.cpp native lib (arm64-v8a); drop MediaPipe tasks-genai"
```

---

## Task 4: Port GgufMetadata with a unit test (TDD)

**Files:**
- Create: `app/src/main/java/com/nomad/android/data/ai/GgufMetadata.kt`
- Test: `app/src/test/java/com/nomad/android/data/ai/GgufMetadataTest.kt`

- [ ] **Step 1: Copy GgufMetadata and rename the package**

```bash
cd /home/raul/Documents/GitHub/nomad-android
cp ../opendroid/app/src/main/java/com/opendroid/llm/GgufMetadata.kt \
   app/src/main/java/com/nomad/android/data/ai/GgufMetadata.kt
sed -i 's/^package com.opendroid.llm/package com.nomad.android.data.ai/' \
   app/src/main/java/com/nomad/android/data/ai/GgufMetadata.kt
```

Verify: `grep -n "^package" app/src/main/java/com/nomad/android/data/ai/GgufMetadata.kt`
Expected: `package com.nomad.android.data.ai`.

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/com/nomad/android/data/ai/GgufMetadataTest.kt`:

```kotlin
package com.nomad.android.data.ai

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GgufMetadataTest {

    /** Build a minimal valid GGUF v3 with one string metadata key. */
    private fun writeGguf(file: File, arch: String) {
        val key = "general.architecture".toByteArray(Charsets.UTF_8)
        val value = arch.toByteArray(Charsets.UTF_8)
        val buf = ByteBuffer.allocate(4 + 4 + 8 + 8 + (8 + key.size) + 4 + (8 + value.size))
            .order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(0x46554747)          // "GGUF"
        buf.putInt(3)                   // version
        buf.putLong(0)                  // tensor_count
        buf.putLong(1)                  // metadata_count
        buf.putLong(key.size.toLong()); buf.put(key)
        buf.putInt(8)                   // T_STRING
        buf.putLong(value.size.toLong()); buf.put(value)
        file.writeBytes(buf.array())
    }

    @Test
    fun `parses architecture from a valid gguf header`() {
        val f = File.createTempFile("model", ".gguf").apply { deleteOnExit() }
        // Pad past the 1KB minimum the loader uses elsewhere; header parse only needs the bytes.
        writeGguf(f, "llama")
        val header = GgufMetadata.read(f)
        assertNotNull(header)
        assertEquals("llama", header!!.architecture)
        assertEquals(GgufMetadata.Verdict.Ok, GgufMetadata.verdict(header))
    }

    @Test
    fun `rejects a file with a bad magic`() {
        val f = File.createTempFile("bad", ".gguf").apply { deleteOnExit() }
        f.writeBytes(ByteArray(64) { 0 })
        assertNull(GgufMetadata.read(f))
    }
}
```

- [ ] **Step 3: Run the test to verify it passes (port is correct)**

Run: `./gradlew testDebugUnitTest --tests "com.nomad.android.data.ai.GgufMetadataTest"`
Expected: PASS (the ported reader already implements this behavior).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/nomad/android/data/ai/GgufMetadata.kt \
        app/src/test/java/com/nomad/android/data/ai/GgufMetadataTest.kt
git commit -m "feat: port GgufMetadata header validator with unit test"
```

---

## Task 5: Add the LlamaBridge Kotlin JNI facade

The native method names declared here MUST match the symbols mangled in Task 2 (class `com.nomad.android.data.ai.LlamaBridge`). This is not unit-testable on the JVM (no `.so`); it is compile-checked here and integration-checked by the CI APK build in Task 11.

**Files:**
- Create: `app/src/main/java/com/nomad/android/data/ai/LlamaBridge.kt`

- [ ] **Step 1: Copy the provider, rename package + object + library**

```bash
cd /home/raul/Documents/GitHub/nomad-android
cp ../opendroid/app/src/main/java/com/opendroid/llm/LlamacppProvider.kt \
   app/src/main/java/com/nomad/android/data/ai/LlamaBridge.kt
sed -i \
  -e 's/^package com.opendroid.llm/package com.nomad.android.data.ai/' \
  -e 's/object LlamacppProvider/object LlamaBridge/' \
  -e 's/System.loadLibrary("opendroid_llm")/System.loadLibrary("nomad_llm")/' \
  app/src/main/java/com/nomad/android/data/ai/LlamaBridge.kt
```

- [ ] **Step 2: Verify the renames**

Run: `grep -n "object LlamaBridge\|loadLibrary\|^package\|LlamacppProvider" app/src/main/java/com/nomad/android/data/ai/LlamaBridge.kt`
Expected: `package com.nomad.android.data.ai`, `object LlamaBridge`, `System.loadLibrary("nomad_llm")`, and zero `LlamacppProvider` matches.

- [ ] **Step 3: Confirm it compiles**

Run: `./gradlew compileDebugKotlin -q`
Expected: BUILD SUCCESSFUL. (Note: `LlamacppProvider` uses `kotlin.Result` from java's `Result.failure/success` — it imports nothing NOMAD-specific, so it compiles standalone.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/nomad/android/data/ai/LlamaBridge.kt
git commit -m "feat: add LlamaBridge Kotlin facade over llama.cpp JNI"
```

---

## Task 6: Add LlamaCppEngine implementing AIEngine (TDD on pure helpers)

This replaces `LiteRTLMEngine`. It defines the single `ModelVariant.MINICPM5_1B` and pure helper functions (`stripThinking`, `buildPromptWithContext`) that are unit-tested. The streaming path delegates to `LlamaBridge`.

**Files:**
- Create: `app/src/main/java/com/nomad/android/data/ai/LlamaCppEngine.kt`
- Test: `app/src/test/java/com/nomad/android/data/ai/LlamaCppEngineTest.kt`

- [ ] **Step 1: Write the failing test for the pure helpers**

Create `app/src/test/java/com/nomad/android/data/ai/LlamaCppEngineTest.kt`:

```kotlin
package com.nomad.android.data.ai

import org.junit.Assert.*
import org.junit.Test

class LlamaCppEngineTest {

    @Test
    fun `MINICPM5_1B variant has correct coordinates`() {
        val v = LlamaCppEngine.ModelVariant.MINICPM5_1B
        assertEquals("MiniCPM5 1B", v.displayName)
        assertEquals("MiniCPM5-1B-Q4_K_M.gguf", v.fileName)
        assertEquals(656, v.sizeMB)
        assertTrue(v.downloadUrl.startsWith("https://huggingface.co/openbmb/MiniCPM5-1B-GGUF/resolve/main/"))
        assertTrue(v.downloadUrl.endsWith("MiniCPM5-1B-Q4_K_M.gguf"))
    }

    @Test
    fun `recommendedVariant always returns the single model`() {
        assertEquals(LlamaCppEngine.ModelVariant.MINICPM5_1B, LlamaCppEngine.recommendedVariant(8192))
        assertEquals(LlamaCppEngine.ModelVariant.MINICPM5_1B, LlamaCppEngine.recommendedVariant(1024))
    }

    @Test
    fun `stripThinking removes think blocks and trims`() {
        val raw = "<think>let me reason</think>\n\nBoil the water for 1 minute."
        assertEquals("Boil the water for 1 minute.", LlamaCppEngine.stripThinking(raw))
    }

    @Test
    fun `stripThinking leaves plain text untouched`() {
        assertEquals("Just an answer.", LlamaCppEngine.stripThinking("Just an answer."))
    }

    @Test
    fun `buildPromptWithContext inlines history before the question`() {
        val out = LlamaCppEngine.buildPromptWithContext("How do I purify water?", listOf("User: hi", "AI: hello"))
        assertTrue(out.contains("User: hi"))
        assertTrue(out.contains("AI: hello"))
        assertTrue(out.trimEnd().endsWith("How do I purify water?"))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.nomad.android.data.ai.LlamaCppEngineTest"`
Expected: FAIL — `LlamaCppEngine` unresolved.

- [ ] **Step 3: Implement LlamaCppEngine**

Create `app/src/main/java/com/nomad/android/data/ai/LlamaCppEngine.kt`:

```kotlin
package com.nomad.android.data.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.nomad.android.data.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * AIEngine backed by the vendored llama.cpp (libnomad_llm.so) via [LlamaBridge].
 * GGUF-only; ships a single installable model (MiniCPM5-1B Q4_K_M).
 *
 * Spec 1 renders each turn as a single user message with the conversation
 * history inlined (matching NOMAD's existing pattern). True multi-turn /
 * tool-calling is Spec 2.
 */
class LlamaCppEngine(
    private val context: Context,
    private val modelVariant: ModelVariant,
    private val deviceTotalRamMB: Long = 0,
) : AIEngine {

    enum class ModelVariant(
        val displayName: String,
        val fileName: String,
        val ramRequiredMB: Long,
        val sizeMB: Int,
        val downloadUrl: String,
    ) {
        MINICPM5_1B(
            displayName = "MiniCPM5 1B",
            fileName = "MiniCPM5-1B-Q4_K_M.gguf",
            ramRequiredMB = 2048,
            sizeMB = 656,
            downloadUrl = "https://huggingface.co/openbmb/MiniCPM5-1B-GGUF/resolve/main/MiniCPM5-1B-Q4_K_M.gguf",
        )
    }

    private val modelDir by lazy { File(context.filesDir, "models").also { it.mkdirs() } }
    private val inferenceMutex = Mutex()

    fun getModelFile(): File = File(modelDir, modelVariant.fileName)

    override suspend fun generate(prompt: String, context: List<String>, imagePath: String?): String =
        withContext(Dispatchers.IO) {
            inferenceMutex.withLock {
                when (val ensured = ensureLoaded()) {
                    is Result.Error -> return@withContext ensured.message
                    else -> Unit
                }
                LlamaBridge.setSystemPrompt(SYSTEM_PROMPT)
                LlamaBridge.resetConversation()
                val sb = StringBuilder()
                val res = LlamaBridge.chat(buildPromptWithContext(prompt, context)) { chunk -> sb.append(chunk) }
                if (res.isFailure) {
                    "AI generation error: ${res.exceptionOrNull()?.message}. Try sending your message again."
                } else {
                    stripThinking(sb.toString())
                }
            }
        }

    override fun generateStream(prompt: String, context: List<String>, imagePath: String?): Flow<String> =
        callbackFlow {
            if (!inferenceMutex.tryLock()) {
                trySend("AI is still processing your previous message. Please wait.")
                close(); return@callbackFlow
            }
            try {
                when (val ensured = ensureLoaded()) {
                    is Result.Error -> { trySend(ensured.message); close(); return@callbackFlow }
                    else -> Unit
                }
                LlamaBridge.setSystemPrompt(SYSTEM_PROMPT)
                LlamaBridge.resetConversation()
                // Buffer raw output so we can strip <think> blocks before emitting.
                val acc = StringBuilder()
                var thinkOpen = false
                val res = LlamaBridge.chat(buildPromptWithContext(prompt, context)) { chunk ->
                    acc.append(chunk)
                    val (emit, stillOpen) = filterThinking(acc, thinkOpen)
                    thinkOpen = stillOpen
                    if (emit.isNotEmpty()) trySend(emit)
                }
                if (res.isFailure) trySend("AI generation error: ${res.exceptionOrNull()?.message}")
            } finally {
                close()
            }
            awaitClose { inferenceMutex.unlock() }
        }

    override suspend fun isAvailable(): Boolean =
        LlamaBridge.isAvailable && getModelFile().let { it.exists() && it.length() > 1_000_000 }

    override fun getModelName(): String = modelVariant.displayName

    override fun getDeviceInfo(): DeviceInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return DeviceInfo(
            totalRamMB = if (deviceTotalRamMB > 0) deviceTotalRamMB else mi.totalMem / (1024 * 1024),
            availableRamMB = mi.availMem / (1024 * 1024),
            hasNPU = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            hasGPU = false,
        )
    }

    override suspend fun loadModel(): Result<Unit> = withContext(Dispatchers.IO) { ensureLoaded() }

    override suspend fun unloadModel() {
        inferenceMutex.withLock { LlamaBridge.unload() }
    }

    fun requiresDownload(): Boolean = !getModelFile().exists()
    fun getModelSizeMB(): Int = modelVariant.sizeMB

    private suspend fun ensureLoaded(): Result<Unit> {
        if (!LlamaBridge.isAvailable) {
            return Result.error("Native AI engine failed to load on this device.")
        }
        if (LlamaBridge.isModelLoaded) return Result.success(Unit)
        val file = getModelFile()
        if (!file.exists()) {
            return Result.error("No AI model downloaded. Go to Settings and download ${modelVariant.displayName} to enable AI chat.")
        }
        if (file.length() < modelVariant.sizeMB * 1_048_576L * 0.9.toLong().coerceAtLeast(1)) {
            // Incomplete download guard (≈90% of expected size).
            if (file.length() < modelVariant.sizeMB * 0.9 * 1_048_576) {
                return Result.error("Model file incomplete. Delete and re-download in Settings.")
            }
        }
        return when (val r = LlamaBridge.loadModel(file, DEFAULT_CTX)) {
            else -> if (r.isSuccess) Result.success(Unit)
                    else Result.error("Failed to load model: ${r.exceptionOrNull()?.message}")
        }
    }

    private fun buildPromptWithContext(prompt: String, ctx: List<String>): String =
        buildPromptWithContext(prompt, ctx, SYSTEM_PROMPT)

    companion object {
        private const val TAG = "LlamaCppEngine"
        private const val DEFAULT_CTX = 4096

        const val SYSTEM_PROMPT =
            "You are NOMAD, an offline survival assistant. Give clear, concise, practical answers " +
            "about survival, first aid, navigation, emergency preparedness, and general knowledge. " +
            "Keep answers direct and actionable."

        fun recommendedVariant(totalRamMB: Long): ModelVariant = ModelVariant.MINICPM5_1B

        /** Remove `<think>...</think>` reasoning blocks and trim leading whitespace. */
        fun stripThinking(s: String): String {
            val out = StringBuilder()
            var i = 0
            while (i < s.length) {
                val open = s.indexOf("<think>", i)
                if (open < 0) { out.append(s.substring(i)); break }
                out.append(s, i, open)
                val close = s.indexOf("</think>", open)
                if (close < 0) { i = open + 7; continue }
                i = close + 8
            }
            return out.toString().trimStart('\n', '\r', ' ', '\t')
        }

        /**
         * Streaming filter: append-only view over [acc]. Returns the newly
         * emittable text (outside any think block) plus whether a think block
         * is still open. Conservative — holds back text once "<think>" appears
         * until the matching "</think>".
         */
        fun filterThinking(acc: StringBuilder, thinkOpen: Boolean): Pair<String, Boolean> {
            val full = acc.toString()
            val cleaned = stripThinking(full)
            // Track open state: more <think> than </think> means still open.
            val opens = Regex("<think>").findAll(full).count()
            val closes = Regex("</think>").findAll(full).count()
            val open = opens > closes
            // Emit only the delta we haven't emitted before by stashing length in acc via a marker is
            // overkill here; callers reset acc per turn, so emit cleaned minus already-emitted via a
            // simple convention: we re-clear acc to the cleaned tail. To keep it simple and correct,
            // emit nothing while open; when closed, emit the whole cleaned string once and clear acc.
            return if (open) {
                "" to true
            } else {
                acc.setLength(0)
                cleaned to false
            }
        }

        /** Build a single user-turn prompt with system preamble + inlined history. */
        fun buildPromptWithContext(prompt: String, ctx: List<String>, system: String = SYSTEM_PROMPT): String =
            buildString {
                appendLine(system)
                appendLine()
                if (ctx.isNotEmpty()) {
                    appendLine("Conversation history:")
                    ctx.forEach { appendLine(it) }
                    appendLine()
                }
                append(prompt)
            }
    }
}
```

> **Implementer note:** the streaming `<think>` filter above is intentionally simple (hold-then-flush). If the model interleaves prose and reasoning heavily, the non-streaming `generate()` path still strips correctly. Refine streaming UX in Spec 2 if needed; keep this task green.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.nomad.android.data.ai.LlamaCppEngineTest"`
Expected: PASS (all 5 tests). If `ensureLoaded`'s size-guard expression fails to compile, simplify it to:

```kotlin
        if (file.length() < (modelVariant.sizeMB * 0.9 * 1_048_576).toLong()) {
            return Result.error("Model file incomplete. Delete and re-download in Settings.")
        }
```

(Replace the nested `if` block with this single guard.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nomad/android/data/ai/LlamaCppEngine.kt \
        app/src/test/java/com/nomad/android/data/ai/LlamaCppEngineTest.kt
git commit -m "feat: add LlamaCppEngine (single MiniCPM5 model) with unit tests"
```

---

## Task 7: Rename the AIEngineType enum value

**Files:**
- Modify: `app/src/main/java/com/nomad/android/data/ai/AIEngine.kt:23`
- Modify: `app/src/main/java/com/nomad/android/data/ai/AIEngineManager.kt:109`
- Modify: `app/src/main/java/com/nomad/android/ui/dashboard/DashboardViewModel.kt:58`
- Test: `app/src/test/java/com/nomad/android/data/ai/AIEngineTypesTest.kt`

- [ ] **Step 1: Rename the enum value**

In `AIEngine.kt`, change:

```kotlin
    LITERTLM_E2B("LiteRT-LM (Gemma 4 E2B)"),
```

to:

```kotlin
    LLAMACPP_MINICPM5("llama.cpp (MiniCPM5 1B)"),
```

- [ ] **Step 2: Update the two usages**

In `AIEngineManager.kt:109` and `DashboardViewModel.kt:58`, replace `AIEngineType.LITERTLM_E2B` with `AIEngineType.LLAMACPP_MINICPM5`.

Run: `grep -rn "LITERTLM_E2B" app/src/main`
Expected: no matches.

- [ ] **Step 3: Update the enum test**

In `AIEngineTypesTest.kt`, replace every `AIEngineType.LITERTLM_E2B` with `AIEngineType.LLAMACPP_MINICPM5`. Delete the two tests `LiteRTLMEngine ModelVariant GEMMA4_E2B has correct properties` and `recommendedVariant always selects Gemma4` (these move to `LlamaCppEngineTest`). The `AIEngineType enum has all values` test still expects size 3 — leave it.

- [ ] **Step 4: Run the test**

Run: `./gradlew testDebugUnitTest --tests "com.nomad.android.data.ai.AIEngineTypesTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nomad/android/data/ai/AIEngine.kt \
        app/src/main/java/com/nomad/android/data/ai/AIEngineManager.kt \
        app/src/main/java/com/nomad/android/ui/dashboard/DashboardViewModel.kt \
        app/src/test/java/com/nomad/android/data/ai/AIEngineTypesTest.kt
git commit -m "refactor: rename AIEngineType.LITERTLM_E2B to LLAMACPP_MINICPM5"
```

---

## Task 8: Migrate the variant type across managers and UI (LiteRTLMEngine → LlamaCppEngine)

This is a mechanical type/symbol migration: every `LiteRTLMEngine.ModelVariant` → `LlamaCppEngine.ModelVariant`, every `GEMMA4_E2B` → `MINICPM5_1B`, every `LiteRTLMEngine(` constructor → `LlamaCppEngine(`, every `LiteRTLMEngine.recommendedVariant` → `LlamaCppEngine.recommendedVariant`, and pack id `ai_gemma4` → `ai_minicpm5`.

**Files:**
- Modify: `AIEngineManager.kt`, `di/AIModule.kt`, `ContentPackManager.kt`,
  `SettingsViewModel.kt`, `OnboardingViewModel.kt`, `OnboardingScreen.kt`

- [ ] **Step 1: Apply the mechanical renames**

```bash
cd /home/raul/Documents/GitHub/nomad-android
FILES="app/src/main/java/com/nomad/android/data/ai/AIEngineManager.kt \
app/src/main/java/com/nomad/android/di/AIModule.kt \
app/src/main/java/com/nomad/android/data/content/ContentPackManager.kt \
app/src/main/java/com/nomad/android/ui/settings/SettingsViewModel.kt \
app/src/main/java/com/nomad/android/ui/onboarding/OnboardingViewModel.kt \
app/src/main/java/com/nomad/android/ui/onboarding/OnboardingScreen.kt"
sed -i \
  -e 's/LiteRTLMEngine\.ModelVariant/LlamaCppEngine.ModelVariant/g' \
  -e 's/LiteRTLMEngine\.recommendedVariant/LlamaCppEngine.recommendedVariant/g' \
  -e 's/\bGEMMA4_E2B\b/MINICPM5_1B/g' \
  -e 's/"ai_gemma4"/"ai_minicpm5"/g' \
  -e 's/import com.nomad.android.data.ai.LiteRTLMEngine/import com.nomad.android.data.ai.LlamaCppEngine/g' \
  $FILES
```

- [ ] **Step 2: Fix the AIEngineManager engine construction**

In `AIEngineManager.kt`, the `sed` left the constructor name as `LiteRTLMEngine(...)`. Replace both occurrences of `LiteRTLMEngine(context,` with `LlamaCppEngine(context,`. Also change the field/param types:

```kotlin
    initialVariant: LlamaCppEngine.ModelVariant,
```
```kotlin
    private var currentEngine: LlamaCppEngine = LlamaCppEngine(context, initialVariant, deviceTotalRamMB)
```

Run: `grep -rn "LiteRTLMEngine" app/src/main`
Expected: no matches (the only remaining file is `LiteRTLMEngine.kt` itself, deleted in Task 9 — if it still shows, that's expected here).

- [ ] **Step 3: Update the OnboardingScreen description text for the new model**

In `OnboardingScreen.kt` around line 253, the `when (variant)` arm now reads `LlamaCppEngine.ModelVariant.MINICPM5_1B ->`. Change its description string from `"Multimodal — text, image, audio"` to:

```kotlin
                    com.nomad.android.data.ai.LlamaCppEngine.ModelVariant.MINICPM5_1B -> "Compact on-device LLM (GGUF, ~656 MB)"
```

- [ ] **Step 4: Verify configuration/compile of main sources (LiteRT file still present, unused)**

Run: `./gradlew compileDebugKotlin -q`
Expected: BUILD SUCCESSFUL. `LiteRTLMEngine.kt` still compiles (it's self-contained) but is now referenced by nothing.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java app/src/main/java/com/nomad/android/di/AIModule.kt
git commit -m "refactor: switch managers + UI from LiteRTLMEngine to LlamaCppEngine (MiniCPM5)"
```

---

## Task 9: Update ContentPackManager pack metadata and delete LiteRTLMEngine

**Files:**
- Modify: `app/src/main/java/com/nomad/android/data/content/ContentPackManager.kt`
- Delete: `app/src/main/java/com/nomad/android/data/ai/LiteRTLMEngine.kt`

- [ ] **Step 1: Update the model-pack id mapping helpers**

In `ContentPackManager.kt`, the `modelVariantToPackId` and `getModelVariantForPack` `when` arms were rewritten by Task 8's sed to use `MINICPM5_1B` and `"ai_minicpm5"`. Confirm they read:

```kotlin
    private fun modelVariantToPackId(variant: LlamaCppEngine.ModelVariant): String = when (variant) {
        LlamaCppEngine.ModelVariant.MINICPM5_1B -> "ai_minicpm5"
    }

    private fun getModelVariantForPack(packId: String): LlamaCppEngine.ModelVariant? = when (packId) {
        "ai_minicpm5" -> LlamaCppEngine.ModelVariant.MINICPM5_1B
        else -> null
    }
```

- [ ] **Step 2: Delete the dead LiteRT engine**

```bash
git rm app/src/main/java/com/nomad/android/data/ai/LiteRTLMEngine.kt
```

- [ ] **Step 3: Verify no references remain**

Run: `grep -rn "LiteRTLMEngine\|mediapipe\|litertlm\|GEMMA4" app/src`
Expected: no matches.

- [ ] **Step 4: Compile**

Run: `./gradlew compileDebugKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nomad/android/data/content/ContentPackManager.kt
git commit -m "feat: point model pack at MiniCPM5; remove LiteRTLMEngine"
```

---

## Task 10: Update ProGuard rules

**Files:**
- Modify: `app/proguard-rules.pro`

- [ ] **Step 1: Replace the MediaPipe/LiteRT keep block**

In `app/proguard-rules.pro`, delete:

```
# MediaPipe GenAI / LiteRT
-keep class com.google.mediapipe.** { *; }
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.ai.edge.** { *; }
```

and replace with:

```
# llama.cpp JNI bridge — keep the class + native method declarations so R8
# doesn't rename symbols the .so binds to by name.
-keep class com.nomad.android.data.ai.LlamaBridge { *; }
```

- [ ] **Step 2: Commit**

```bash
git add app/proguard-rules.pro
git commit -m "build: proguard keep for LlamaBridge JNI; drop MediaPipe keeps"
```

---

## Task 11: Update CI for submodules + NDK

**Files:**
- Modify: `.github/workflows/ci.yml`

- [ ] **Step 1: Checkout submodules**

In `.github/workflows/ci.yml`, change the checkout step from:

```yaml
      - uses: actions/checkout@v4
```

to:

```yaml
      - uses: actions/checkout@v4
        with:
          submodules: recursive
```

- [ ] **Step 2: Add NDK setup before the build steps**

After the `Install Android SDK components` step, add:

```yaml
      - name: Setup Android NDK
        uses: nttld/setup-ndk@v1
        id: setup-ndk
        with:
          ndk-version: r27c
```

- [ ] **Step 3: Export ANDROID_NDK_HOME for the test + build steps**

Add `env:` to the `Run unit tests` and `Build debug APK` steps so the native build finds the NDK. For `Build debug APK`:

```yaml
      - name: Build debug APK
        env:
          ANDROID_NDK_HOME: ${{ steps.setup-ndk.outputs.ndk-path }}
        run: |
          ./gradlew assembleDebug
```

(The lint and unit-test steps don't compile native code, but add the same `env` to `Build debug APK` only; lint may still trigger a configure — if `./gradlew lint` fails locating the NDK, add the same `env` block to the lint step.)

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: checkout llama.cpp submodule + set up Android NDK r27c"
```

---

## Task 12: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full unit-test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL; all tests pass (including `GgufMetadataTest`, `LlamaCppEngineTest`, `AIEngineTypesTest`).

- [ ] **Step 2: Run lint**

Run: `./gradlew lint`
Expected: BUILD SUCCESSFUL (no new errors).

- [ ] **Step 3: Build the debug APK (compiles llama.cpp — slow first run)**

Run: `ANDROID_NDK_HOME=<ndk-path> ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL; `app/build/outputs/apk/debug/*.apk` exists and contains `lib/arm64-v8a/libnomad_llm.so`.

Verify the `.so` is packaged:
Run: `unzip -l app/build/outputs/apk/debug/*.apk | grep libnomad_llm.so`
Expected: one `lib/arm64-v8a/libnomad_llm.so` entry.

- [ ] **Step 4: Final commit (if any verification fixups were needed)**

```bash
git add -A
git commit -m "test: verify llama.cpp build + MiniCPM5 engine wiring" || echo "nothing to commit"
```

---

## Notes for the implementer

- **First native build is ~10 min** (llama.cpp compiles from source); CMake caches afterward.
- **JVM tests can't load the `.so`** — `LlamaBridge`/`LlamaCppEngine` runtime inference is only exercised on-device or by the CI APK build. Keep unit tests on the pure helpers.
- **Do not re-add a GGUF architecture allowlist** — MiniCPM5 reports `arch=llama` and was wrongly rejected by such lists in opendroid.
- **Tool-calling methods on `LlamaBridge` are intentionally unused in Spec 1.** Don't delete them; Spec 2 wires them to NOMAD tools.
