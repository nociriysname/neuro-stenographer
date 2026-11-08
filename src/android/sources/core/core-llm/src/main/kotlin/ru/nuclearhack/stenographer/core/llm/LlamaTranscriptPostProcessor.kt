package ru.nuclearhack.stenographer.core.llm

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ru.nuclearhack.stenographer.core.domain.engine.EngineInfo
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.TranscriptPostProcessor
import ru.nuclearhack.stenographer.core.model.Transcript

/**
 * Этап постобработки расшифровки оставлен «прозрачным» намеренно.
 *
 * Организаторы попросили текст не приглаживать: его сравнивают с эталоном по WER, а пунктуацию уже ставит
 * GigaAM-v3 e2e. Локальная LLM работает отдельно — составляет протокол встречи ([LfmProtocolGenerator]).
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class LlamaTranscriptPostProcessor : TranscriptPostProcessor {

    override val info = EngineInfo(
        kind = EngineKind.Llm,
        modelName = "без изменений текста",
        runtime = "—",
        isReady = true,
    )

    override suspend fun process(transcript: Transcript): Transcript = transcript
}
