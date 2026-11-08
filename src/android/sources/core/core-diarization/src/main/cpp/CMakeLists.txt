cmake_minimum_required(VERSION 3.22)
project(steno_diarization LANGUAGES C CXX)

# -----------------------------------------------------------------------------
# Исходники — git-сабмодули в src/android/third_party (путь передаёт build.gradle.kts).
# -----------------------------------------------------------------------------
if(NOT STENO_THIRD_PARTY_DIR)
    message(FATAL_ERROR "STENO_THIRD_PARTY_DIR не задан: его передаёт core-diarization/build.gradle.kts")
endif()
set(NEMO_DIR      "${STENO_THIRD_PARTY_DIR}/nemo-speech.cpp")
set(LLAMA_CPP_DIR "${STENO_THIRD_PARTY_DIR}/llama.cpp")
set(SPM_DIR       "${STENO_THIRD_PARTY_DIR}/sentencepiece")
foreach(dir IN ITEMS "${NEMO_DIR}" "${LLAMA_CPP_DIR}" "${SPM_DIR}")
    if(NOT EXISTS "${dir}/CMakeLists.txt")
        message(FATAL_ERROR "Нет исходников в ${dir}. Выполните в корне репозитория: "
                            "git submodule update --init --depth 1")
    endif()
endforeach()

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Debug-сборка приложения передаёт сюда CMAKE_BUILD_TYPE=Debug, а нейросеть на -O0 работает
# в десятки раз медленнее. Поэтому нативный код оптимизируется всегда, отладочная информация остаётся.
foreach(lang C CXX)
    set(CMAKE_${lang}_FLAGS_DEBUG "-O2 -g")
endforeach()
# Отдельные секции + --gc-sections: из статических библиотек в .so попадает только используемый код.
add_compile_options(-ffunction-sections -fdata-sections)

