package ru.nuclearhack.stenographer.core.llm

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.nuclearhack.stenographer.common.models.ModelInstaller
import ru.nuclearhack.stenographer.common.models.ModelNotFoundException
import ru.nuclearhack.stenographer.core.domain.engine.EngineInfo
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.EngineUnavailableException
import ru.nuclearhack.stenographer.core.domain.engine.ProtocolGenerator
import ru.nuclearhack.stenographer.core.model.MeetingProtocol
import ru.nuclearhack.stenographer.core.model.Transcript

/**
 * Протокол встречи на LFM3-1.7B через llama.cpp (CPU).
 *
 * Расшифровка подаётся строками `спикер_N: текст` — так же, как при обучении. Запись длиннее
 * [LfmModel.CHUNK_TOKENS] режется по репликам на части, протоколы частей объединяются.
 * Модель загружается на один вызов и освобождается: в памяти телефона не держим 1 ГБ без нужды.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class LfmProtocolGenerator(
    private val modelInstaller: ModelInstaller,
) : ProtocolGenerator {

    override val info: EngineInfo
        get() = EngineInfo(
            kind = EngineKind.Llm,
            modelName = LfmModel.NAME,
            runtime = "llama.cpp (ggml, CPU)",
            isReady = modelInstaller.isAvailable(LfmModel.ASSET),
        )

    private val mutex = Mutex()

    override suspend fun generate(transcript: Transcript, onProgress: (Float) -> Unit): MeetingProtocol {
        val words = transcript.utterances.sumOf { u -> u.text.split(' ').count { it.any(Char::isLetterOrDigit) } }
        if (words < MIN_WORDS) {
            // На паре фраз маленькая модель начинает «додумывать» встречу — честнее не составлять протокол.
            throw EngineUnavailableException(EngineKind.Llm, "В записи слишком мало речи для протокола ($words сл.)")
        }
        val modelFile = try {
            modelInstaller.install(LfmModel.ASSET)
        } catch (e: ModelNotFoundException) {
            throw EngineUnavailableException(EngineKind.Llm, e.message ?: "Нет файла модели", e)
        }
        return mutex.withLock {
            withContext(Dispatchers.Default) {
                val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, MAX_THREADS)
                val handle = try {
                    NativeLlm.nativeLoad(modelFile.absolutePath, threads, LfmModel.CONTEXT_TOKENS)
                } catch (e: IllegalStateException) {
                    throw EngineUnavailableException(EngineKind.Llm, e.message ?: "Модель не загрузилась", e)
                }
                try {
                    run(handle, transcript, onProgress)
                } finally {
                    NativeLlm.nativeFree(handle)
                }
            }
        }
    }

    private suspend fun run(handle: Long, transcript: Transcript, onProgress: (Float) -> Unit): MeetingProtocol {
        val lines = transcript.utterances.map { "спикер_${speakerNumber(it.speaker.value)}: ${it.text.trim()}" }
        val chunks = chunk(handle, lines)
        val parts = mutableListOf<MeetingProtocol>()
        val ctx = currentCoroutineContext()
        chunks.forEachIndexed { index, chunkLines ->
            val base = index.toFloat() / chunks.size
            val share = 1f / chunks.size
            val answer = NativeLlm.nativeGenerate(
                handle,
                LfmModel.prompt(chunkLines.joinToString("\n")),
                LfmModel.MAX_ANSWER_TOKENS,
                object : GenerationListener {
                    // Чтение промпта ~40 % времени части, генерация ~60 % (оценка ~400 токенов ответа).
                    override fun onPrefill(done: Int, total: Int): Boolean {
                        onProgress(base + share * PREFILL_SHARE * done / total)
                        return ctx.isActive
                    }

                    override fun onToken(generated: Int): Boolean {
                        val gen = (generated.toFloat() / EXPECTED_ANSWER_TOKENS).coerceAtMost(1f)
                        onProgress(base + share * (PREFILL_SHARE + (1 - PREFILL_SHARE) * gen))
                        return ctx.isActive
                    }
                },
            )
            if (!currentCoroutineContext().isActive) return@forEachIndexed
            ProtocolJson.parse(answer)?.let(parts::add)
        }
        onProgress(1f)
        if (parts.isEmpty()) {
            throw EngineUnavailableException(EngineKind.Llm, "Модель вернула ответ не в формате протокола")
        }
        return ProtocolJson.merge(parts)
    }

    /** Реплики подряд, пока сумма токенов не превысит бюджет части. Одна длинная реплика — отдельная часть. */
    private fun chunk(handle: Long, lines: List<String>): List<List<String>> {
        val chunks = mutableListOf<MutableList<String>>()
        var budget = 0
        for (line in lines) {
            val tokens = NativeLlm.nativeCountTokens(handle, line) + 1
            if (chunks.isEmpty() || budget + tokens > LfmModel.CHUNK_TOKENS) {
                chunks += mutableListOf<String>()
                budget = 0
            }
            chunks.last() += line
            budget += tokens
        }
        return chunks
    }

    private fun speakerNumber(id: String): String = id.substringAfterLast('_')

    private companion object {
        const val MAX_THREADS = 6
        const val MIN_WORDS = 25
        const val PREFILL_SHARE = 0.4f
        const val EXPECTED_ANSWER_TOKENS = 400f
    }
}
