package ru.nuclearhack.stenographer.core.asr

import ru.nuclearhack.stenographer.common.models.ModelAsset

/**
 * Варианты GigaAM-v3 в ONNX-конвертации k2-fsa (энкодер int8). Каталоги и размеры совпадают с тем,
 * что раскладывает `tools/fetch_models.sh --asr=…`. В APK кладётся один вариант, и распознаватель
 * берёт тот, что есть на устройстве.
 *
 * Какой оставить для сдачи, зависит от нормализации текста у организаторов перед подсчётом WER:
 * e2e пишет числа цифрами и расставляет пунктуацию, rnnt выдаёт строчные слова как есть.
 */
enum class GigaAmVariant(
    val title: String,
    val punctuated: Boolean,
    dir: String,
    encoderBytes: Long,
    decoderBytes: Long,
    joinerBytes: Long,
    tokensBytes: Long,
) {
    /** С пунктуацией и нормализацией; словарь SentencePiece на 1024 токена. */
    E2eRnnt(
        title = "e2e_rnnt",
        punctuated = true,
        dir = "gigaam-v3-e2e-rnnt",
        encoderBytes = 224_570_820,
        decoderBytes = 4_600_132,
        joinerBytes = 2_712_896,
        tokensBytes = 13_354,
    ),

    /** Лучший WER по карточке модели, текст без пунктуации; словарь из 33 букв и пробела. */
    Rnnt(
        title = "rnnt",
        punctuated = false,
        dir = "gigaam-v3-rnnt",
        encoderBytes = 224_570_814,
        decoderBytes = 3_331_651,
        joinerBytes = 1_440_448,
        tokensBytes = 196,
    ),
    ;

    val encoder = ModelAsset("$dir/encoder.int8.onnx", encoderBytes)
    val decoder = ModelAsset("$dir/decoder.onnx", decoderBytes)
    val joiner = ModelAsset("$dir/joiner.onnx", joinerBytes)
    val tokens = ModelAsset("$dir/tokens.txt", tokensBytes)

    val assets: List<ModelAsset> get() = listOf(encoder, decoder, joiner, tokens)
}