# -----------------------------------------------------------------------------
# ggml (из llama.cpp): только CPU-бэкенд, статически.
# Тот же llama.cpp позже понадобится для локальной LLM — общий ggml на оба движка.
# -----------------------------------------------------------------------------
set(BUILD_SHARED_LIBS   OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE         OFF CACHE BOOL "" FORCE)
# Базовый уровень — ARMv8.2 + dotprod + fp16: телефоны примерно с 2019 года (Snapdragon 855+)
# и эмулятор на Apple Silicon. dotprod заметно ускоряет матричные умножения в q8_0.
set(GGML_CPU_ARM_ARCH   "armv8.2-a+dotprod+fp16" CACHE STRING "" FORCE)
set(GGML_OPENMP         OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE      ON  CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL     OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
add_subdirectory("${LLAMA_CPP_DIR}/ggml" "${CMAKE_BINARY_DIR}/ggml" EXCLUDE_FROM_ALL)

# -----------------------------------------------------------------------------
# SentencePiece: самой диаризации не нужен, но ASR-код NeMo в той же библиотеке на него ссылается.
# -----------------------------------------------------------------------------
set(SPM_ENABLE_SHARED   OFF CACHE BOOL "" FORCE)
set(SPM_ENABLE_TCMALLOC OFF CACHE BOOL "" FORCE)
set(SPM_BUILD_TEST      OFF CACHE BOOL "" FORCE)
add_subdirectory("${SPM_DIR}" "${CMAKE_BINARY_DIR}/sentencepiece" EXCLUDE_FROM_ALL)

# -----------------------------------------------------------------------------
# NeMo-Speech.cpp. Их CMake опирается на CMAKE_SOURCE_DIR и не подключается через add_subdirectory,
# поэтому цели собраны здесь заново. Состав повторяет nemo_speech_common, nemo_speech_runtime_ggml
# и nemo_speech_asr + c_api из их CMakeLists при NEMO_SPEECH_BUILD_DIAR=ON и GGML_PATCHED=OFF
# (патчи NVIDIA к ggml касаются в основном CUDA).
# -----------------------------------------------------------------------------
file(STRINGS "${NEMO_DIR}/VERSION" nemo_version_line REGEX "^NEMO_SPEECH_VERSION:")
string(REGEX REPLACE "^NEMO_SPEECH_VERSION:[ \t]*" "" NEMO_VERSION "${nemo_version_line}")

add_library(nemo_common STATIC
    "${NEMO_DIR}/src/common/audio_decoder.cpp"
    "${NEMO_DIR}/src/common/audio_decoders.cpp"
    "${NEMO_DIR}/src/common/audio_file.cpp"
    "${NEMO_DIR}/src/common/audio_ingest.cpp"
    "${NEMO_DIR}/src/common/audio_miniaudio.cpp"
    "${NEMO_DIR}/src/common/audio_resampler.cpp"
    "${NEMO_DIR}/src/common/json.cpp"
    "${NEMO_DIR}/src/common/parameter_parser.cpp"
    "${NEMO_DIR}/src/common/subtitles.cpp"
)
target_include_directories(nemo_common
    PUBLIC  "${NEMO_DIR}/src/common"
    PRIVATE "${LLAMA_CPP_DIR}/vendor/miniaudio")

file(GLOB nemo_runtime_sources CONFIGURE_DEPENDS "${NEMO_DIR}/src/runtime/ggml/*.cpp")
add_library(nemo_runtime STATIC ${nemo_runtime_sources} "${LLAMA_CPP_DIR}/src/llama-mmap.cpp")
target_include_directories(nemo_runtime
    PUBLIC  "${NEMO_DIR}/src/runtime/ggml"
    PRIVATE "${NEMO_DIR}/src/common" "${LLAMA_CPP_DIR}/src" "${LLAMA_CPP_DIR}/include")
target_link_libraries(nemo_runtime PUBLIC ggml ggml-base ggml-cpu)

set(nemo_asr_dirs "" features encoder decoders vad diar postproc pnc)
set(nemo_asr_globs "")
set(nemo_asr_includes "")
foreach(sub IN LISTS nemo_asr_dirs)
    list(APPEND nemo_asr_globs "${NEMO_DIR}/src/asr/${sub}/*.cpp")
    list(APPEND nemo_asr_includes "${NEMO_DIR}/src/asr/${sub}")
endforeach()
file(GLOB nemo_asr_sources CONFIGURE_DEPENDS ${nemo_asr_globs})
add_library(nemo_asr STATIC ${nemo_asr_sources})
target_include_directories(nemo_asr
    PUBLIC  "${NEMO_DIR}/include" ${nemo_asr_includes}
    # sentencepiece_processor.h подключает "third_party/absl/...": это копия abseil в корне SentencePiece.
    PRIVATE "${SPM_DIR}/src" "${SPM_DIR}")
target_compile_definitions(nemo_asr PRIVATE NEMO_SPEECH_ASR_BUILD)
target_link_libraries(nemo_asr PUBLIC nemo_common nemo_runtime PRIVATE sentencepiece-static)

foreach(target IN ITEMS nemo_common nemo_runtime nemo_asr)
    target_compile_definitions(${target} PRIVATE NEMO_SPEECH_VERSION_STR="${NEMO_VERSION}")
endforeach()

# -----------------------------------------------------------------------------
# libsteno_diar.so — единственная библиотека, которая попадает в APK.
# whole-archive: код NeMo регистрирует компоненты статическими конструкторами, их нельзя выкидывать.
# --exclude-libs,ALL: наружу видны только JNI-функции, символы ggml/NeMo скрыты.
# -----------------------------------------------------------------------------
add_library(steno_diar SHARED diar_jni.cpp)
target_link_libraries(steno_diar PRIVATE
    -Wl,--whole-archive nemo_asr nemo_runtime nemo_common -Wl,--no-whole-archive
    android
    log)
target_link_options(steno_diar PRIVATE "LINKER:--exclude-libs,ALL" "LINKER:--gc-sections")
