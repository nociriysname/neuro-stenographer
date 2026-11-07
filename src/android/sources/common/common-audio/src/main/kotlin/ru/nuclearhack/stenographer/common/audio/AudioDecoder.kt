package ru.nuclearhack.stenographer.common.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import dev.zacsweers.metro.Inject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class UnsupportedAudioException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Декодирует аудиофайл любого формата, который умеет платформа (wav, mp3, m4a/aac, ogg/opus, flac, amr…),
 * в моно float PCM с нужной частотой дискретизации. Каналы усредняются.
 */
@Inject
class AudioDecoder(private val context: Context) {

    suspend fun decodeToMono(uri: Uri, targetSampleRate: Int): FloatArray = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw UnsupportedAudioException("В файле нет аудиодорожки")

            extractor.selectTrack(trackIndex)
            val decoded = decodeTrack(extractor, extractor.getTrackFormat(trackIndex))
            Resampler.resample(decoded.samples, decoded.sampleRate, targetSampleRate)
        } finally {
            extractor.release()
        }
    }

    private class MonoPcm(val samples: FloatArray, val sampleRate: Int)

    private suspend fun decodeTrack(extractor: MediaExtractor, format: MediaFormat): MonoPcm {
        val mime = format.getString(MediaFormat.KEY_MIME)
            ?: throw UnsupportedAudioException("Не удалось определить формат аудио")
        val codec = try {
            MediaCodec.createDecoderByType(mime)
        } catch (e: Exception) {
            throw UnsupportedAudioException("На устройстве нет декодера для $mime", e)
        }

        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var encoding = AudioFormat.ENCODING_PCM_16BIT
        val output = FloatArrayBuilder(estimateCapacity(format, sampleRate))

        try {
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                currentCoroutineContext().ensureActive()

                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val buffer = checkNotNull(codec.getInputBuffer(inputIndex))
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outputIndex >= 0 -> {
                        if (info.size > 0) {
                            val buffer = checkNotNull(codec.getOutputBuffer(outputIndex))
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            appendDownmixed(buffer.slice().order(ByteOrder.nativeOrder()), channels, encoding, output)
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }

                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = codec.outputFormat
                        sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            encoding = outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }
                    }
                }
            }
        } finally {
            runCatching { codec.stop() }
            codec.release()
        }

        return MonoPcm(output.toArray(), sampleRate)
    }

    private fun appendDownmixed(buffer: ByteBuffer, channels: Int, encoding: Int, output: FloatArrayBuilder) {
        when (encoding) {
            AudioFormat.ENCODING_PCM_16BIT -> {
                val shorts = buffer.asShortBuffer()
                repeat(shorts.remaining() / channels) {
                    var sum = 0f
                    repeat(channels) { sum += shorts.get() / 32_768f }
                    output.add(sum / channels)
                }
            }

            AudioFormat.ENCODING_PCM_FLOAT -> {
                val floats = buffer.asFloatBuffer()
                repeat(floats.remaining() / channels) {
                    var sum = 0f
                    repeat(channels) { sum += floats.get() }
                    output.add(sum / channels)
                }
            }

            else -> throw UnsupportedAudioException("Неподдерживаемая PCM-кодировка декодера: $encoding")
        }
    }

    private fun estimateCapacity(format: MediaFormat, sampleRate: Int): Int {
        val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
        return (durationUs * sampleRate / 1_000_000L).coerceIn(16_000L, Int.MAX_VALUE / 2L).toInt()
    }

    private companion object {
        const val TIMEOUT_US = 10_000L
    }
}

/** Растущий FloatArray без боксинга. */
private class FloatArrayBuilder(initialCapacity: Int) {
    private var data = FloatArray(initialCapacity)
    private var size = 0

    fun add(value: Float) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = value
    }

    fun toArray(): FloatArray = data.copyOf(size)
}
