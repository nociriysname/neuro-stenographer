#!/usr/bin/env bash
# Скачивает веса моделей в assets модулей (в git они не хранятся) и проверяет SHA-256.
# Ревизии на Hugging Face зафиксированы: повторный запуск даёт ровно те же файлы.
#
#   tools/fetch_models.sh            — скачать недостающее
#   tools/fetch_models.sh --adb      — ещё и подложить на подключённое устройство (без пересборки APK)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$ROOT/src/android"
APP_ID="ru.nuclearhack.stenographer"

sha256() {
    if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d' ' -f1; else shasum -a 256 "$1" | cut -d' ' -f1; fi
}

# fetch <url> <файл назначения> <sha256>
fetch() {
    local url=$1 dest=$2 expected=$3
    if [[ -f "$dest" && "$(sha256 "$dest")" == "$expected" ]]; then
        echo "ok    ${dest#"$ROOT"/}"
        return
    fi
    mkdir -p "$(dirname "$dest")"
    echo "fetch ${dest#"$ROOT"/}"
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

DIAR_ASSETS="$ANDROID_DIR/sources/core/core-diarization/src/main/assets/models"

# nvidia/Nemotron-3-Diarization — OpenMDW-1.1, q8_0 GGUF для NeMo-Speech.cpp.
fetch "https://huggingface.co/nvidia/Nemotron-3-Diarization/resolve/f667ed73aee57d40cc39428eb768b4fd87a0a29e/Nemotron-3-Diarization.q8_0.gguf" \
      "$DIAR_ASSETS/Nemotron-3-Diarization.q8_0.gguf" \
      "08456d9e22cd9a323c0364d98375f3746d6e68507ebb705cd46438c534c7a3a1"

if [[ "${1:-}" == "--adb" ]]; then
    ADB="${ADB:-adb}"
    REMOTE="/sdcard/Android/data/$APP_ID/files/models"
    "$ADB" shell mkdir -p "$REMOTE"
    "$ADB" push "$DIAR_ASSETS/Nemotron-3-Diarization.q8_0.gguf" "$REMOTE/"
fi
