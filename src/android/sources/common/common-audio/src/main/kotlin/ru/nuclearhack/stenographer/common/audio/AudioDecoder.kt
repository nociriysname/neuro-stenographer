package ru.nuclearhack.stenographer.common.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

        val outputFormat = DecoderOutputFormat(
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
        )
        val output = FloatArrayBuilder(estimateCapacity(format, outputFormat.sampleRate))

        try {
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                currentCoroutineContext().ensureActive()
                if (!inputDone) inputDone = feedInput(codec, extractor)
                outputDone = drainOutput(codec, info, outputFormat, output)
            }
        } finally {
            runCatching { codec.stop() }
            codec.release()
        }

        return MonoPcm(output.toArray(), outputFormat.sampleRate)
    }

    /** Формат PCM на выходе декодера: может смениться после первого буфера (INFO_OUTPUT_FORMAT_CHANGED). */
    private class DecoderOutputFormat(var sampleRate: Int, var channels: Int) {
        var encoding = AudioFormat.ENCODING_PCM_16BIT

        fun update(format: MediaFormat) {
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                encoding = format.getInteger(MediaFormat.KEY_PCM_ENCODING)
            }
        }
    }

    /** Отдаёт декодеру очередной кусок файла. true — файл кончился. */
    private fun feedInput(codec: MediaCodec, extractor: MediaExtractor): Boolean {
        val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
        if (inputIndex < 0) return false
        val buffer = checkNotNull(codec.getInputBuffer(inputIndex))
        val size = extractor.readSampleData(buffer, 0)
        if (size < 0) {
            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            return true
        }
        codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
        extractor.advance()
        return false
    }

    /** Забирает готовый PCM и сводит его в моно. true — декодер выдал конец потока. */
    private fun drainOutput(
        codec: MediaCodec,
        info: MediaCodec.BufferInfo,
        format: DecoderOutputFormat,
        output: FloatArrayBuilder,
    ): Boolean {
        val outputIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
        if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            format.update(codec.outputFormat)
            return false
        }
        if (outputIndex < 0) return false
        if (info.size > 0) {
            val buffer = checkNotNull(codec.getOutputBuffer(outputIndex))
            buffer.position(info.offset)
            buffer.limit(info.offset + info.size)
            appendDownmixed(buffer.slice().order(ByteOrder.nativeOrder()), format.channels, format.encoding, output)
        }
        codec.releaseOutputBuffer(outputIndex, false)
        return info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
    }

    private fun appendDownmixed(buffer: ByteBuffer, channels: Int, encoding: Int, output: FloatArrayBuilder) {
        when (encoding) {
            AudioFormat.ENCODING_PCM_16BIT -> {
                val shorts = buffer.asShortBuffer()
                repeat(shorts.remaining() / channels) {
                    var sum = 0f
                    repeat(channels) { sum += shorts.get() / PCM16_FULL_SCALE }
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
        val hasDuration = format.containsKey(MediaFormat.KEY_DURATION)
        val durationUs = if (hasDuration) format.getLong(MediaFormat.KEY_DURATION) else 0L
        return (durationUs * sampleRate / US_PER_SECOND).coerceIn(MIN_CAPACITY_SAMPLES, Int.MAX_VALUE / 2L).toInt()
    }

    private companion object {
        const val TIMEOUT_US = 10_000L
        const val US_PER_SECOND = 1_000_000L
        const val MIN_CAPACITY_SAMPLES = 16_000L

        /** 16-битный PCM → float [-1; 1]. */
        const val PCM16_FULL_SCALE = 32_768f
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
