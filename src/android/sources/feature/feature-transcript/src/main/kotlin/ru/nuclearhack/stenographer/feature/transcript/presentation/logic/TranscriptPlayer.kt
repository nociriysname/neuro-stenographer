package ru.nuclearhack.stenographer.feature.transcript.presentation.logic

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import ru.nuclearhack.stenographer.core.model.Utterance

/**
 * Проигрывание исходной записи на экране результата: тап по реплике, мини-плеер, скорость, пропуск тишины.
 * Живёт только пока открыт экран; MediaPlayer готовится лениво — при первом нажатии «играть».
 */
@Stable
internal class TranscriptPlayer(private val context: Context, private val uri: String) {
    var positionMs by mutableLongStateOf(0L)
        private set
    var playing by mutableStateOf(false)
        private set
    var speed by mutableFloatStateOf(1f)
        private set
    var skipSilence by mutableStateOf(false)
        private set

    private var player: MediaPlayer? = null
    private var failed = false

    private fun ensurePlayer(): MediaPlayer? {
        if (failed) return null
        player?.let { return it }
        return runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
                )
                setDataSource(context, Uri.parse(uri))
                prepare()
                setOnCompletionListener { this@TranscriptPlayer.playing = false }
            }
        }.onFailure { failed = true }.getOrNull().also { player = it }
    }

    fun toggle() = if (playing) pause() else play()

    fun play() {
        val p = ensurePlayer() ?: return
        applySpeed(p)
        p.start()
        playing = true
    }

    fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
        playing = false
    }

    fun seekTo(ms: Long) {
        positionMs = ms
        ensurePlayer()?.seekTo(ms, MediaPlayer.SEEK_CLOSEST)
    }

    /** Тап по реплике: перейти к её началу и играть. */
    fun playFrom(ms: Long) {
        seekTo(ms)
        play()
    }

    fun cycleSpeed() {
        speed = SPEEDS[(SPEEDS.indexOf(speed) + 1) % SPEEDS.size]
        player?.let { if (playing) applySpeed(it) }
    }

    fun toggleSkipSilence() {
        skipSilence = !skipSilence
    }

    private fun applySpeed(p: MediaPlayer) {
        p.playbackParams = p.playbackParams.setSpeed(speed)
    }

    /** Опрос позиции; при «пропуске тишины» паузы между репликами перескакиваются. */
    suspend fun tick(utterances: List<Utterance>) {
        val p = player ?: return
        if (!p.isPlaying) return
        positionMs = p.currentPosition.toLong()
        if (skipSilence) {
            val inside = utterances.any { positionMs in it.startMs until it.endMs }
            val next = utterances.firstOrNull { it.startMs > positionMs }
            if (!inside && next != null && next.startMs - positionMs > SILENCE_JUMP_MS) seekTo(next.startMs)
        }
        delay(TICK_MS)
    }

    fun release() {
        player?.release()
        player = null
        playing = false
    }

    private companion object {
        val SPEEDS = listOf(1f, 1.5f, 2f)
        const val TICK_MS = 100L
        const val SILENCE_JUMP_MS = 400L
    }
}

@Composable
internal fun rememberTranscriptPlayer(uri: String, utterances: List<Utterance>): TranscriptPlayer {
    val context = LocalContext.current
    val player = remember(uri) { TranscriptPlayer(context.applicationContext, uri) }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(player, player.playing, utterances) {
        while (player.playing) player.tick(utterances)
    }
    return player
}

/** Индекс реплики, которая звучит в момент [positionMs], или null. */
internal fun playingUtterance(utterances: List<Utterance>, positionMs: Long): Int? =
    utterances.indexOfFirst { positionMs in it.startMs until it.endMs }.takeIf { it >= 0 }
