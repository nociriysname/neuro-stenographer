# Нейро-стенографист

Android-приложение для Nuclear IT Hack (ноябрь 2026): локально, без интернета, превращает запись
разговора в расшифровку — **кто** говорил, **когда** и **что**, — показывает её и экспортирует в JSON.

| Этап | Модель | Рантайм |
|---|---|---|
| Диаризация | [nvidia/Nemotron-3-Diarization](https://huggingface.co/nvidia/Nemotron-3-Diarization) | NeMo-Speech.cpp (ggml) + JNI |
| Распознавание речи | [ai-sage/GigaAM-v3](https://huggingface.co/ai-sage/GigaAM-v3) | sherpa-onnx (ONNX Runtime) |
| Протокол встречи | [LiquidAI/LFM3-1.7B](https://huggingface.co/LiquidAI/LFM3-1.7B) + наша LoRA, Q4_K_M ([подробно](docs/LLM_PROTOCOL.md)) | llama.cpp |

Стек приложения: Kotlin, Jetpack Compose, Metro DI, Navigation 3, Clean Architecture и многомодульность.

## Статус

- [x] Каркас: модули по слоям, граф Metro, экраны выбора файла и результата, экспорт JSON/TXT/SRT.
- [x] Конвейер: декодирование → диаризация → нарезка → ASR → сборка реплик → LLM, покрыт тестами на фейковых движках.
- [x] GigaAM-v3 через sherpa-onnx: вариант e2e_rnnt, весь конвейер проверен на эмуляторе, см. [TEST_RESULTS](docs/TEST_RESULTS.md).
- [x] Nemotron-3-Diarization через NeMo-Speech.cpp (NDK): собрано, проверено на эмуляторе, см. [TEST_RESULTS](docs/TEST_RESULTS.md).
- [x] Дизайн-система «Бумага и небо» ([docs/design](docs/design/UIKIT.md)), экраны главной, расшифровки, онбординга и настроек.
- [x] Локальная LLM через llama.cpp: протокол встречи (LFM3-1.7B, дообучена на синтетике), экран S08, см. [LLM_PROTOCOL](docs/LLM_PROTOCOL.md).
- [x] Сборка arm64-v8a + x86_64, весь сценарий проверен на стандартном эмуляторе x86_64 с 4 ГБ: WER 0.8–2.7 %, DER с collar 0.4–1.4 %, RTF 0.21–0.24, см. [TEST_RESULTS](docs/TEST_RESULTS.md).
- [x] Открытие аудио из других приложений («Открыть в…», «Поделиться»).
- [x] Стенд метрик (`tools/metrics.py`: WER / DER / границы реплик) и результаты в `artifacts/test-results`.

### План функций

По приоритету. Движки выше важнее: без GigaAM и LLM показывать нечего. Список составлен по обзору
похожих приложений (Google Recorder, Samsung, Otter, Notta, Whisper Notes и др.).

1. [x] Онбординг: всё работает офлайн, готовность моделей, разрешение на микрофон.
2. [x] Запись с микрофона: WAV 16 кГц, таймер, волна, пауза, сразу расшифровка.
3. [ ] Импорт и экспорт: JSON по формату организаторов, TXT / SRT / VTT с именами спикеров.
4. [ ] История расшифровок и поиск по ней.
5. [x] Воспроизведение реплики по нажатию, подсветка звучащей реплики, скорость и пропуск тишины (подсветка отдельных слов — нет).
6. [x] Переименование спикеров (имя в репликах и протоколе). Подсказки числа спикеров перед обработкой нет.
7. [x] Протокол встречи от локальной LLM: итоги, решения, задачи.
8. [ ] Ускорение: параллельный конвейер на корутинах, оптимизация моделей (RTF).
9. [ ] Эмоции реплик с цветовой подсветкой. Подаём как подсказку, не как оценку человека.
10. [ ] Пользовательский словарь терминов и имён — если останется время.

Подробный план по этапам — в [docs/PLAN.md](docs/PLAN.md).

## Структура репозитория

Репозиторий следует [шаблону организаторов](https://github.com/grigorevmp/hacathon_template).

| Путь | Что внутри |
|---|---|
| [`src/android`](src/android) | Android-проект (Gradle), инструкция по сборке — [`src/README.md`](src/README.md) |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Архитектура, модели, технический анализ, вопросы организаторам |
| [`docs/PROTOCOL.md`](docs/PROTOCOL.md) | Формат выходного JSON и контракт конвейера |
| [`docs/LLM_PROTOCOL.md`](docs/LLM_PROTOCOL.md) | Локальная LLM: модель протоколов, данные, обучение, метрики |
| [`docs/LIMITATIONS.md`](docs/LIMITATIONS.md) | Ограничения решения |
| [`docs/TEST_RESULTS.md`](docs/TEST_RESULTS.md) | Методика и результаты собственных тестов |
| [`artifacts/`](artifacts) | Видео демонстрации и сырые результаты тестов |
| [`config/example.env`](config/example.env) | Настройки скриптов оценки |

## Для проверяющих

- **APK:** [Releases → v2](https://github.com/nociriysname/neuro-stenographer/releases/tag/v2), `NeiroStenograph_v2.apk`
  (~1.5 ГБ, модели внутри, работает без интернета; arm64-v8a и x86_64 — ставится на стандартный эмулятор).
- **Как пользоваться и где экспорт JSON:** [src/README.md → Установка и использование](src/README.md#установка-и-использование).
- **Результаты наших тестов на эмуляторе x86_64 4 ГБ:** WER 0.8–2.7 %, DER с collar 250 мс 0.4–1.4 %,
  RTF 0.21–0.25 — [TEST_RESULTS](docs/TEST_RESULTS.md).
- **Архитектура и модели:** [ARCHITECTURE](docs/ARCHITECTURE.md), локальная LLM — [LLM_PROTOCOL](docs/LLM_PROTOCOL.md).
- **Ограничения:** [LIMITATIONS](docs/LIMITATIONS.md).

## Быстрый старт

```bash
cd src/android
./gradlew test assembleDebug
```

APK: `src/android/app/build/outputs/apk/debug/app-debug.apk`. Подробнее — в [`src/README.md`](src/README.md).
