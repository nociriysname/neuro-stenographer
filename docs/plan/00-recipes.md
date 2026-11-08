# Общие рецепты

Здесь собраны шаги, которые повторяются почти в каждом этапе. В описаниях этапов на них ссылаются: «см. рецепт Р3».
Пути даны от корня репозитория. Gradle запускается из `src/android`.

## Р0. Как устроен проект за 5 минут

```
src/android
├── app/                      точка сборки: MainActivity, граф Metro, навигация (только здесь фичи связаны)
├── build-logic/convention/   convention-плагины: steno.android.library, .feature, .compose, .detekt
├── detekt-rules/             свои правила detekt
├── config/detekt/detekt.yml  конфиг detekt
├── third_party/              подмодули: llama.cpp, nemo-speech.cpp, sentencepiece; maven/ — AAR sherpa-onnx
└── sources/
    ├── common/
    │   ├── common-uikit      дизайн-система, UDF-база (UdfViewModel, CollectEffects), форматтеры
    │   ├── common-audio      декодирование аудио (MediaCodec), ресемплинг
    │   └── common-models     ModelInstaller: доставка весов из APK / adb
    ├── core/
    │   ├── core-model        чистые модели: Transcript, Utterance, PcmAudio, AudioRef (Kotlin JVM)
    │   ├── core-domain       порты (интерфейсы) и use case'ы, без Android (Kotlin JVM)
    │   ├── core-data         реализации портов: аудио, экспорт, настройки
    │   ├── core-asr          GigaAM-v3 через sherpa-onnx
    │   ├── core-diarization  Nemotron через NeMo-Speech.cpp (NDK, JNI)
    │   └── core-llm          локальная LLM (llama.cpp) — пока заглушка
    └── feature/
        ├── feature-home, feature-transcript, feature-onboarding, feature-settings
```

Правила зависимостей (их держат convention-плагины, не нарушайте):

- `feature-*` видит только `core-domain` и `common-uikit`. Фича **не знает**, чем реализован порт.
- `core-domain` не знает про Android: никаких `Context`, `Uri`, `android.*`.
- Реализации портов (`core-data`, `core-asr`, …) подключает только `:app`. Metro сам находит их по аннотациям.
- Фичи не знают друг о друге: переходы между экранами — колбэки, связывает их `app/navigation/StenographerNavHost.kt`.

Документы, которые стоит прочитать до начала работы: [ARCHITECTURE](../ARCHITECTURE.md), [UIKIT](../design/UIKIT.md),
[src/README](../../src/README.md) (сборка, модели, device-тесты).

## Р1. Подготовка окружения

```bash
git clone <repo> && cd neuro-stenographer
git submodule update --init --depth 1
tools/fetch_models.sh            # веса моделей + AAR sherpa-onnx, проверка SHA-256
cd src/android
./gradlew assembleDebug test detekt
```

- Android Studio 2026.1+, JDK 21, NDK и CMake 3.22.1 AGP скачает сам. **Не указывайте другую версию CMake**: на Mac
  без sdkmanager поставится только версия по умолчанию.
- Эмулятор: Pixel 9a (arm64), Android 16+. Записи для проверки кладём через `adb push` (в git их нет).
- Чтобы не пересобирать APK с весами (400+ МБ): `tools/fetch_models.sh --adb` кладёт модели на устройство,
  `ModelInstaller` возьмёт их оттуда.

## Р2. Работа с git

1. От свежего `main`: `git switch main && git pull && git switch -c feature/<этап>-<кратко>` (например
   `feature/llm-postprocess`).
2. Коммиты небольшие и законченные, сообщение на русском в повелительном или описательном стиле:
   `Запись с микрофона: foreground service и WAV-запись`.
3. Перед пушем обязательно: `./gradlew assembleDebug test detekt` — всё зелёное.
4. Pull request в `main`, в описании: что сделано, как проверить, скриншоты для UI, замеры для движков.
5. Веса моделей, записи и APK в git **не кладём** (их отсекает `.gitignore`; проверяйте `git status`).

