package ru.nuclearhack.stenographer.core.llm

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ru.nuclearhack.stenographer.core.domain.engine.EngineInfo
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.EngineUnavailableException
import ru.nuclearhack.stenographer.core.domain.engine.TranscriptPostProcessor
import ru.nuclearhack.stenographer.core.model.Transcript

/**
 * Постобработка локальной LLM через llama.cpp (GGUF, 1–2B instruct, Q4).
 *
 * Пока не подключено. Реализация обязана соблюдать контракт [TranscriptPostProcessor]:
 * сравнивать нормализованные слова до и после и откатывать реплику, если LLM поменяла слова.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class LlamaTranscriptPostProcessor : TranscriptPostProcessor {

    override val info = EngineInfo(
        kind = EngineKind.Llm,
        modelName = "выбирается по замерам (1–2B instruct, Q4_K_M)",
        runtime = "llama.cpp",
        isReady = false,
    )

    override suspend fun process(transcript: Transcript): Transcript =
        throw EngineUnavailableException(info.kind, "Локальная LLM ещё не подключена к приложению")
}
