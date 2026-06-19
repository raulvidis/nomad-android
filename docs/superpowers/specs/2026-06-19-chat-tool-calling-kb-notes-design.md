# Design: model-driven KB/Notes tool-calling + thinking & tool-call UI for Nomad chat

Date: 2026-06-19
Status: Approved (ready for implementation)
Repo: `nomad-android` (Kotlin 2.0.21, Jetpack Compose + Material 3, Hilt, Room v6, single module `app/`)
Reference app: `../opendroid` (same author; on-device local-LLM chat with reasoning UI + in-chat tools). UI there is XML/Views; **Nomad is Compose — translate patterns, do not copy XML.**

## Goal

Make the Nomad chat model **decide when** to read from the knowledge base and the user's notes, instead of always-on RAG injection. Add an opendroid-style **collapsible "thinking" UI**, **interleaved tool-call cards**, and **stick-to-bottom streaming scroll** — all in Compose, in the Pip-Boy CRT aesthetic (phosphor green `#00FF41`, monospace, square corners; see `docs/design.md`).

## Approved product decisions

1. **Retrieval = tool-driven only.** Remove always-on RAG context injection. The model calls tools when it judges the question needs stored survival knowledge or the user's notes.
2. **Approval UX = auto-run, show result card.** No approve/deny gate (local, offline, read-only lookups). A collapsible tool card shows query + result.
3. **Tool set = `search_knowledge_base` + `search_notes`** (read-only). No write tools, no `read_note(id)` for now.
4. **Persistence = transient cards, persist final answer.** Thinking text + tool cards live only in the in-memory `StateFlow` during a turn. Only the final assistant text is saved to Room (as today). **No DB migration.**

## Key precondition (already in the codebase)

`LlamaBridge` (`app/src/main/java/com/nomad/android/data/ai/LlamaBridge.kt`) already exposes the full tool-call loop primitives — no native/JNI work needed:

```kotlin
suspend fun submitTurn(prompt: String, attachmentsJson: String, toolsJson: String): Result<Unit>
suspend fun streamTokens(onChunk: suspend (String) -> Unit): Result<Unit>
fun finishTurnAndParse(): ParsedTurn            // ParsedTurn(content: String, toolCalls: List<ParsedToolCall>)
fun appendToolResult(callId: String, name: String, content: String)
fun setSystemPrompt(prompt: String)
fun resetConversation()
val isModelLoaded: Boolean
```

`toolsJson` is the OpenAI-compatible array consumed by llama.cpp `common_chat`. `ParsedToolCall(id, name, arguments)` where `arguments` is a raw JSON string.

## Reference pointers in `../opendroid` (read before implementing each part)

- Agent loop / thinking parse / tool execution: `app/src/main/java/com/opendroid/core/ChatManager.kt` — `runAgentLoop` (~400-582), `splitThinking` (~659-701), `updateAssistantMessage`/`finalizeAssistantMessage` (~599-640).
- Tool layer: `app/src/main/java/com/opendroid/tools/Tool.kt`, `ToolRegistry.kt` (`describeAsJson` ~36-56), `ToolExecutor.kt`.
- XML-fallback tool-call parser: `app/src/main/java/com/opendroid/llm/LlamacppProvider.kt` — `parseFunctionXmlCalls` (~265-287).
- Scroll behavior: `app/src/main/java/com/opendroid/ui/ChatFragment.kt` — `pinStreamingBottom` (~266-277), scroll listener (~108-114), re-engage on new turn (~187).
- Default tool-aware system prompt: `ChatManager.kt` `DEFAULT_SYSTEM_PROMPT` (~50-56).

## Current Nomad anchors

- Chat UI: `app/src/main/java/com/nomad/android/ui/chat/ChatScreen.kt` (`LazyColumn` + `rememberLazyListState`, naive `animateScrollToItem(size-1)` on every message ~308-312; `MessageBubble` ~654; `StreamingMessageBubble` "Processing…" ~707).
- VM: `app/src/main/java/com/nomad/android/ui/chat/ChatViewModel.kt` — `ChatUiState`/`ChatData` (~23-67), `sendUserMessage()` (~270-420) with always-on `knowledgeBase.retrieveContext()` injection (~346-349) to be removed.
- Engines: `data/ai/AIEngine.kt`, `LlamaCppEngine.kt` (hardcoded `SYSTEM_PROMPT` ~152, `stripThinking()` ~160-177 — currently discards reasoning), `RAGEngine.kt`, `FallbackEngine.kt`, `AIEngineManager.kt`.
- KB: `data/ai/KnowledgeBase.kt` — `search(query, topK=3, categoryFilter?): List<KnowledgeEntry>`; `KnowledgeEntry(id, category, title, content, source)`.
- Notes: `data/repository/NoteRepository.kt` — `searchNotes(query): Flow<Result<List<NoteEntity>>>`; `NoteEntity(id, title, content, createdAt, updatedAt)`.

