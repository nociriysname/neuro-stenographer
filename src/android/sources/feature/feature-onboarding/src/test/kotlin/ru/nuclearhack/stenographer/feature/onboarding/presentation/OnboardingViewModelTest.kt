package ru.nuclearhack.stenographer.feature.onboarding.presentation

import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.ModelPack
import ru.nuclearhack.stenographer.core.domain.settings.AppSettings
import ru.nuclearhack.stenographer.core.domain.settings.SettingsRepository
import ru.nuclearhack.stenographer.core.domain.usecase.GetModelStorageStatusUseCase
import ru.nuclearhack.stenographer.core.domain.usecase.PrepareModelsUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private class FakePack(
        override val kind: EngineKind,
        override val sizeBytes: Long,
        var installed: Boolean = false,
        private val available: Boolean = true,
        /** Если задан, установка ждёт его завершения. */
        val gate: CompletableDeferred<Unit>? = null,
        private val failure: Exception? = null,
    ) : ModelPack {
        override val title = kind.name

        override fun isInstalled() = installed

        override fun isAvailable() = available

        override suspend fun install(onBytesCopied: (Long) -> Unit) {
            onBytesCopied(sizeBytes / 2)
            gate?.await()
            failure?.let { throw it }
            onBytesCopied(sizeBytes - sizeBytes / 2)
            installed = true
        }
    }

    private class FakeSettings : SettingsRepository {
        private val flow = MutableStateFlow(AppSettings())
        override val settings: StateFlow<AppSettings> = flow

        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            flow.value = transform(flow.value)
        }
    }

    private val settings = FakeSettings()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(vararg packs: FakePack): OnboardingViewModel {
        val set = packs.toSet<ModelPack>()
        return OnboardingViewModel(GetModelStorageStatusUseCase(set), PrepareModelsUseCase(set), settings)
    }

    private fun OnboardingViewModel.next(times: Int = 1) = repeat(times) { onAction(OnboardingAction.Next) }

    @Test
    fun `steps go forward and back`() {
        val vm = viewModel(FakePack(EngineKind.Diarization, 100, installed = true))
        vm.next(2)
        assertEquals(OnboardingStep.Models, vm.state.value.step)
        vm.onAction(OnboardingAction.Back)
        assertEquals(OnboardingStep.Privacy, vm.state.value.step)
    }

    @Test
    fun `models already installed are done at once`() {
        val vm = viewModel(FakePack(EngineKind.Diarization, 100, installed = true))
        assertEquals(ModelsState.Done, vm.state.value.models)
        vm.next(2)
        assertTrue(vm.state.value.canProceed)
    }

    @Test
    fun `copying starts on step three and next is blocked until done`() {
        val gate = CompletableDeferred<Unit>()
        val vm = viewModel(
            FakePack(EngineKind.Diarization, 100, installed = true),
            FakePack(EngineKind.SpeechRecognition, 200, gate = gate),
        )
        vm.next(2)
        val copying = vm.state.value.models
        assertTrue(copying is ModelsState.Copying)
        assertFalse(vm.state.value.canProceed)

        vm.next()
        assertEquals(OnboardingStep.Models, vm.state.value.step)
        assertEquals(ModelRowStatus.Active, vm.state.value.rows.last().status)

        gate.complete(Unit)
        assertEquals(ModelsState.Done, vm.state.value.models)
        assertTrue(vm.state.value.rows.all { it.status == ModelRowStatus.Done })
        vm.next()
        assertEquals(OnboardingStep.Microphone, vm.state.value.step)
    }

    @Test
    fun `missing weights allow continuing`() {
        val vm = viewModel(FakePack(EngineKind.Diarization, 100, available = false))
        vm.next(2)
        assertEquals(ModelsState.Missing, vm.state.value.models)
        assertEquals(ModelRowStatus.Unavailable, vm.state.value.rows.single().status)
        vm.next()
        assertEquals(OnboardingStep.Microphone, vm.state.value.step)
    }

    @Test
    fun `failed copy can be retried`() {
        val pack = FakePack(EngineKind.Diarization, 100, failure = IOException("нет места"))
        val vm = viewModel(pack)
        vm.next(2)
        assertEquals(ModelsState.Failed, vm.state.value.models)
        assertTrue(vm.state.value.canProceed)
    }

    @Test
    fun `complete marks onboarding done once and finishes`() = runTest {
        val vm = viewModel(FakePack(EngineKind.Diarization, 100))
        vm.onAction(OnboardingAction.Complete)
        vm.onAction(OnboardingAction.Complete)
        assertTrue(settings.settings.value.onboardingCompleted)
        assertEquals(OnboardingEffect.Finish, vm.effects.first())
    }
}
