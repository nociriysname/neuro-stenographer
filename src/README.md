# Исходный код

Платформа: **mobile / android** — проект в [`android/`](android).

## Требования

- Android Studio 2026.1 (Quail) или новее — AGP 9.2.
- JDK 21 (Gradle-демон выбирает его сам по `gradle/gradle-daemon-jvm.properties`).
- Android SDK: platform 37, build-tools 36.x.
- NDK 29.0.14206865 и CMake 3.22.1 (версия AGP по умолчанию) — AGP скачивает их сам при первой сборке
  (нужна принятая лицензия SDK).

## Подготовка (один раз)

```bash
git submodule update --init --depth 1   # NeMo-Speech.cpp, llama.cpp, SentencePiece в src/android/third_party
tools/fetch_models.sh                   # веса моделей в assets, с проверкой SHA-256
```

## Сборка

```bash
cd src/android
./gradlew assembleDebug          # APK для отладки
./gradlew assembleRelease        # APK для сдачи (подписан debug-ключом)
./gradlew test                   # unit-тесты всех модулей
```

Первая сборка компилирует нативную часть (ggml, NeMo-Speech.cpp, SentencePiece) и идёт несколько минут,
дальше CMake пересобирает только изменённое.

Чтобы при разработке не пересобирать APK с весами, модель можно подложить на устройство:
`tools/fetch_models.sh --adb` кладёт её в `/sdcard/Android/data/ru.nuclearhack.stenographer/files/models/`.

Готовые APK: `app/build/outputs/apk/{debug,release}/`.

Если Android SDK не находится, создайте `src/android/local.properties` со строкой
`sdk.dir=/путь/к/Android/sdk` (Android Studio делает это сама при открытии проекта).

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

## Модели

Веса в git не хранятся: [`tools/fetch_models.sh`](../tools/fetch_models.sh) скачивает их с Hugging Face
по зафиксированной ревизии и кладёт в `assets/models` модулей. При первом запуске приложение копирует
их в `noBackupFilesDir/models` (ggml открывает модель по пути через mmap).

## Использование

1. Установить APK: `adb install app-release.apk`.
2. Нажать «Выбрать аудиофайл» и выбрать запись (wav, mp3, m4a, ogg, flac…).
3. Дождаться обработки: прогресс показывается по этапам.
4. Экспортировать результат кнопкой JSON, TXT или SRT в верхней панели.
