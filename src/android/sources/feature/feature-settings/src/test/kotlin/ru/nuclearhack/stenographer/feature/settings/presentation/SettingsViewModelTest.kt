package ru.nuclearhack.stenographer.feature.settings.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.nuclearhack.stenographer.common.uikit.format.formatMegabytes
import ru.nuclearhack.stenographer.core.domain.engine.EngineInfo
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.ModelPack
import ru.nuclearhack.stenographer.core.domain.engine.RecognizedWord
import ru.nuclearhack.stenographer.core.domain.engine.SpeechRecognizer
import ru.nuclearhack.stenographer.core.domain.settings.AppSettings
import ru.nuclearhack.stenographer.core.domain.settings.SettingsRepository
import ru.nuclearhack.stenographer.core.domain.settings.ThemeMode
import ru.nuclearhack.stenographer.core.domain.usecase.GetModelStorageStatusUseCase
import ru.nuclearhack.stenographer.core.domain.usecase.PrepareModelsUseCase
import ru.nuclearhack.stenographer.core.model.PcmAudio
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private class FakeSettings(initial: AppSettings = AppSettings()) : SettingsRepository {
        private val flow = MutableStateFlow(initial)
        override val settings: StateFlow<AppSettings> = flow

        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            flow.value = transform(flow.value)
        }
    }

    private class FakeRecognizer(
        private val punctuated: Boolean = true,
        private val plain: Boolean = true,
    ) : SpeechRecognizer {
        override val info = EngineInfo(EngineKind.SpeechRecognition, "fake", "fake", isReady = true)

        override suspend fun recognize(audio: PcmAudio, offsetMs: Long): List<RecognizedWord> = emptyList()

        override fun supportsPunctuationMode(punctuated: Boolean) = if (punctuated) this.punctuated else plain
    }

    private class FakePack(
        override val kind: EngineKind,
        override val sizeBytes: Long,
        private var installed: Boolean,
        private val available: Boolean = true,
    ) : ModelPack {
        override val title = kind.name

        override fun isInstalled() = installed

        override fun isAvailable() = available || installed

        override suspend fun install(onBytesCopied: (Long) -> Unit) {
            onBytesCopied(sizeBytes)
            installed = true
        }
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        settings: FakeSettings = FakeSettings(),
        recognizer: FakeRecognizer = FakeRecognizer(),
        packs: Set<ModelPack> = setOf(FakePack(EngineKind.SpeechRecognition, 3L * 1024 * 1024, installed = true)),
    ) = SettingsViewModel(settings, recognizer, GetModelStorageStatusUseCase(packs), PrepareModelsUseCase(packs))

    @Test
    fun `theme and emotions are written to the repository and reflected in state`() = runTest {
        val settings = FakeSettings()
        val vm = viewModel(settings)

        vm.onAction(SettingsAction.SelectTheme(ThemeMode.Dark))
        vm.onAction(SettingsAction.SetHighlightEmotions(false))

        assertEquals(ThemeMode.Dark, settings.settings.value.themeMode)
        assertFalse(settings.settings.value.highlightEmotions)
        assertEquals(ThemeMode.Dark, vm.state.value.themeMode)
        assertFalse(vm.state.value.highlightEmotions)
    }

    @Test
    fun `unavailable punctuation mode cannot be selected`() = runTest {
        val settings = FakeSettings(AppSettings(punctuation = true))
        val vm = viewModel(settings, FakeRecognizer(punctuated = true, plain = false))

        vm.onAction(SettingsAction.SelectPunctuation(false))

        assertTrue(settings.settings.value.punctuation)
        assertFalse(vm.state.value.plainAvailable)
    }

    @Test
    fun `effective punctuation falls back to the installed variant`() = runTest {
        val vm = viewModel(
            settings = FakeSettings(AppSettings(punctuation = true)),
            recognizer = FakeRecognizer(punctuated = false, plain = true),
        )

        assertEquals(false, vm.state.value.effectivePunctuation)
    }

    @Test
    fun `check files installs missing weights and reports success`() = runTest {
        val asr = FakePack(EngineKind.SpeechRecognition, 4L * 1024 * 1024, installed = false)
        val vm = viewModel(packs = setOf(asr))
        assertFalse(vm.state.value.storage.allReady)

        vm.onAction(SettingsAction.CheckFiles)

        assertTrue(vm.state.value.storage.allReady)
        assertFalse(vm.state.value.checking)
        val message = assertIs<SettingsEffect.ShowMessage>(vm.effects.first())
        assertEquals("Все модели на месте", message.text)
        assertTrue(message.success)
    }

    @Test
    fun `check files names the weights that are not found`() = runTest {
        val asr = FakePack(EngineKind.SpeechRecognition, 4L, installed = false, available = false)
        val vm = viewModel(packs = setOf(asr))

        vm.onAction(SettingsAction.CheckFiles)

        val message = assertIs<SettingsEffect.ShowMessage>(vm.effects.first())
        assertEquals("Не найдены веса: SpeechRecognition", message.text)
        assertFalse(message.success)
        assertFalse(vm.state.value.checking)
    }

    @Test
    fun `size is rounded up to megabytes`() {
        assertEquals("412\u00A0МБ", formatMegabytes(412L * 1024 * 1024))
        assertEquals("1\u00A0МБ", formatMegabytes(1))
        assertEquals("0\u00A0МБ", formatMegabytes(0))
    }
}