## Architecture

### 1. Tool layer — new package `data/ai/tools/`

- `ChatTool` interface: `val name`, `val description`, `val parameters: JSONObject` (JSON Schema), `suspend fun execute(args: JSONObject): ToolResult`.
- `sealed class ToolResult { data class Ok(val text: String); data class Err(val message: String) }` with `toModelString()`.
- `SearchKnowledgeBaseTool(knowledgeBase)` — params `{query: string (required), category: string (optional)}`; calls `KnowledgeBase.search(query, topK=3, categoryFilter=category)`; formats each hit as `"[<title> | <source>]\n<content>"`. Empty → `Ok("No matching knowledge-base entries.")`.
- `SearchNotesTool(noteRepository)` — params `{query: string (required)}`; `noteRepository.searchNotes(query).first()`, unwrap `Result`; format `"<title>: <content snippet (≤300 chars)>"`. Empty → `Ok("No matching notes.")`.
- `ChatToolRegistry(tools: List<ChatTool>)`: `get(name)`, `describeAsJson(): String` (OpenAI envelope `[{type:"function",function:{name,description,parameters}}]`), `suspend execute(name, argsJson): ToolResult` (parse JSON, 10s `withTimeout`, catch → `Err`).
- DI: `di/ToolModule.kt` (`@InstallIn(SingletonComponent)`) provides the two tools + the registry. `KnowledgeBase` and `NoteRepository` are already injectable.

### 2. Agent loop — new `data/ai/ChatAgent.kt` (data layer; keeps VM thin)

- Wrap `LlamaBridge` behind an injectable interface (e.g. `LlamaBridgeHandle`) so the loop is unit-testable; default impl delegates to the existing `object LlamaBridge`.
- `fun run(userText: String, history: List<ChatTurn>): Flow<AgentEvent>` (cold flow on `Dispatchers.Default`).
- `sealed class AgentEvent { AnswerDelta(text); ThinkingDelta(text); ToolCallStarted(card); ToolCallFinished(card, result, durationMs); Finished(finalText, thinking, stats); Error(message) }`.
- Loop (cap = 5 iterations):
  1. `submitTurn(prompt, "", toolsJson)` — `prompt = userText` on iter 1, `""` after.
  2. `streamTokens { chunk -> acc += chunk; val (thinking, answer) = splitThinking(acc); emit ThinkingDelta/AnswerDelta }`.
  3. `val parsed = finishTurnAndParse()`. Hedge: if `parsed.toolCalls` empty, run `parseFunctionXmlCalls(acc)` (ported) to salvage `<function>`-style calls.
  4. If no tool calls → emit `Finished`, stop.
  5. For each call: emit `ToolCallStarted`, `registry.execute(...)`, emit `ToolCallFinished`, `appendToolResult(id, name, result.toModelString())`.
  6. Loop with empty prompt.
  7. On cap hit → `Finished` with a footer note.
- Port `splitThinking` (handles `<think>…</think>`, implicit-close, GPT-OSS `<|channel|>` — keep all branches for forward-compat even though MiniCPM5 likely uses `<think>` or none).
- **System prompt** (set via `setSystemPrompt`): extend Nomad's survival-assistant prompt with tool guidance, e.g. *"You can call tools: `search_knowledge_base(query, category?)` for stored survival/first-aid/reference knowledge, and `search_notes(query)` for the user's saved notes. When a question needs that stored information, call the tool instead of guessing. Keep answers concise and practical."*
- **Fallback:** if `!isModelLoaded`, do not run the loop — delegate to existing `FallbackEngine` path (unchanged behavior).

### 3. Remove always-on RAG

- In `ChatViewModel.sendUserMessage()` delete the `knowledgeBase.retrieveContext()` injection and route sends through `ChatAgent.run(...)`.
- If `RAGEngine` / `KnowledgeBase.retrieveContext()` become unreferenced after this, delete them (repo rule: "Refactor → delete old paths by default"). `KnowledgeBase.search()` stays (used by the tool). Check for test references first.

