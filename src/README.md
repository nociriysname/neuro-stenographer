# Исходный код

Платформа: **mobile / android** — проект в [`android/`](android).

## Требования

- Android Studio 2026.1 (Quail) или новее — AGP 9.2.
- JDK 21 (Gradle-демон выбирает его сам по `gradle/gradle-daemon-jvm.properties`).
- Android SDK: platform 37, build-tools 36.x.
- Для нативных движков (следующий этап): NDK и CMake из SDK Manager.

## Сборка

```bash
cd src/android
./gradlew assembleDebug          # APK для отладки
./gradlew assembleRelease        # APK для сдачи (подписан debug-ключом)
./gradlew test                   # unit-тесты всех модулей
```

Готовые APK: `app/build/outputs/apk/{debug,release}/`.

Если Android SDK не находится, создайте `src/android/local.properties` со строкой
`sdk.dir=/путь/к/Android/sdk` (Android Studio делает это сама при открытии проекта).

## Модели

Веса моделей в git не хранятся. Скрипт загрузки и упаковки в APK появится вместе с интеграцией
движков — см. раздел «Поставка моделей» в [`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md).

## Использование

1. Установить APK: `adb install app-release.apk`.
2. Нажать «Выбрать аудиофайл» и выбрать запись (wav, mp3, m4a, ogg, flac…).
3. Дождаться обработки: прогресс показывается по этапам.
4. Экспортировать результат кнопкой JSON, TXT или SRT в верхней панели.
