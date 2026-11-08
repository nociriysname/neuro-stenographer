#!/usr/bin/env bash
# Скачивает веса моделей и сборочные артефакты, которых нет в git, и проверяет SHA-256.
# Ревизии зафиксированы: повторный запуск даёт ровно те же файлы.
#
#   tools/fetch_models.sh               — скачать недостающее (GigaAM-v3 e2e_rnnt: с пунктуацией)
#   tools/fetch_models.sh --asr=rnnt    — GigaAM-v3 rnnt (без пунктуации) вместо e2e_rnnt
#   tools/fetch_models.sh --adb         — ещё и подложить модели на подключённое устройство (без пересборки APK)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$ROOT/src/android"
CACHE="$ROOT/.cache/downloads"
APP_ID="ru.nuclearhack.stenographer"

ASR_VARIANT="e2e_rnnt"
PUSH_ADB=0
for arg in "$@"; do
    case "$arg" in
        --adb) PUSH_ADB=1 ;;
        --asr=rnnt | --asr=e2e_rnnt) ASR_VARIANT="${arg#--asr=}" ;;
        -h | --help) sed -n '2,7p' "$0"; exit 0 ;;
        *) echo "Неизвестный аргумент: $arg (см. --help)" >&2; exit 2 ;;
    esac
done

sha256() {
    if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d' ' -f1; else shasum -a 256 "$1" | cut -d' ' -f1; fi
}

rel() { echo "${1#"$ROOT"/}"; }

# fetch <url> <файл назначения> <sha256>
fetch() {
    local url=$1 dest=$2 expected=$3
    if [[ -f "$dest" && "$(sha256 "$dest")" == "$expected" ]]; then
        echo "ok    $(rel "$dest")"
        return
    fi
    mkdir -p "$(dirname "$dest")"
    echo "fetch $(rel "$dest")"
    local progress=(-sS)
    [[ -t 2 ]] && progress=(--progress-bar)
    curl -fL --retry 3 "${progress[@]}" -o "$dest.part" "$url"
    local actual
    actual="$(sha256 "$dest.part")"
    if [[ "$actual" != "$expected" ]]; then
        rm -f "$dest.part"
        echo "SHA-256 не совпал для $url: $actual" >&2
        exit 1
    fi
    mv "$dest.part" "$dest"
}

# fetch_archive <url .tar.bz2> <sha256> <каталог назначения> <файл>...
# Архив кэшируется в .cache/downloads, в каталог назначения попадают только перечисленные файлы.
fetch_archive() {
    local url=$1 expected=$2 dest=$3
    shift 3
    local stamp="$dest/.sha256" complete=1 file
    for file in "$@"; do [[ -f "$dest/$file" ]] || complete=0; done
    if [[ $complete == 1 && -f "$stamp" && "$(cat "$stamp")" == "$expected" ]]; then
        echo "ok    $(rel "$dest")/"
        return
    fi
    local name
    name="$(basename "$url")"
    fetch "$url" "$CACHE/$name" "$expected"
    local top="${name%.tar.bz2}" members=()
    for file in "$@"; do members+=("$top/$file"); done
    rm -rf "$dest.tmp"
    mkdir -p "$dest.tmp"
    tar -xjf "$CACHE/$name" -C "$dest.tmp" --strip-components=1 "${members[@]}"
    echo "$expected" >"$dest.tmp/.sha256"
    rm -rf "$dest"
    mv "$dest.tmp" "$dest"
    echo "unpack $(rel "$dest")/"
}

# --- sherpa-onnx: рантайм GigaAM, AAR только в GitHub Releases ---------------------------------------
# Кладём в локальный Maven-репозиторий third_party/maven (подключён в settings.gradle.kts).
SHERPA_VERSION="2.0.1"
SHERPA_SHA256="07e042d97f3a6bd5b7fe74d4eef0513dc01a714b16c15a903250095aba43a575"
SHERPA_ARTIFACT="sherpa-onnx-static-link-onnxruntime"
catalog_version="$(sed -n 's/^sherpa-onnx = "\(.*\)"$/\1/p' "$ANDROID_DIR/gradle/libs.versions.toml")"
if [[ "$catalog_version" != "$SHERPA_VERSION" ]]; then
    echo "libs.versions.toml ждёт sherpa-onnx $catalog_version, а скрипт качает $SHERPA_VERSION: обновите оба места" >&2
    exit 1
fi
fetch "https://github.com/k2-fsa/sherpa-onnx/releases/download/v$SHERPA_VERSION/$SHERPA_ARTIFACT-$SHERPA_VERSION.aar" \
      "$ANDROID_DIR/third_party/maven/com/k2fsa/sherpa/onnx/$SHERPA_ARTIFACT/$SHERPA_VERSION/$SHERPA_ARTIFACT-$SHERPA_VERSION.aar" \
      "$SHERPA_SHA256"

