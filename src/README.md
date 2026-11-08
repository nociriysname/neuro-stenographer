# Исходный код

Платформа: **mobile / android** — проект в [`android/`](android).

## Требования

- Android Studio 2026.1 (Quail) или новее — AGP 9.2.
- JDK 21 — именно JDK (с `javac`), JRE недостаточно; Gradle-демон выбирает его по `gradle/gradle-daemon-jvm.properties`.
  Если JDK не находится: `org.gradle.java.installations.paths=/путь/к/jdk-21` в `~/.gradle/gradle.properties`.
- Android SDK: platform 37, build-tools 36.x/37.
- NDK 29.0.14206865 и CMake 3.22.1 (версия AGP по умолчанию) — AGP скачивает их сам при первой сборке
  (нужна принятая лицензия SDK).

## Подготовка (один раз)

```bash
git submodule update --init --depth 1   # NeMo-Speech.cpp, llama.cpp, SentencePiece в src/android/third_party
tools/fetch_models.sh                   # веса моделей, AAR sherpa-onnx и abseil, с проверкой SHA-256
```

`fetch_models.sh` скачивает: Nemotron-3-Diarization (107 МБ), GigaAM-v3 e2e_rnnt (232 МБ), модель протокола
LFM3-1.7B Q4_K_M (1.1 ГБ, [Hugging Face](https://huggingface.co/Den4ikAI/LFM3-1.7B-meeting-protocol-ru-GGUF)),
AAR sherpa-onnx и исходники abseil для SentencePiece (без них сборка диаризации не стартует: CMake не клонирует
abseil с GitHub во время сборки).

## Сборка

```bash
cd src/android
./gradlew assembleDebug          # APK для отладки
./gradlew assembleRelease        # APK для сдачи (подписан debug-ключом)
./gradlew test                   # unit-тесты всех модулей
```

Первая сборка компилирует нативную часть (ggml, NeMo-Speech.cpp, SentencePiece, llama.cpp) под две ABI —
`arm64-v8a` (телефоны) и `x86_64` (стандартный эмулятор) — и идёт несколько минут, дальше CMake пересобирает
только изменённое. Release APK весит ~1.5 ГБ: модели лежат внутри, приложение работает без интернета.

Чтобы при разработке не пересобирать APK с весами, модели можно подложить на устройство:
`tools/fetch_models.sh --adb` кладёт их в `/sdcard/Android/data/ru.nuclearhack.stenographer/files/models/`.

GigaAM-v3 бывает в двух вариантах: `e2e_rnnt` (по умолчанию, с пунктуацией) и `rnnt` (без пунктуации).
В APK попадает один: `tools/fetch_models.sh --asr=rnnt` заменяет вариант в assets.

Готовые APK: `app/build/outputs/apk/{debug,release}/`.

Если Android SDK не находится, создайте `src/android/local.properties` со строкой
`sdk.dir=/путь/к/Android/sdk` (Android Studio делает это сама при открытии проекта).

## Проверка кода

```bash
./gradlew detekt                       # статический анализ всех модулей (входит в check)
./gradlew detekt -PdetektAutoCorrect   # заодно исправить форматирование
```

Конфиг — `config/detekt/detekt.yml` (перенесён из CuApp), свои правила — модуль `detekt-rules`.
Отчёты: `<модуль>/build/reports/detekt/detekt.html`.

## Проверка движков на устройстве

Инструментальные тесты прогоняют настоящую модель на записи, которую кладут через adb (в git записей нет).
Пример для диаризации — WAV 16 кГц моно 16 бит:

```bash
./gradlew :sources:core:core-diarization:installDebugAndroidTest
adb push dialog.wav /sdcard/Android/data/ru.nuclearhack.stenographer.core.diarization.test/files/dialog.wav
adb shell am instrument -w -e class ru.nuclearhack.stenographer.core.diarization.NemotronDiarizerDeviceTest \
    ru.nuclearhack.stenographer.core.diarization.test/androidx.test.runner.AndroidJUnitRunner
```

Время, RTF и пиковая память — в `adb logcat -s StenoDiarTest`, сегменты — рядом с записью в `dialog.segments.json`.

Так же устроены `GigaAmSpeechRecognizerDeviceTest` в `core-asr` (пакет `…core.asr.test`, лог `StenoAsrTest`)
и сквозной тест всего конвейера в `:app` — на моделях из APK, результат в формате организаторов:

```bash
./gradlew :app:installDebug :app:installDebugAndroidTest
# На Android 11+ файл, положенный adb в Android/data, приложению не виден — кладём во внутреннюю папку:
adb push dialog.mp3 /data/local/tmp/dialog.mp3
adb shell "cat /data/local/tmp/dialog.mp3 | run-as ru.nuclearhack.stenographer sh -c 'mkdir -p files/test && cat > files/test/dialog.mp3'"
adb shell am instrument -w -e class ru.nuclearhack.stenographer.PipelineDeviceTest \
    -e audio /data/user/0/ru.nuclearhack.stenographer/files/test/dialog.mp3 \
    ru.nuclearhack.stenographer.test/androidx.test.runner.AndroidJUnitRunner
```

Расшифровка — в `dialog.transcript.json`, протокол встречи — в `dialog.protocol.txt` рядом с записью
(`-e protocol false` — без протокола), тайминги этапов — в `adb logcat -s StenoPipelineTest`.
Метрики против эталона: `python tools/metrics.py dialog.transcript.json reference.json` (WER, DER, ошибка границ).

## Модели

Веса в git не хранятся: [`tools/fetch_models.sh`](../tools/fetch_models.sh) скачивает их с Hugging Face
по зафиксированной ревизии и кладёт в `assets/models` модулей. При первом запуске приложение копирует
их в `noBackupFilesDir/models` (ggml открывает модель по пути через mmap).

## Установка и использование

**Установка.** Готовый APK — в [GitHub Releases](https://github.com/nociriysname/neuro-stenographer/releases/tag/v2)
(`NeiroStenograph_v2.apk`, ~1.5 ГБ, Android 9+, arm64-v8a и x86_64). Через кабель или на эмулятор:

```bash
adb install -r NeiroStenograph_v2.apk
```

Установка с телефона тоже работает, но файл 1.5 ГБ легко обрезается при пересылке через мессенджер — если
установщик пишет «Не удалось обработать пакет», сверьте SHA-256 файла с указанным в релизе.

**Первый запуск.** Онбординг из 4 шагов; на третьем приложение один раз копирует модели из APK в память
(1.37 ГБ, на эмуляторе — секунды). Разрешение на микрофон нужно только для записи, можно пропустить.

**Расшифровка записи** (сценарий проверки):

1. Главная → «Импортировать файл» → выбрать запись (MP3, M4A, WAV, OGG, FLAC, AMR…).
   Ещё способ: «Открыть в…» / «Поделиться» аудиофайлом из файлового менеджера или мессенджера.
2. Прогресс по этапам: декодирование → кто говорит → распознавание, с оценкой оставшегося времени.
3. Результат: «Диалог» (пузыри и лента спикеров) или «Стенограмма»; поиск по тексту; тап по реплике —
   проигрывание с этого места, внизу мини-плеер (скорость, пропуск тишины).
4. **Экспорт JSON** в формате ТЗ: «⋯» (Ещё) → «Экспорт» → JSON → «Сохранить в файл». Там же TXT и SRT.
5. «⋯» (Ещё) → говорящие → переименовать (имя видно в репликах и протоколе; в JSON остаются `speaker_N`).
6. **Протокол встречи** (локальная LLM): иконка протокола в верхней панели или «⋯» → «Протокол встречи».
   Около минуты на эмуляторе; краткое содержание, решения, задачи с исполнителем и сроком; копировать и экспортировать.

**Запись с микрофона:** Главная → «Записать разговор» → кнопка записи, пауза → «Готово» — сразу расшифровка.

Интернет не нужен ни на одном шаге: у приложения нет разрешения INTERNET.
