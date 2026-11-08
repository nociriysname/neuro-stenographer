package ru.nuclearhack.stenographer.core.llm

import ru.nuclearhack.stenographer.common.models.ModelAsset

/**
 * LFM3-1.7B (LiquidAI), дообученная LoRA на протоколы встреч (синтетические диалоги, разметка gpt-6.1),
 * квантизация Q4_K_M. Шаблон чата и формат ответа повторяют обучение один в один.
 */
internal object LfmModel {
    val ASSET = ModelAsset(fileName = "lfm3-protocol-Q4_K_M.gguf", sizeBytes = 1_099_627_136L)
    const val NAME = "LFM3-1.7B протоколы (LoRA), Q4_K_M"

    const val CONTEXT_TOKENS = 4096
    const val MAX_ANSWER_TOKENS = 1024

    /** Бюджет расшифровки в одном запросе: обучение шло на диалогах до ~2.5 тыс. токенов. */
    const val CHUNK_TOKENS = 2200

    private const val SYSTEM =
        "Ты — секретарь встреч. Работаешь только с переданной расшифровкой: ничего не выдумывай, " +
            "имена и сроки бери только из текста."

    private const val INSTRUCTION =
        "Составь протокол встречи. Верни JSON: {\"title\", \"summary\", \"decisions\": [...], " +
            "\"tasks\": [{\"task\", \"assignee\", \"due\"}], \"key_points\": [...]}."

    fun prompt(transcriptLines: String): String {
        val user = "Расшифровка (спикер_N — анонимные участники, имена в репликах — упоминания людей):\n\n" +
            transcriptLines + "\n\n" + INSTRUCTION
        return "<|startoftext|><|im_start|>system\n$SYSTEM<|im_end|>\n" +
            "<|im_start|>user\n$user<|im_end|>\n" +
            "<|im_start|>assistant\n<think></think>"
    }
}