# --- nvidia/Nemotron-3-Diarization — OpenMDW-1.1, q8_0 GGUF для NeMo-Speech.cpp --------------------
DIAR_ASSETS="$ANDROID_DIR/sources/core/core-diarization/src/main/assets/models"
fetch "https://huggingface.co/nvidia/Nemotron-3-Diarization/resolve/f667ed73aee57d40cc39428eb768b4fd87a0a29e/Nemotron-3-Diarization.q8_0.gguf" \
      "$DIAR_ASSETS/Nemotron-3-Diarization.q8_0.gguf" \
      "08456d9e22cd9a323c0364d98375f3746d6e68507ebb705cd46438c534c7a3a1"

# --- ai-sage/GigaAM-v3 — MIT, ONNX-конвертация k2-fsa (encoder int8) -------------------------------
# Каталоги в assets совпадают с GigaAmVariant в core-asr. В APK попадает только выбранный вариант.
ASR_ASSETS="$ANDROID_DIR/sources/core/core-asr/src/main/assets/models"
GIGAAM_FILES=(encoder.int8.onnx decoder.onnx joiner.onnx tokens.txt)
case "$ASR_VARIANT" in
    e2e_rnnt)
        GIGAAM_ARCHIVE="sherpa-onnx-nemo-transducer-punct-giga-am-v3-russian-2025-12-16"
        GIGAAM_SHA256="f9620a0099019c6afcee26525ef9ed3297fa50dd5691c1902af0c948fc1a470b"
        GIGAAM_DIR="gigaam-v3-e2e-rnnt"
        GIGAAM_OTHER_DIR="gigaam-v3-rnnt"
        ;;
    rnnt)
        GIGAAM_ARCHIVE="sherpa-onnx-nemo-transducer-giga-am-v3-russian-2025-12-16"
        GIGAAM_SHA256="20a439491904b839f35eb8efaa3d99cbfaaad0c6dcba22d06ff218bb0056772d"
        GIGAAM_DIR="gigaam-v3-rnnt"
        GIGAAM_OTHER_DIR="gigaam-v3-e2e-rnnt"
        ;;
esac
fetch_archive "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/$GIGAAM_ARCHIVE.tar.bz2" \
              "$GIGAAM_SHA256" "$ASR_ASSETS/$GIGAAM_DIR" "${GIGAAM_FILES[@]}"
if [[ -d "$ASR_ASSETS/$GIGAAM_OTHER_DIR" ]]; then
    rm -rf "${ASR_ASSETS:?}/$GIGAAM_OTHER_DIR"
    echo "rm    $(rel "$ASR_ASSETS/$GIGAAM_OTHER_DIR")/ (в APK остаётся один вариант GigaAM)"
fi

# Протокол встречи: LFM3-1.7B, дообученная на протоколы (LoRA), Q4_K_M. См. docs/LLM_PROTOCOL.md.
LLM_ASSETS="$ANDROID_DIR/sources/core/core-llm/src/main/assets/models"
fetch "https://huggingface.co/Den4ikAI/LFM3-1.7B-meeting-protocol-ru-GGUF/resolve/85f0f9916269bc85fbd2aea525e78b03968b0f2b/lfm3-protocol-ru-Q4_K_M.gguf" \
      "$LLM_ASSETS/lfm3-protocol-Q4_K_M.gguf" \
      "7bfeccc5cafbedf311bb314ef5248a9f0b814cde4a75181ca1436e5338b19b82"

# abseil для SentencePiece (собирается вместе с диаризацией). Кладём заранее: иначе CMake клонирует его с GitHub
# во время сборки, причём для двух ABI параллельно в одну папку (см. core-diarization/src/main/cpp/CMakeLists.txt).
ABSL_DIR="$ANDROID_DIR/third_party/sentencepiece/third_party/abseil-cpp"
ABSL_SHA256="6e1aee535473414164bf83e4ebc40240dec71a4701f8a642d906e95bea1aea0c"
if [[ -f "$ABSL_DIR/CMakeLists.txt" ]]; then
    echo "ok    $(rel "$ABSL_DIR")/"
else
    fetch "https://github.com/abseil/abseil-cpp/archive/refs/tags/20260526.0.tar.gz" \
          "$CACHE/abseil-cpp-20260526.0.tar.gz" "$ABSL_SHA256"
    mkdir -p "$ABSL_DIR"
    tar -xzf "$CACHE/abseil-cpp-20260526.0.tar.gz" -C "$ABSL_DIR" --strip-components=1
    echo "unpack $(rel "$ABSL_DIR")/"
fi

if [[ $PUSH_ADB == 1 ]]; then
    ADB="${ADB:-adb}"
    REMOTE="/sdcard/Android/data/$APP_ID/files/models"
    "$ADB" shell mkdir -p "$REMOTE/$GIGAAM_DIR"
    # Каталог, созданный adb, принадлежит shell с правами 2770, и приложение в него не заходит.
    "$ADB" shell chmod 777 "$REMOTE" "$REMOTE/$GIGAAM_DIR" 2>/dev/null || true
    "$ADB" push "$DIAR_ASSETS/Nemotron-3-Diarization.q8_0.gguf" "$REMOTE/"
    for file in "${GIGAAM_FILES[@]}"; do "$ADB" push "$ASR_ASSETS/$GIGAAM_DIR/$file" "$REMOTE/$GIGAAM_DIR/"; done
    "$ADB" push "$LLM_ASSETS/lfm3-protocol-Q4_K_M.gguf" "$REMOTE/"
fi
