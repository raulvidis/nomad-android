// OpenDroid JNI bridge for llama.cpp.
//
// Modeled on llama.cpp's own examples/llama.android/lib/.../ai_chat.cpp.
// Single global model/context/sampler — one model loaded at a time —
// matches how the Kotlin ChatManager uses it.

#include <jni.h>
#include <android/log.h>

#include <mutex>
#include <string>
#include <sstream>
#include <vector>

#include "llama.h"
#include "common.h"
#include "sampling.h"
#include "chat.h"
#include "nlohmann/json.hpp"

#define LOG_TAG    "opendroid-llm"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// ── llama.cpp log capture ────────────────────────────────────────────────
//
// llama.cpp prints failure reasons through its log callback. We forward
// them to logcat under the `llama` tag (so adb logcat shows them) and
// also buffer WARN+ lines so Kotlin can fetch the last error after a
// load returns 0.

static std::mutex  g_log_mutex;
static std::string g_last_error;
constexpr size_t   MAX_ERR_LEN = 4096;

static void log_callback(ggml_log_level level, const char * text, void * /*ud*/) {
    if (!text) return;
    int prio = ANDROID_LOG_INFO;
    if (level >= GGML_LOG_LEVEL_ERROR)      prio = ANDROID_LOG_ERROR;
    else if (level >= GGML_LOG_LEVEL_WARN)  prio = ANDROID_LOG_WARN;
    __android_log_print(prio, "llama", "%s", text);

    if (level >= GGML_LOG_LEVEL_WARN || level == GGML_LOG_LEVEL_CONT) {
        std::lock_guard<std::mutex> lock(g_log_mutex);
        g_last_error.append(text);
        if (g_last_error.size() > MAX_ERR_LEN) {
            g_last_error.erase(0, g_last_error.size() - MAX_ERR_LEN);
        }
    }
}

static std::string get_and_clear_last_error() {
    std::lock_guard<std::mutex> lock(g_log_mutex);
    std::string out;
    out.swap(g_last_error);
    while (!out.empty() && (out.back() == '\n' || out.back() == '\r' ||
                            out.back() == ' '  || out.back() == '\t')) {
        out.pop_back();
    }
    return out;
}

// ── Global engine state ──────────────────────────────────────────────────

constexpr int DEFAULT_N_CTX = 4096;
constexpr int BATCH_SIZE    = 512;
constexpr int N_THREADS     = 4;
constexpr int N_PREDICT     = 1024;

static llama_model              * g_model        = nullptr;
static llama_context            * g_ctx          = nullptr;
static llama_batch                g_batch        = {};
static common_sampler           * g_sampler      = nullptr;
static common_chat_templates_ptr  g_chat_tmpl;
static std::vector<common_chat_msg> g_chat_msgs;
static llama_pos                  g_position     = 0;
static bool                       g_initialized  = false;
static std::string                g_system_prompt; // prepended on every render

// The most recent common_chat_templates_apply() result. We hold onto it
// across a turn so nativeFinishTurnAndParse() can build matching
// parser params (format + serialized PEG arena) at EOG.
static common_chat_params         g_chat_params;

static void reset_streaming_buffers();

static void reset_engine_state() {
    if (g_sampler) { common_sampler_free(g_sampler); g_sampler = nullptr; }
    g_chat_tmpl.reset();
    if (g_batch.token != nullptr) { llama_batch_free(g_batch); g_batch = {}; }
    if (g_ctx)   { llama_free(g_ctx);          g_ctx = nullptr; }
    if (g_model) { llama_model_free(g_model);  g_model = nullptr; }
    g_chat_msgs.clear();
    g_chat_params = {};
    g_position = 0;
    reset_streaming_buffers();
}

// ── Helpers ──────────────────────────────────────────────────────────────

static bool is_valid_utf8(const std::string & s) {
    const auto * b = reinterpret_cast<const unsigned char *>(s.c_str());
    while (*b) {
        int n;
        if ((*b & 0x80) == 0)        n = 1;
        else if ((*b & 0xE0) == 0xC0) n = 2;
        else if ((*b & 0xF0) == 0xE0) n = 3;
        else if ((*b & 0xF8) == 0xF0) n = 4;
        else return false;
        b += 1;
        for (int i = 1; i < n; i++, b++) {
            if ((*b & 0xC0) != 0x80) return false;
        }
    }
    return true;
}

