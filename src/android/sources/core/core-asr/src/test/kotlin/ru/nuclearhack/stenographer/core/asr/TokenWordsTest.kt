package ru.nuclearhack.stenographer.core.asr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.nuclearhack.stenographer.core.domain.engine.RecognizedWord

class TokenWordsTest {

    private fun words(tokens: List<String>, times: List<Double>, offsetMs: Long = 0, durationMs: Long = 10_000) =
        tokensToWords(tokens.toTypedArray(), FloatArray(times.size) { times[it].toFloat() }, offsetMs, durationMs)

    @Test
    fun `sentencepiece tokens join into words with punctuation and offset`() {
        val result = words(
            tokens = listOf("▁При", "вет", ",", "▁ко", "т", "!"),
            times = listOf(0.0, 0.08, 0.16, 0.40, 0.48, 0.52),
            offsetMs = 1_000,
            durationMs = 2_000,
        )

        assertEquals(
            listOf(
                // Конец — кадр после последнего токена (160 + 40), он раньше начала следующего слова.
                RecognizedWord("Привет,", 1_000, 1_200),
                RecognizedWord("кот!", 1_400, 1_560),
            ),
            result,
        )
    }

    @Test
    fun `character tokens are split by the space token`() {
        val result = words(
            tokens = listOf(" ", "к", "о", "т", " ", "и", " ", "п", "ё", "с"),
            times = listOf(0.0, 0.04, 0.08, 0.12, 0.2, 0.24, 0.32, 0.36, 0.4, 0.44),
        )

        assertEquals(listOf("кот", "и", "пёс"), result.map { it.text })
        assertEquals(listOf(0L, 200L, 320L), result.map { it.startMs })
    }

    @Test
    fun `standalone marker and opening quote start a word`() {
        val result = words(
            tokens = listOf("ко", "т", "▁", "е", "ж", "▁«", "Мур", "».", "▁—", "▁да"),
            times = listOf(0.0, 0.04, 0.2, 0.24, 0.28, 0.4, 0.44, 0.48, 0.6, 0.7),
        )

        assertEquals(listOf("кот", "еж", "«Мур».", "—", "да"), result.map { it.text })
    }

    @Test
    fun `word end never precedes its start even for tokens in one frame`() {
        val result = words(tokens = listOf("▁а", "▁б", "▁в"), times = listOf(0.4, 0.4, 0.4), durationMs = 400)

        assertEquals(3, result.size)
        result.forEach { assertTrue(it.endMs > it.startMs, "$it") }
    }

    @Test
    fun `unknown tokens and empty input give no words`() {
        assertEquals(emptyList(), words(tokens = emptyList(), times = emptyList()))
        assertEquals(listOf("кот"), words(listOf("<unk>", "▁кот", "<unk>"), listOf(0.0, 0.1, 0.2)).map { it.text })
    }
}