## Р3. Новый порт и use case в домене

Порт — интерфейс в `core-domain`, который описывает, **что** нужно приложению. Реализация живёт в другом модуле.

```kotlin
// core-domain/.../repository/TranscriptRepository.kt
interface TranscriptRepository {
    fun observeAll(): Flow<List<TranscriptSummary>>
    suspend fun get(id: TranscriptId): SavedTranscript?
    suspend fun save(transcript: SavedTranscript)
    suspend fun delete(id: TranscriptId)
}
```

Use case — класс с `@Inject` и одним `operator fun invoke`. Он нужен, когда есть логика (несколько портов,
правила, преобразования). Простой проброс одного метода порта можно делать и без use case.

```kotlin
@Inject
class DeleteTranscriptUseCase(
    private val repository: TranscriptRepository,
    private val audioStorage: AudioStorage,
) {
    suspend operator fun invoke(id: TranscriptId) {
        repository.get(id)?.let { audioStorage.deleteRecording(it.audio) }
        repository.delete(id)
    }
}
```

Правила:

- Модели данных (`data class`) — в `core-model`, если их используют несколько слоёв, иначе рядом с портом.
- Никаких `android.*` в домене. Нужен файл — передавайте `AudioRef`/`DocumentRef` (строковые URI-обёртки).
- Тест на use case — в `core-domain/src/test`, с фейковыми реализациями портов (см. `TranscribeAudioUseCaseTest`,
  `PrepareModelsUseCaseTest`).

## Р4. Реализация порта и подключение через Metro

```kotlin
@Inject
@SingleIn(AppScope::class)            // один экземпляр на приложение (для репозиториев, движков)
@ContributesBinding(AppScope::class)  // «я — реализация своего единственного интерфейса»
class TranscriptRepositoryImpl(
    private val context: Context,     // Context даёт AppGraph
) : TranscriptRepository { … }
```

- Если реализаций интерфейса несколько и нужны все — `@ContributesIntoSet(AppScope::class, binding<Port>())`,
  потребитель получает `Set<Port>` (так сделаны `ModelPack`).
- Ошибка в графе (нет реализации, цикл) — это **ошибка компиляции** `:app`, а не краш.
- Модуль с реализацией должен быть в зависимостях `:app` (`app/build.gradle.kts`), иначе Metro её не увидит.
- Реализации, которые трогают диск или нативный код, делают это на `Dispatchers.IO` / `Default` внутри себя.

## Р5. Новый feature-модуль

1. Создайте `src/android/sources/feature/feature-<имя>/build.gradle.kts`:

   ```kotlin
   plugins {
       alias(libs.plugins.steno.android.feature)
   }

   android {
       namespace = "ru.nuclearhack.stenographer.feature.<имя>"
   }
   ```

   Плагин сам подключит compose, Metro, `core-domain`, `common-uikit`, lifecycle и тестовые корутины.
2. В `src/android/settings.gradle.kts` добавьте `include(":sources:feature:feature-<имя>")`.
3. В `src/android/app/build.gradle.kts` — `implementation(projects.sources.feature.feature<Имя>)`.
4. Пакет `ru.nuclearhack.stenographer.feature.<имя>.presentation` с файлами по рецепту Р6.
5. Маршрут и экран — в навигацию по рецепту Р7.

Образец — `feature-settings` (небольшой и полный: контракт, ViewModel, экран, тесты).

## Р6. Экран: Contract → ViewModel → Screen (UDF)

**Контракт** (`<Имя>Contract.kt`) — состояние, действия пользователя и одноразовые эффекты:

```kotlin
data class HistoryState(
    val query: String = "",
    val items: List<HistoryItem> = emptyList(),
    val pendingDelete: HistoryItem? = null,
)

sealed interface HistoryAction {
    data class QueryChanged(val query: String) : HistoryAction
    data class DeleteRequested(val item: HistoryItem) : HistoryAction
    data object DeleteConfirmed : HistoryAction
    data object DeleteDismissed : HistoryAction
    data class Open(val item: HistoryItem) : HistoryAction
}

sealed interface HistoryEffect {
    data class OpenTranscript(val id: TranscriptId) : HistoryEffect
    data class ShowMessage(val text: String) : HistoryEffect
}
```

