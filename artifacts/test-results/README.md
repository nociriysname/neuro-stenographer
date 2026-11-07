# Артефакты тестирования

`results.csv` — одна строка на прогон одного файла. Методика — [`docs/TEST_RESULTS.md`](../../docs/TEST_RESULTS.md).

| Колонка | Значение |
|---|---|
| `processing_sec`, `rtf` | полное время обработки и RTF из `ProcessingTimings` |
| `wer`, `der` | доли от 0 до 1 |
| `boundary_err_ms` | среднее отклонение границ реплик от эталона |
| `asr_model`, `diar_model`, `llm_model` | точные версии/варианты моделей прогона |
