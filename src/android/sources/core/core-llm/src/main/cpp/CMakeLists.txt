cmake_minimum_required(VERSION 3.22)
project(steno_llm LANGUAGES C CXX)

# -----------------------------------------------------------------------------
# Локальная LLM (протокол встречи) на llama.cpp. Отдельная библиотека со своим, более свежим llama.cpp:
# у диаризации llama.cpp закреплён на коммите NeMo-Speech.cpp, а архитектура LFM3 появилась позже.
# Символы ggml в обеих .so скрыты (--exclude-libs,ALL), поэтому две копии ggml не конфликтуют.
# -----------------------------------------------------------------------------
if(NOT STENO_THIRD_PARTY_DIR)
    message(FATAL_ERROR "STENO_THIRD_PARTY_DIR не задан: его передаёт core-llm/build.gradle.kts")
endif()
set(LLAMA_DIR "${STENO_THIRD_PARTY_DIR}/llama.cpp-llm")
if(NOT EXISTS "${LLAMA_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "Нет исходников в ${LLAMA_DIR}. Выполните: git submodule update --init --depth 1")
endif()

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
foreach(lang C CXX)
    set(CMAKE_${lang}_FLAGS_DEBUG "-O2 -g")
endforeach()
add_compile_options(-ffunction-sections -fdata-sections)

set(BUILD_SHARED_LIBS     OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE           OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP           OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE        ON  CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL       OFF CACHE BOOL "" FORCE)
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(GGML_CPU_ARM_ARCH "armv8.2-a+dotprod+fp16" CACHE STRING "" FORCE)
elseif(ANDROID_ABI STREQUAL "x86_64")
    # Стандартный эмулятор Android (QEMU) отдаёт гостю только SSE4.2/AVX/F16C — без AVX2 и FMA,
    # даже если хост их умеет (проверено на android-36 x86_64). AVX2-сборка упала бы с SIGILL.
    foreach(opt GGML_SSE42 GGML_AVX GGML_F16C)
        set(${opt} ON CACHE BOOL "" FORCE)
    endforeach()
    foreach(opt GGML_AVX2 GGML_FMA GGML_BMI2 GGML_AVX512)
        set(${opt} OFF CACHE BOOL "" FORCE)
    endforeach()
endif()
foreach(opt LLAMA_BUILD_COMMON LLAMA_BUILD_TESTS LLAMA_BUILD_TOOLS LLAMA_BUILD_EXAMPLES LLAMA_BUILD_SERVER
            LLAMA_BUILD_APP LLAMA_CURL LLAMA_OPENSSL LLAMA_LLGUIDANCE)
    set(${opt} OFF CACHE BOOL "" FORCE)
endforeach()
add_subdirectory("${LLAMA_DIR}" "${CMAKE_BINARY_DIR}/llama.cpp" EXCLUDE_FROM_ALL)

add_library(steno_llm SHARED llm_jni.cpp)
target_link_libraries(steno_llm PRIVATE llama ggml android log)
target_link_options(steno_llm PRIVATE "LINKER:--exclude-libs,ALL" "LINKER:--gc-sections")