// ── JNI surface ──────────────────────────────────────────────────────────

extern "C" JNIEXPORT void JNICALL
Java_com_nomad_android_data_ai_LlamaBridge_nativeInit(JNIEnv *, jclass) {
    if (g_initialized) return;
    llama_log_set(log_callback, nullptr);
    llama_backend_init();
    g_initialized = true;
    LOGI("backend initialized");
}

extern "C" JNIEXPORT void JNICALL
Java_com_nomad_android_data_ai_LlamaBridge_nativeShutdown(JNIEnv *, jclass) {
    reset_engine_state();
    if (g_initialized) {
        llama_backend_free();
        g_initialized = false;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_nomad_android_data_ai_LlamaBridge_nativeGetLastError(JNIEnv * env, jclass) {
    return env->NewStringUTF(get_and_clear_last_error().c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_nomad_android_data_ai_LlamaBridge_nativeLoadModel(
        JNIEnv * env, jclass, jstring jpath, jint requested_n_ctx) {

    reset_engine_state();

    const char * path = env->GetStringUTFChars(jpath, nullptr);
    if (!path) {
        LOGE("nativeLoadModel: failed to read path string");
        return JNI_FALSE;
    }

    LOGI("loading model: %s  ctx=%d", path, requested_n_ctx);
    llama_model_params mparams = llama_model_default_params();
    mparams.use_mmap  = true;
    mparams.use_mlock = false;

    g_model = llama_model_load_from_file(path, mparams);
    env->ReleaseStringUTFChars(jpath, path);

    if (!g_model) {
        LOGE("llama_model_load_from_file returned NULL");
        return JNI_FALSE;
    }

    // Context size: 0 → DEFAULT_N_CTX; user-supplied otherwise. Cap to
    // the trained context (going higher requires RoPE scaling which we
    // don't configure).
    int n_ctx = requested_n_ctx > 0 ? (int) requested_n_ctx : DEFAULT_N_CTX;
    const int trained = llama_model_n_ctx_train(g_model);
    if (trained > 0 && n_ctx > trained) {
        LOGW("requested ctx %d > trained %d, clamping", n_ctx, trained);
        n_ctx = trained;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx           = n_ctx;
    cparams.n_batch         = BATCH_SIZE;
    cparams.n_ubatch        = BATCH_SIZE;
    cparams.n_threads       = N_THREADS;
    cparams.n_threads_batch = N_THREADS;

    g_ctx = llama_init_from_model(g_model, cparams);
    if (!g_ctx) {
        LOGE("llama_init_from_model returned NULL");
        llama_model_free(g_model); g_model = nullptr;
        return JNI_FALSE;
    }

    g_batch      = llama_batch_init(BATCH_SIZE, 0, 1);
    g_chat_tmpl  = common_chat_templates_init(g_model, "");

    common_params_sampling sparams;
    sparams.temp     = 0.7f;
    sparams.top_k    = 40;
    sparams.top_p    = 0.9f;
    sparams.min_p    = 0.05f;
    sparams.penalty_repeat = 1.1f;
    g_sampler = common_sampler_init(g_model, sparams);

    g_chat_msgs.clear();
    g_position = 0;

    char desc[256] = {0};
    llama_model_desc(g_model, desc, sizeof(desc));
    LOGI("loaded: %s (%.1f B params)", desc,
         (double) llama_model_n_params(g_model) / 1e9);

    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_nomad_android_data_ai_LlamaBridge_nativeUnloadModel(JNIEnv *, jclass) {
    reset_engine_state();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_nomad_android_data_ai_LlamaBridge_nativeIsLoaded(JNIEnv *, jclass) {
    return (g_ctx != nullptr) ? JNI_TRUE : JNI_FALSE;
}

// Submit a user prompt: format it with the chat template, tokenize,
// decode into the KV cache. After this, the engine is ready to stream
// assistant tokens via nativeNextToken.
// Per-turn streaming buffers (cleared at the start of every new prompt).
static std::string       g_cached_chars;
static std::ostringstream g_assistant_acc;

static void reset_streaming_buffers() {
    g_cached_chars.clear();
    g_assistant_acc.str("");
    g_assistant_acc.clear();
}

// Strip <think>...</think> blocks from a message so the chat template
// doesn't re-render reasoning content on subsequent turns. Some models
// (MiniCPM5, Qwen3.5, etc.) parse this themselves and choke on
// previously-emitted reasoning blobs.
static std::string strip_thinking(const std::string & s) {
    std::string out;
    out.reserve(s.size());
    size_t i = 0;
    while (i < s.size()) {
        const size_t open = s.find("<think>", i);
        if (open == std::string::npos) { out.append(s, i, std::string::npos); break; }
        out.append(s, i, open - i);
        const size_t close = s.find("</think>", open);
        if (close == std::string::npos) { i = open + 7; continue; }
        i = close + 8;
    }
    // Trim leading whitespace/newlines left behind.
    size_t start = 0;
    while (start < out.size() && (out[start] == '\n' || out[start] == '\r' ||
                                  out[start] == ' '  || out[start] == '\t')) start++;
    return out.substr(start);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_nomad_android_data_ai_LlamaBridge_nativeSubmitUserPrompt(
        JNIEnv * env, jclass, jstring jprompt) {

    if (!g_ctx) { LOGE("nativeSubmitUserPrompt: no model loaded"); return JNI_FALSE; }
    const char * cprompt = env->GetStringUTFChars(jprompt, nullptr);
    if (!cprompt) return JNI_FALSE;

    try {
        reset_streaming_buffers();

        common_chat_msg msg;
        msg.role    = "user";
        msg.content = cprompt;
        env->ReleaseStringUTFChars(jprompt, cprompt);
        g_chat_msgs.push_back(msg);

        // Render the *full* conversation. common_chat_format_single
        // (used previously) computes a diff between past-only and past+new
        // template renders; Jinja templates like MiniCPM5's render the
        // SAME past assistant message differently in those two cases
        // (because last_query_index moves), so the diff was corrupted on
        // turn 2 → garbage in KV cache → model emits EOG immediately →
        // empty response. Re-rendering the whole conversation and
        // re-decoding from a cleared cache is slower but correct.
        common_chat_templates_inputs inputs;
        inputs.add_generation_prompt = true;
        inputs.use_jinja             = true;
        inputs.enable_thinking       = true;
        // Prepend the user-configured system prompt (if any) on every
        // render so it's always present, even after a /clear that nuked
        // the chat history.
        if (!g_system_prompt.empty()) {
            common_chat_msg sys;
            sys.role    = "system";
            sys.content = g_system_prompt;
            inputs.messages.push_back(sys);
        }
        for (const auto & m : g_chat_msgs) inputs.messages.push_back(m);

        auto params = common_chat_templates_apply(g_chat_tmpl.get(), inputs);
        const std::string & formatted = params.prompt;
        if (formatted.empty()) {
            LOGE("chat template produced empty formatted string");
            return JNI_FALSE;
        }

        // Clear KV cache + sampler + position so we decode the whole
        // (re-rendered) conversation cleanly.
        llama_memory_clear(llama_get_memory(g_ctx), false);
        if (g_sampler) common_sampler_reset(g_sampler);
        g_position = 0;

        // add_special: only when the template didn't render a BOS itself
        // (it almost always does for Jinja-aware models).
        const bool has_tmpl = common_chat_templates_was_explicit(g_chat_tmpl.get());
        const bool add_special = !has_tmpl;
        auto tokens = common_tokenize(g_ctx, formatted, add_special, /*parse_special*/ true);

        if (tokens.empty()) {
            LOGE("tokenization produced 0 tokens");
            return JNI_FALSE;
        }
        const int n_ctx_max = llama_n_ctx(g_ctx);
        if ((int) tokens.size() >= n_ctx_max - 16) {
            LOGE("conversation history (%d tokens) exceeds context window (%d)",
                 (int) tokens.size(), n_ctx_max);
            std::lock_guard<std::mutex> lock(g_log_mutex);
            g_last_error = "Conversation too long for the configured context size — open Settings and pick a larger context, or clear the chat.";
            return JNI_FALSE;
        }

        for (size_t i = 0; i < tokens.size(); i += BATCH_SIZE) {
            const int cur = std::min<int>(BATCH_SIZE, tokens.size() - i);
            common_batch_clear(g_batch);
            for (int j = 0; j < cur; j++) {
                const bool want_logit = (i + j + 1 == tokens.size());
                common_batch_add(g_batch, tokens[i + j], g_position + i + j, {0}, want_logit);
            }
            if (llama_decode(g_ctx, g_batch) != 0) {
                LOGE("llama_decode failed during prompt");
                return JNI_FALSE;
            }
        }
        g_position += (llama_pos) tokens.size();
        return JNI_TRUE;
    } catch (const std::exception & e) {
        LOGE("nativeSubmitUserPrompt threw: %s", e.what());
        std::lock_guard<std::mutex> lock(g_log_mutex);
        g_last_error = std::string("Native exception: ") + e.what();
        return JNI_FALSE;
    } catch (...) {
        LOGE("nativeSubmitUserPrompt threw unknown exception");
        std::lock_guard<std::mutex> lock(g_log_mutex);
        g_last_error = "Native exception (unknown type)";
        return JNI_FALSE;
    }
}

// Sample one token. Returns the decoded UTF-8 fragment, "" if the token
// expanded into invalid UTF-8 (caller should re-call to accumulate), or
// null when the model emits EOG (end-of-generation).
extern "C" JNIEXPORT jstring JNICALL
Java_com_nomad_android_data_ai_LlamaBridge_nativeNextToken(JNIEnv * env, jclass) {
    if (!g_ctx || !g_sampler) return nullptr;

    try {
        // Hard stop near context limit; the model is responsible for
        // emitting EOG before then but we guard against runaway
        // generations.
        if (g_position >= llama_n_ctx(g_ctx) - 4) {
            LOGW("context full, stopping generation");
            if (!g_cached_chars.empty()) g_assistant_acc << g_cached_chars;
            common_chat_msg done;
            done.role    = "assistant";
            done.content = g_assistant_acc.str();
            g_chat_msgs.push_back(done);
            reset_streaming_buffers();
            return nullptr;
        }

        llama_token tok = common_sampler_sample(g_sampler, g_ctx, -1);
        common_sampler_accept(g_sampler, tok, true);

        if (llama_vocab_is_eog(llama_model_get_vocab(g_model), tok)) {
            if (!g_cached_chars.empty()) g_assistant_acc << g_cached_chars;
            common_chat_msg done;
            done.role    = "assistant";
            // Save raw model output (including <think>...</think>). Since
            // nativeSubmitUserPrompt re-renders the full conversation
            // via the chat template each turn, the template handles the
            // reasoning content extraction itself — we don't need to
            // strip here.
            done.content = g_assistant_acc.str();
            g_chat_msgs.push_back(done);
            reset_streaming_buffers();
            return nullptr;
        }

        common_batch_clear(g_batch);
        common_batch_add(g_batch, tok, g_position, {0}, true);
        if (llama_decode(g_ctx, g_batch) != 0) {
            LOGE("llama_decode failed for generated token");
            return nullptr;
        }
        g_position++;

        g_cached_chars += common_token_to_piece(g_ctx, tok);
        if (is_valid_utf8(g_cached_chars)) {
            std::string chunk = g_cached_chars;
            g_assistant_acc << chunk;
            g_cached_chars.clear();
            return env->NewStringUTF(chunk.c_str());
        } else {
            // Mid-codepoint — defer emit until we have complete bytes.
            return env->NewStringUTF("");
        }
    } catch (const std::exception & e) {
        LOGE("nativeNextToken threw: %s", e.what());
        std::lock_guard<std::mutex> lock(g_log_mutex);
        g_last_error = std::string("Native exception: ") + e.what();
        reset_streaming_buffers();
        return nullptr;
    } catch (...) {
        LOGE("nativeNextToken threw unknown exception");
        std::lock_guard<std::mutex> lock(g_log_mutex);
        g_last_error = "Native exception (unknown type)";
        reset_streaming_buffers();
        return nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_nomad_android_data_ai_LlamaBridge_nativeSetSystemPrompt(
        JNIEnv * env, jclass, jstring jprompt) {
    if (jprompt == nullptr) {
        g_system_prompt.clear();
        return;
    }
    const char * p = env->GetStringUTFChars(jprompt, nullptr);
    g_system_prompt = (p != nullptr) ? p : "";
    if (p) env->ReleaseStringUTFChars(jprompt, p);
    LOGI("system prompt set (%zu chars)", g_system_prompt.size());
}

extern "C" JNIEXPORT void JNICALL
Java_com_nomad_android_data_ai_LlamaBridge_nativeResetConversation(JNIEnv *, jclass) {
    if (g_ctx)     llama_memory_clear(llama_get_memory(g_ctx), false);
    if (g_sampler) common_sampler_reset(g_sampler);
    g_chat_msgs.clear();
    g_chat_params = {};
    g_position = 0;
    reset_streaming_buffers();
}

// ── Tool-calling JNI surface ──────────────────────────────────────────────
//
// These extend the chat flow to support the model calling external tools.
// Flow per user turn:
//   1) nativeSubmitTurn(prompt, attachmentsJson, toolsJson)
//        — appends user msg (if prompt non-empty), renders full convo with
//          tools wired into the template inputs, decodes prompt
//   2) loop nativeNextToken() until null   (same as today)
//   3) nativeFinishTurnAndParse() -> JSON  (parses the raw assistant msg
//        that nativeNextToken just pushed; replaces it with a structured
//        common_chat_msg carrying tool_calls; returns the parsed shape)
//   4) for each tool call: app executes it, then calls
//      nativeAppendToolResult(callId, name, content)
//   5) nativeSubmitTurn("", "", toolsJson) — empty prompt means "continue
//      from existing g_chat_msgs without appending a user message", which
//      lets the model see the tool results and respond.
//
// All entry points keep the try/catch -> g_last_error contract from the
// rest of this file. No C++ exceptions cross the JNI boundary.

// Append a "[attachments: ...]" suffix to a user message if there are any.
// Format: "<original>\n\n[attachments: id=<u> name=<n> mime=<m> size=<s>; ...]"
static std::string apply_attachments_suffix(const std::string & prompt, const std::string & attachmentsJson) {
    if (attachmentsJson.empty()) return prompt;
    try {
        using nlohmann::json;
        const auto arr = json::parse(attachmentsJson);
        if (!arr.is_array() || arr.empty()) return prompt;
        std::ostringstream out;
        out << prompt << "\n\n[attachments: ";
        bool first = true;
        for (const auto & a : arr) {
            if (!first) out << "; ";
            first = false;
            out << "id=" << a.value("id", "") << " name=" << a.value("name", "")
                << " mime=" << a.value("mime", "") << " size=" << a.value("size", (uint64_t)0);
        }
        out << "]";
        return out.str();
    } catch (...) {
        // Malformed attachmentsJson -> don't crash, just emit prompt as-is.
        return prompt;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_nomad_android_data_ai_LlamaBridge_nativeSubmitTurn(
        JNIEnv * env, jclass,
        jstring jprompt, jstring jattachments, jstring jtools) {

    if (!g_ctx) { LOGE("nativeSubmitTurn: no model loaded"); return JNI_FALSE; }

    const char * cprompt = jprompt ? env->GetStringUTFChars(jprompt, nullptr) : "";
    const char * cattach = jattachments ? env->GetStringUTFChars(jattachments, nullptr) : "";
    const char * ctools  = jtools ? env->GetStringUTFChars(jtools, nullptr) : "";
    std::string prompt = cprompt ? cprompt : "";
    std::string attachments = cattach ? cattach : "";
    std::string tools = ctools ? ctools : "";
    if (jprompt && cprompt)         env->ReleaseStringUTFChars(jprompt, cprompt);
    if (jattachments && cattach)    env->ReleaseStringUTFChars(jattachments, cattach);
    if (jtools && ctools)           env->ReleaseStringUTFChars(jtools, ctools);

    try {
        reset_streaming_buffers();

        // Append user msg only when there's actually a prompt — the
        // continue-after-tool-results path comes in with empty prompt.
        if (!prompt.empty()) {
            common_chat_msg msg;
            msg.role    = "user";
            msg.content = apply_attachments_suffix(prompt, attachments);
            g_chat_msgs.push_back(msg);
        }

        common_chat_templates_inputs inputs;
        inputs.add_generation_prompt = true;
        inputs.use_jinja             = true;
        inputs.enable_thinking       = true;

        // Parse the tools JSON (OpenAI-compatible: [{name,description,parameters},...])
        // into common_chat_tool vector. Empty tools -> no tool support, identical
        // to the pre-tools render path.
        if (!tools.empty() && tools != "[]") {
            try {
                // common_chat_tools_parse_oaicompat wants ordered_json
                // (the project-wide alias). nlohmann::json::parse via
                // ordered_json keeps key order which some templates rely on.
                inputs.tools = common_chat_tools_parse_oaicompat(
                    nlohmann::ordered_json::parse(tools));
            } catch (const std::exception & e) {
                LOGW("toolsJson parse failed (%s); rendering without tools", e.what());
            }
        }

        if (!g_system_prompt.empty()) {
            common_chat_msg sys;
            sys.role    = "system";
            sys.content = g_system_prompt;
            inputs.messages.push_back(sys);
        }
        for (const auto & m : g_chat_msgs) inputs.messages.push_back(m);

        // Stash the apply result on a file-scope global so
        // nativeFinishTurnAndParse can build matching parser params later.
        g_chat_params = common_chat_templates_apply(g_chat_tmpl.get(), inputs);
        const std::string & formatted = g_chat_params.prompt;
        if (formatted.empty()) {
            LOGE("chat template produced empty formatted string");
            return JNI_FALSE;
        }

        llama_memory_clear(llama_get_memory(g_ctx), false);
        if (g_sampler) common_sampler_reset(g_sampler);
        g_position = 0;

        const bool has_tmpl = common_chat_templates_was_explicit(g_chat_tmpl.get());
        const bool add_special = !has_tmpl;
        auto tokens = common_tokenize(g_ctx, formatted, add_special, /*parse_special*/ true);

        if (tokens.empty()) {
            LOGE("tokenization produced 0 tokens");
            return JNI_FALSE;
        }
        const int n_ctx_max = llama_n_ctx(g_ctx);
        if ((int) tokens.size() >= n_ctx_max - 16) {
            LOGE("conversation history (%d tokens) exceeds context window (%d)",
                 (int) tokens.size(), n_ctx_max);
            std::lock_guard<std::mutex> lock(g_log_mutex);
            g_last_error = "Conversation too long for the configured context size — open Settings and pick a larger context, or clear the chat.";
            return JNI_FALSE;
        }

        for (size_t i = 0; i < tokens.size(); i += BATCH_SIZE) {
            const int cur = std::min<int>(BATCH_SIZE, tokens.size() - i);
            common_batch_clear(g_batch);
            for (int j = 0; j < cur; j++) {
                const bool want_logit = (i + j + 1 == tokens.size());
                common_batch_add(g_batch, tokens[i + j], g_position + i + j, {0}, want_logit);
            }
            if (llama_decode(g_ctx, g_batch) != 0) {
                LOGE("llama_decode failed during prompt");
                return JNI_FALSE;
            }
        }
        g_position += (llama_pos) tokens.size();
        return JNI_TRUE;
    } catch (const std::exception & e) {
        LOGE("nativeSubmitTurn threw: %s", e.what());
        std::lock_guard<std::mutex> lock(g_log_mutex);
        g_last_error = std::string("Native exception: ") + e.what();
        return JNI_FALSE;
    } catch (...) {
        LOGE("nativeSubmitTurn threw unknown exception");
        std::lock_guard<std::mutex> lock(g_log_mutex);
        g_last_error = "Native exception (unknown type)";
        return JNI_FALSE;
    }
}

// Parse the just-completed assistant turn into a structured message.
//
// Precondition: nativeNextToken just returned null (EOG), which means the
// raw assistant content has already been pushed to g_chat_msgs.back() with
// role="assistant". We re-parse that raw content through the chat
// template's PEG parser and REPLACE the back entry with the structured
// version (preserving content + populating tool_calls when present).
// Returns a JSON string: {"content":"...","tool_calls":[{id,name,arguments},...]}.
extern "C" JNIEXPORT jstring JNICALL
Java_com_nomad_android_data_ai_LlamaBridge_nativeFinishTurnAndParse(JNIEnv * env, jclass) {
    try {
        if (g_chat_msgs.empty() || g_chat_msgs.back().role != "assistant") {
            // Nothing to parse — return a benign empty result.
            return env->NewStringUTF("{\"content\":\"\",\"tool_calls\":[]}");
        }
        const std::string raw = g_chat_msgs.back().content;

        common_chat_parser_params pp;
        pp.format            = g_chat_params.format;
        pp.generation_prompt = g_chat_params.generation_prompt;
        pp.parse_tool_calls  = true;
        if (!g_chat_params.parser.empty()) {
            pp.parser.load(g_chat_params.parser);
        }

        common_chat_msg parsed = common_chat_parse(raw, /*is_partial*/ false, pp);
        if (parsed.role.empty()) parsed.role = "assistant";
        // Assign stable ids for any tool calls that came back without one.
        std::vector<std::string> ids_cache;
        int seq = 0;
        parsed.set_tool_call_ids(ids_cache, [&seq]() { return std::to_string(seq++); });

        // Replace the raw-content back-entry with the structured version
        // so the next template render sees the proper assistant msg with
        // tool_calls (the template needs that to format e.g. the
        // <|tool_call|>...<|tool_call_end|> tokens correctly).
        g_chat_msgs.back() = parsed;

        nlohmann::json out;
        out["content"] = parsed.content;
        out["tool_calls"] = nlohmann::json::array();
        for (const auto & c : parsed.tool_calls) {
            out["tool_calls"].push_back({
                {"id", c.id},
                {"name", c.name},
                {"arguments", c.arguments},
            });
        }
        const std::string s = out.dump();
        return env->NewStringUTF(s.c_str());
    } catch (const std::exception & e) {
        LOGE("nativeFinishTurnAndParse threw: %s", e.what());
        std::lock_guard<std::mutex> lock(g_log_mutex);
        g_last_error = std::string("Native exception: ") + e.what();
        // Fallback: return what raw content we have, no tool calls.
        nlohmann::json out;
        out["content"] = g_chat_msgs.empty() ? "" : g_chat_msgs.back().content;
        out["tool_calls"] = nlohmann::json::array();
        const std::string s = out.dump();
        return env->NewStringUTF(s.c_str());
    } catch (...) {
        LOGE("nativeFinishTurnAndParse threw unknown");
        std::lock_guard<std::mutex> lock(g_log_mutex);
        g_last_error = "Native exception (unknown type)";
        return env->NewStringUTF("{\"content\":\"\",\"tool_calls\":[]}");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_nomad_android_data_ai_LlamaBridge_nativeAppendToolResult(
        JNIEnv * env, jclass,
        jstring jcallId, jstring jname, jstring jcontent) {
    const char * ccall    = jcallId ? env->GetStringUTFChars(jcallId, nullptr) : "";
    const char * cname    = jname    ? env->GetStringUTFChars(jname, nullptr)    : "";
    const char * ccontent = jcontent ? env->GetStringUTFChars(jcontent, nullptr) : "";
    try {
        common_chat_msg msg;
        msg.role         = "tool";
        msg.tool_call_id = ccall ? ccall : "";
        msg.tool_name    = cname ? cname : "";
        msg.content      = ccontent ? ccontent : "";
        g_chat_msgs.push_back(msg);
    } catch (const std::exception & e) {
        LOGE("nativeAppendToolResult threw: %s", e.what());
    } catch (...) {
        LOGE("nativeAppendToolResult threw unknown");
    }
    if (jcallId && ccall)       env->ReleaseStringUTFChars(jcallId, ccall);
    if (jname && cname)         env->ReleaseStringUTFChars(jname, cname);
    if (jcontent && ccontent)   env->ReleaseStringUTFChars(jcontent, ccontent);
}
