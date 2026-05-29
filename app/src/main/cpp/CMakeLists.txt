cmake_minimum_required(VERSION 3.22)
project(nomad_llm CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# ── llama.cpp build flags ────────────────────────────────────────────────
# Vendored as a git submodule and built from source. The key flags here:
#   BUILD_SHARED_LIBS=OFF   → static link everything into our one .so
#   GGML_BACKEND_DL=OFF     → no runtime dlopen of backend variants. The
#                             CPU backend is statically registered into
#                             libnomad_llm.so itself, so we don't need
#                             ggml_backend_load_all_from_path() or
#                             useLegacyPackaging=true to make opendir() see
#                             extracted .so files.
#   GGML_CPU_ALL_VARIANTS=OFF → one CPU variant compiled for arm64-v8a
#                               armv8.2+dotprod+i8mm (KleidiAI baseline).
#                               We give up per-device kernel selection in
#                               exchange for ~10× less native code size and
#                               a much simpler runtime.
#   GGML_NATIVE=OFF         → don't auto-detect host CPU features (host =
#                             CI machine ≠ target Android device).
#   LLAMA_BUILD_*=OFF       → no tests/examples/tools/server.
#   LLAMA_CURL=OFF          → no libcurl link; we download via OkHttp on
#                             the Kotlin side.
set(BUILD_SHARED_LIBS          OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL            OFF CACHE BOOL "" FORCE)
set(GGML_CPU_ALL_VARIANTS      OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE                OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE             OFF CACHE BOOL "" FORCE)
set(GGML_CPU                   ON  CACHE BOOL "" FORCE)
# LLAMA_BUILD_COMMON defaults to LLAMA_STANDALONE, which is FALSE when
# llama.cpp is consumed via add_subdirectory. Force it on so the
# `llama-common` target (chat templates, sampler, tokenization helpers)
# gets built.
set(LLAMA_BUILD_COMMON         ON  CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS          OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES       OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS          OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER         OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL                 OFF CACHE BOOL "" FORCE)

add_subdirectory(llama.cpp llama_build EXCLUDE_FROM_ALL)

# ── JNI bridge ───────────────────────────────────────────────────────────
add_library(nomad_llm SHARED llama-jni.cpp)
# Most include paths (common/, vendor/nlohmann/) are exported by the
# `llama-common` target's PUBLIC target_include_directories, so we only
# need to add the few llama.cpp public headers we use directly.
target_include_directories(nomad_llm PRIVATE
    ${CMAKE_SOURCE_DIR}/llama.cpp/include
    ${CMAKE_SOURCE_DIR}/llama.cpp/ggml/include
)
target_link_libraries(nomad_llm PRIVATE llama llama-common ggml android log)
