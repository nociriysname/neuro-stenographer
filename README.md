# Нейро-стенографист

Android-приложение для Nuclear IT Hack (ноябрь 2026): локально, без интернета, превращает запись
разговора в расшифровку — **кто** говорил, **когда** и **что**, — показывает её и экспортирует в JSON.

| Этап | Модель | Рантайм |
|---|---|---|
| Диаризация | [nvidia/Nemotron-3-Diarization](https://huggingface.co/nvidia/Nemotron-3-Diarization) | NeMo-Speech.cpp (ggml) + JNI |
| Распознавание речи | [ai-sage/GigaAM-v3](https://huggingface.co/ai-sage/GigaAM-v3) | sherpa-onnx (ONNX Runtime) |
| Постобработка | локальная LLM 1–2B (GGUF) | llama.cpp |

Стек приложения: Kotlin, Jetpack Compose, Metro DI, Navigation 3, Clean Architecture и многомодульность.

## Статус

- [x] Каркас: модули по слоям, граф Metro, экраны выбора файла и результата, экспорт JSON/TXT/SRT.
- [x] Конвейер: декодирование → диаризация → нарезка → ASR → сборка реплик → LLM, покрыт тестами на фейковых движках.
- [ ] GigaAM-v3 через sherpa-onnx.
- [x] Nemotron-3-Diarization через NeMo-Speech.cpp (NDK): собрано, проверено на эмуляторе, см. [TEST_RESULTS](docs/TEST_RESULTS.md).
- [ ] Локальная LLM через llama.cpp.
- [ ] Стенд метрик (WER / DER / RTF) и результаты в `artifacts/test-results`.

## Структура репозитория

Репозиторий следует [шаблону организаторов](https://github.com/grigorevmp/hacathon_template).

| Путь | Что внутри |
|---|---|
| [`src/android`](src/android) | Android-проект (Gradle), инструкция по сборке — [`src/README.md`](src/README.md) |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Архитектура, модели, технический анализ, вопросы организаторам |
| [`docs/PROTOCOL.md`](docs/PROTOCOL.md) | Формат выходного JSON и контракт конвейера |
| [`docs/LIMITATIONS.md`](docs/LIMITATIONS.md) | Ограничения решения |
| [`docs/TEST_RESULTS.md`](docs/TEST_RESULTS.md) | Методика и результаты собственных тестов |
| [`artifacts/`](artifacts) | Видео демонстрации и сырые результаты тестов |
| [`config/example.env`](config/example.env) | Настройки скриптов оценки |

## Быстрый старт

```bash
cd src/android
./gradlew test assembleDebug
```

APK: `src/android/app/build/outputs/apk/debug/app-debug.apk`. Подробнее — в [`src/README.md`](src/README.md).