**ViewModel** — наследник `UdfViewModel`, регистрируется в Metro:

```kotlin
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding<ViewModel>())   // binding<ViewModel>() обязателен
class HistoryViewModel(
    private val observeHistory: ObserveHistoryUseCase,
    private val deleteTranscript: DeleteTranscriptUseCase,
) : UdfViewModel<HistoryState, HistoryAction, HistoryEffect>(HistoryState()) {

    init {
        viewModelScope.launch {
            observeHistory().collect { items -> updateState { copy(items = items) } }
        }
    }

    override fun onAction(action: HistoryAction) {
        when (action) {
            is HistoryAction.QueryChanged -> updateState { copy(query = action.query) }
            is HistoryAction.Open -> sendEffect(HistoryEffect.OpenTranscript(action.item.id))
            …
        }
    }
}
```

**Экран** — точка входа с колбэками наружу и «чистое» содержимое для превью:

```kotlin
@Composable
fun HistoryScreen(
    onOpenTranscript: (TranscriptId) -> Unit,
    viewModel: HistoryViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is HistoryEffect.OpenTranscript -> onOpenTranscript(effect.id)
            is HistoryEffect.ShowMessage -> toasts.show(effect.text)
        }
    }
    HistoryContent(state = state, onAction = viewModel::onAction)
}

@Composable
private fun HistoryContent(state: HistoryState, onAction: (HistoryAction) -> Unit) { … }
```

Правила:

- Composable только рисует `state` и отправляет `Action`. Логика, таймеры, корутины — в ViewModel.
- Навигация, системные диалоги (SAF, разрешения), тосты — через `Effect`, не через state.
- Системные лаунчеры (`rememberLauncherForActivityResult`) живут в Screen, результат уходит в ViewModel как `Action`.
- Превью: `@Preview` светлое и тёмное на `HistoryContent` с фейковым состоянием, обёрнутое в
  `StenoThemedSurface(darkTheme = …)`. Для колбэков — `::previewNoop` (detekt запрещает пустые `{}`).
- Тест ViewModel — в `src/test` модуля, use case'ы собираются из фейковых портов, корутины — `runTest` +
  `Dispatchers.setMain(StandardTestDispatcher())`. Образец — `SettingsViewModelTest`.

## Р7. Навигация

Все маршруты — в `app/src/main/kotlin/ru/nuclearhack/stenographer/navigation/StenographerNavHost.kt`
(Navigation 3, `NavKey` + `@Serializable`):

```kotlin
@Serializable
data object HistoryRoute : NavKey

entry<HistoryRoute> {
    HistoryScreen(onOpenTranscript = { id -> backStack.add(TranscriptRoute.saved(id)) })
}
```

- Параметры маршрута — только простые типы (`String`, `Long`): маршрут сохраняется при пересоздании процесса.
- Назад — `backStack.removeLastOrNull()`. Замена экрана (как после онбординга) — `add(новый)` + `remove(старый)`.
- Нижняя навигация (этап 4) — отдельный корневой маршрут с вложенным back stack на вкладку.

## Р8. Интерфейс: только common-uikit

- Перед версткой откройте макет экрана в `docs/design/claude-design/S*.dc.html` (1 px = 1 dp, стили — `ds.css`) и
  каталог компонентов в [UIKIT.md](../design/UIKIT.md).
- В фичах запрещены Material 3, «сырые» `dp`/`sp`, `Color(0x…)`, доли и прозрачности — detekt упадёт.
  Берите `StenoTheme.spacing.*`, `StenoTheme.shapes.*`, `StenoTheme.colors.*`, `StenoTheme.typography.*` и компоненты.
