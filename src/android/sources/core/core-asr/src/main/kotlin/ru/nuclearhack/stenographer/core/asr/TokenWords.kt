package ru.nuclearhack.stenographer.core.asr

import ru.nuclearhack.stenographer.core.domain.engine.RecognizedWord
import kotlin.math.roundToLong

/** Шаг кадра энкодера GigaAM: hop 10 мс × subsampling 4. Таймкод токена — начало его кадра. */
internal const val FRAME_MS = 40L

/** Маркер начала слова в SentencePiece (e2e-варианты). */
private const val WORD_MARK = '▁'

private const val UNKNOWN_TOKEN = "<unk>"

private const val MS_PER_SECOND = 1_000f

/**
 * Собирает слова из токенов sherpa-onnx. Понимает оба словаря GigaAM-v3:
 * - SentencePiece: слово начинается с токена `▁…`, пунктуация (`,`, `».`) приклеивается к предыдущему;
 * - посимвольный: слова разделены токеном-пробелом.
 *
 * Начало слова — таймкод его первого токена. Конец — кадр после последнего токена, но не позже начала
 * следующего слова и конца фрагмента. Таймкоды сдвигаются на [offsetMs] — начало фрагмента в записи.
 */
internal fun tokensToWords(
    tokens: Array<String>,
    timestampsSec: FloatArray,
    offsetMs: Long,
    durationMs: Long,
): List<RecognizedWord> {
    require(tokens.size == timestampsSec.size) {
        "Токенов ${tokens.size}, а таймкодов ${timestampsSec.size}"
    }

    class Draft(val startMs: Long) {
        val text = StringBuilder()
        var lastTokenMs = startMs
    }

    val drafts = mutableListOf<Draft>()
    tokens.forEachIndexed { i, token ->
        if (token == UNKNOWN_TOKEN) return@forEachIndexed
        val timeMs = (timestampsSec[i] * MS_PER_SECOND).roundToLong().coerceIn(0, durationMs)
        val startsWord = token.startsWith(WORD_MARK) || token.startsWith(' ')
        val piece = if (startsWord) token.substring(1) else token
        val draft = drafts.lastOrNull()?.takeUnless { startsWord } ?: Draft(timeMs).also { drafts += it }
        draft.text.append(piece)
        draft.lastTokenMs = timeMs
    }

    val words = drafts.filter { it.text.isNotBlank() }
    return words.mapIndexed { i, draft ->
        val nextStartMs = words.getOrNull(i + 1)?.startMs ?: durationMs
        val endMs = (draft.lastTokenMs + FRAME_MS)
            .coerceAtMost(nextStartMs)
            .coerceAtLeast(draft.startMs + 1)
        RecognizedWord(
            text = draft.text.toString().trim(),
            startMs = offsetMs + draft.startMs,
            endMs = offsetMs + endMs,
        )
    }
}
