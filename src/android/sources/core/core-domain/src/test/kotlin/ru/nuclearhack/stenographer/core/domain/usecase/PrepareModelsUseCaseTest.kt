package ru.nuclearhack.stenographer.core.domain.usecase

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.ModelPack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PrepareModelsUseCaseTest {

    private class FakePack(
        override val kind: EngineKind,
        override val sizeBytes: Long,
        var installed: Boolean = false,
        private val available: Boolean = true,
    ) : ModelPack {
        var installCalls = 0
        override val title = kind.name

        override fun isInstalled() = installed

        override fun isAvailable() = available || installed

        override suspend fun install(onBytesCopied: (Long) -> Unit) {
            installCalls++
            onBytesCopied(sizeBytes / 2)
            onBytesCopied(sizeBytes - sizeBytes / 2)
            installed = true
        }
    }

    @Test
    fun `copies only missing packs and finishes with all ready`() = runTest {
        val diarization = FakePack(EngineKind.Diarization, sizeBytes = 100)
        val asr = FakePack(EngineKind.SpeechRecognition, sizeBytes = 300, installed = true)

        val events = PrepareModelsUseCase(setOf(asr, diarization))().toList()

        assertEquals(1, diarization.installCalls)
        assertEquals(0, asr.installCalls)
        val copying = events.filterIsInstance<ModelPreparation.Copying>()
        assertTrue(copying.all { it.totalBytes == 100L })
        val done = assertIs<ModelPreparation.Done>(events.last())
        assertTrue(done.status.allReady)
        assertEquals(400L, done.status.usedBytes)
        assertEquals(listOf(EngineKind.Diarization, EngineKind.SpeechRecognition), done.status.packs.map { it.kind })
    }

    @Test
    fun `reports missing weights without copying anything`() = runTest {
        val diarization = FakePack(EngineKind.Diarization, sizeBytes = 100)
        val asr = FakePack(EngineKind.SpeechRecognition, sizeBytes = 300, available = false)

        val events = PrepareModelsUseCase(setOf(diarization, asr))().toList()

        val missing = assertIs<ModelPreparation.Missing>(events.single())
        assertEquals(listOf(EngineKind.SpeechRecognition), missing.packs.map { it.kind })
        assertEquals(0, diarization.installCalls)
    }
}
