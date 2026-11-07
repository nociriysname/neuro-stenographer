package ru.nuclearhack.stenographer.core.data.audio

import android.net.Uri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ru.nuclearhack.stenographer.common.audio.AudioDecoder
import ru.nuclearhack.stenographer.common.audio.AudioFileInfoReader
import ru.nuclearhack.stenographer.core.domain.repository.AudioRepository
import ru.nuclearhack.stenographer.core.model.AudioMetadata
import ru.nuclearhack.stenographer.core.model.AudioRef
import ru.nuclearhack.stenographer.core.model.PcmAudio

@Inject
@ContributesBinding(AppScope::class)
class AudioRepositoryImpl(
    private val decoder: AudioDecoder,
    private val infoReader: AudioFileInfoReader,
) : AudioRepository {

    override suspend fun metadata(ref: AudioRef): AudioMetadata {
        val info = infoReader.read(Uri.parse(ref.value))
        return AudioMetadata(displayName = info.displayName, durationMs = info.durationMs)
    }

    override suspend fun load(ref: AudioRef): PcmAudio {
        val samples = decoder.decodeToMono(Uri.parse(ref.value), targetSampleRate = PcmAudio.SAMPLE_RATE_16K)
        return PcmAudio(samples, PcmAudio.SAMPLE_RATE_16K)
    }
}