- Нужен новый визуальный элемент, которого нет в uikit, и он пригодится больше чем на одном экране — добавьте
  компонент в `common-uikit/components/<пакет>/` (там dp разрешены), с KDoc и превью в обеих темах.
- Тексты интерфейса — на русском, без канцелярита. Числа и время — `formatTimestamp`, `formatMegabytes`,
  табличные цифры (`TextStyle.tabular()`).
- Зона касания ≥ 48 dp, `contentDescription` у иконочных кнопок, проверка в тёмной теме и на 360 dp.

## Р9. Нативный код (NDK, JNI)

Образец — `core-diarization`: `src/main/cpp/CMakeLists.txt`, `jni_diar.cpp`, Kotlin-обёртка `NativeDiarizer`.

- `cmake_minimum_required(VERSION 3.22)`, никаких возможностей CMake новее 3.22.
- Статические библиотеки подмодулей линкуются в одну `.so` через `-Wl,--whole-archive … -Wl,--no-whole-archive`.
- `ndkVersion` задают convention-плагины — тогда AGP вырезает символы и `.so` весит единицы мегабайт.
- ABI — только `arm64-v8a` (`abiFilters`), эмулятор тоже arm64.
- JNI-функции не держат глобального состояния: нативный объект создаётся `create…()`, возвращается как `Long`-хэндл
  и освобождается `release…()` в `finally`.
- Долгие вызовы прерываемы: Kotlin проверяет `ensureActive()` между порциями, а нативная сторона получает флаг
  отмены или колбэк прогресса.

## Р10. Модели (веса)

- Файл весов описывается `ModelAsset(fileName, sizeBytes)`. Размер проверяется при копировании — не ошибитесь.
- Скачивание при разработке — `tools/fetch_models.sh`: добавьте туда URL и SHA-256 новой модели (функция `fetch`
  или `fetch_archive`), положите файл в `src/main/assets/models/` нужного модуля, `noCompress` для расширения в
  `build.gradle.kts` модуля (`androidResources { noCompress += "gguf" }`).
- Чтобы новая модель появилась в онбординге и настройках, реализуйте `ModelPack` и пометьте
  `@ContributesIntoSet(AppScope::class, binding<ModelPack>())` (образец — `NemotronModelPack`).
- Лицензию модели проверьте до интеграции и впишите в таблицу зависимостей `docs/ARCHITECTURE.md` и в шторку
  лицензий (`feature-settings/.../LicenseEntry.kt`).

## Р11. Проверка на устройстве

```bash
cd src/android
./gradlew installDebug
adb shell monkey -p ru.nuclearhack.stenographer -c android.intent.category.LAUNCHER 1
adb push dialog.wav /sdcard/Download/          # файл появится в системном выборе файлов
adb logcat -s steno:* AndroidRuntime:E         # логи и краши
adb exec-out screencap -p > screen.png         # скриншот для PR
```

- Сбросить онбординг, не удаляя модели: `adb shell run-as ru.nuclearhack.stenographer rm shared_prefs/settings.xml`.
- Удалить распакованные модели, чтобы проверить копирование: `adb shell run-as ru.nuclearhack.stenographer rm -rf no_backup/models`.
- Device-тесты движков и конвейера — раздел «Проверка движков на устройстве» в [src/README](../../src/README.md).
- Замеры (RTF, память, WER, DER) — в `docs/TEST_RESULTS.md` и строкой в `artifacts/test-results/results.csv`.

## Р12. Определение «готово» для любой задачи

- [ ] `./gradlew assembleDebug test detekt` зелёный.
- [ ] Новая логика покрыта unit-тестами (use case, ViewModel, парсеры, форматтеры).
- [ ] UI: превью в светлой и тёмной теме, проверено на эмуляторе, скриншоты в PR.
- [ ] Движки: device-тест или ручной прогон на записи, замеры записаны.
- [ ] Обновлены документы, которых коснулось изменение (README, ARCHITECTURE, LIMITATIONS, TEST_RESULTS).
- [ ] В git нет весов, записей, APK и локальных путей.