### 4. UI (Compose) — `ui/chat/`

- Extend the **in-memory** UI message model (the `ChatMessage` used in `ChatData.messages`, not the Room entity) with: `thinkingText: String = ""`, `isThinkingExpanded: Boolean = false`, `kind: MessageKind = Normal`, `toolCall: ToolCallUi? = null`. `enum MessageKind { Normal, ToolCall }`. `ToolCallUi(name, args, status: Running|Ok|Err, resultText, durationMs)`.
- VM maps `AgentEvent` → updates to `ChatData.messages`:
  - `ThinkingDelta`/`AnswerDelta` → update current streaming assistant message; auto-expand thinking once when it first appears; do **not** collapse mid-stream.
  - `ToolCallStarted` → append a `ToolCall` message (status Running).
  - `ToolCallFinished` → update that card (status Ok/Err, result, ms); then a fresh streaming assistant bubble for the next iteration.
  - `Finished` → finalize current assistant message (collapse thinking, set stats), persist final text to Room.
- New composables (own file `ui/chat/ChatTurnComponents.kt`):
  - `ThinkingSection(thinking, expanded, streaming, onToggle)`: header `"⏳ THINKING…"` while streaming with empty answer, else `"▼ THOUGHT FOR A MOMENT"`; tap toggles; Pip-Boy styled.
  - `ToolCallCard(toolCall, expanded, onToggle)`: `🔧 <name>`, args line, status (`running…` / `✓ <ms>` / `✗ <ms>`), collapsible result. No buttons.
- `getItemViewType`-equivalent: render `ToolCall` items distinctly inside the `LazyColumn`, interleaved chronologically with bubbles.

### 5. Scrolling (Compose stick-to-bottom)

- Replace the per-message `animateScrollToItem(size-1)` with stick-to-bottom on `LazyListState`:
  - Track `autoFollowBottom` (default true). A `snapshotFlow` over `lazyListState.layoutInfo` sets it false when the user scrolls up (last item not fully visible while `isScrollInProgress`), true again when they return to bottom.
  - While streaming and `autoFollowBottom`, a `LaunchedEffect` keyed on the streaming text length keeps the last item pinned (`scrollToItem(lastIndex)` / `animateScrollBy` of the overflow).
  - Re-engage `autoFollowBottom = true` whenever a new user message is sent.
- Optional (nice-to-have, mark clearly): a scroll-to-bottom FAB shown only when `!autoFollowBottom`.

### 6. Testing (repo rule: feature → tests; CI runs `testDebugUnitTest`)

- `ChatToolRegistryTest`: `describeAsJson()` produces valid OpenAI-envelope JSON with both tools.
- `SearchKnowledgeBaseToolTest` / `SearchNotesToolTest`: `execute()` happy path + empty + bad-JSON args (mockk / Robolectric where a context is needed).
- `SplitThinkingTest`: port opendroid's cases (`<think>…</think>`, implicit close, channel, no-think).
- `ChatAgentTest`: fake `LlamaBridgeHandle` scripted to emit a tool call then a final answer → assert `AgentEvent` sequence (Started → Finished tool → Finished turn) and that `appendToolResult` was called.
- VM mapping test: `AgentEvent` stream → expected `ChatData.messages` shape.

## Risks

- **1B tool reliability**: MiniCPM5-1B may under-call tools or emit non-native call formats → mitigated by system-prompt nudging + ported `parseFunctionXmlCalls`. Accepted trade-off vs always-on RAG recall.
- **`LlamaBridge` is a singleton `object`** accessed statically by `LlamaCppEngine`; wrap behind `LlamaBridgeHandle` for the loop's testability without disturbing existing callers.
- **Streaming + persistence ordering**: keep the existing "write final text to DB before clearing streaming flag" discipline (prior bugfixes #21/#31/#35/#39) — do not regress it.

## Out of scope

Write tools, `read_note(id)`/`list_notes`, persisting thinking/cards to Room, multi-model support, attachments/images in the tool loop.

## Docs / changelog

- Update `docs/architecture.md` (AI engine stack: tool-driven retrieval replaces always-on RAG) and add a `CHANGELOG.md` `[Unreleased]` entry on landing.
