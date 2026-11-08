package ru.nuclearhack.stenographer.feature.home.presentation.record

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.nuclearhack.stenographer.common.uikit.components.audio.StenoLevelMeter
import ru.nuclearhack.stenographer.common.uikit.components.audio.StenoRecordButton
import ru.nuclearhack.stenographer.common.uikit.components.audio.StenoWaveform
import ru.nuclearhack.stenographer.common.uikit.components.audio.StenoWaveformStyle
import ru.nuclearhack.stenographer.common.uikit.components.badge.StenoBadge
import ru.nuclearhack.stenographer.common.uikit.components.badge.StenoBadgeTone
import ru.nuclearhack.stenographer.common.uikit.components.bar.StenoTopBar
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.overlay.rememberStenoToastHostState
import ru.nuclearhack.stenographer.common.uikit.components.scaffold.StenoScaffold
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private enum class RecState { Idle, Recording, Paused }

/**
 * Экран 3 «Запись»: таймер, живая волна и уровень, пауза, «Готово» — и сразу расшифровка.
 * Файл — WAV 16 кГц моно в `filesDir/recordings`, остаётся на устройстве.
 */
@Composable
fun RecordScreen(onBack: () -> Unit, onRecorded: (audioUri: String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toasts = rememberStenoToastHostState()
    var state by remember { mutableStateOf(RecState.Idle) }
    var level by remember { mutableFloatStateOf(0f) }
    var writtenMs by remember { mutableLongStateOf(0L) }
    val levels = remember { mutableStateListOf<Float>() }
    var recorder by remember { mutableStateOf<WavRecorder?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    val file = remember {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        File(File(context.filesDir, "recordings"), "Запись $stamp.wav")
    }

    fun start() {
        val rec = WavRecorder(file).also { recorder = it }
        state = RecState.Recording
        job = scope.launch {
            runCatching {
                rec.record { l, ms ->
                    level = l
                    writtenMs = ms
                    levels += l
                    if (levels.size > WAVE_BARS) levels.removeAt(0)
                }
            }.onFailure {
                state = RecState.Idle
                toasts.show("Не удалось записать: ${it.message}", icon = StenoIcons.Warning)
            }
        }
    }

    fun finish() {
        val rec = recorder ?: return
        rec.stopRequested = true
        scope.launch {
            job?.join()
            if (writtenMs < MIN_RECORDING_MS) {
                state = RecState.Idle
                toasts.show("Запись слишком короткая", icon = StenoIcons.Warning)
            } else {
                onRecorded(Uri.fromFile(file).toString())
            }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            start()
        } else {
            scope.launch { toasts.show("Без доступа к микрофону записать не получится", icon = StenoIcons.Mic) }
        }
    }

    fun onMainButton() {
        when (state) {
            RecState.Idle -> {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) start() else permission.launch(Manifest.permission.RECORD_AUDIO)
            }
            RecState.Recording, RecState.Paused -> finish()
        }
    }

    DisposableEffect(Unit) { onDispose { recorder?.stopRequested = true } }
    BackHandler {
        recorder?.stopRequested = true
        file.delete()
        onBack()
    }

    val spacing = StenoTheme.spacing
    StenoScaffold(
        topBar = {
            StenoTopBar(
                title = "Запись разговора",
                onBack = {
                    recorder?.stopRequested = true
                    file.delete()
                    onBack()
                },
            )
        },
        toastHostState = toasts,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.s6, Alignment.CenterVertically),
        ) {
            when (state) {
                RecState.Idle -> StenoBadge("Готово к записи", icon = StenoIcons.Shield)
                RecState.Recording -> StenoBadge("Идёт запись", tone = StenoBadgeTone.Rec, pulsingDot = true)
                RecState.Paused -> StenoBadge("Пауза", tone = StenoBadgeTone.Warning)
            }
            StenoText(formatClock(writtenMs), style = StenoTheme.typography.timer, textAlign = TextAlign.Center)
            StenoWaveform(levels = levels.toList(), style = StenoWaveformStyle.Recording)
            StenoLevelMeter(level = if (state == RecState.Recording) level else 0f)
            StenoRecordButton(recording = state != RecState.Idle, level = level, onClick = ::onMainButton)
            StenoText(
                when (state) {
                    RecState.Idle -> "Нажмите, чтобы начать. Звук пишется только на устройство."
                    else -> "Нажмите ещё раз, чтобы закончить и расшифровать."
                },
                style = StenoTheme.typography.bodySmall,
                color = StenoTheme.colors.text2,
                textAlign = TextAlign.Center,
            )
            if (state != RecState.Idle) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.s3)) {
                    StenoButton(
                        if (state == RecState.Paused) "Продолжить" else "Пауза",
                        onClick = {
                            val rec = recorder ?: return@StenoButton
                            rec.paused = state == RecState.Recording
                            state = if (rec.paused) RecState.Paused else RecState.Recording
                        },
                        modifier = Modifier.weight(1f),
                        variant = StenoButtonVariant.Secondary,
                        icon = if (state == RecState.Paused) StenoIcons.Record else StenoIcons.Pause,
                    )
                    StenoButton(
                        "Готово",
                        onClick = ::finish,
                        modifier = Modifier.weight(1f),
                        icon = StenoIcons.Check,
                    )
                }
            }
        }
    }
}

private fun formatClock(ms: Long): String {
    val s = ms / MS_IN_SECOND
    return "%02d:%02d".format(s / SECONDS_IN_MINUTE, s % SECONDS_IN_MINUTE)
}

private const val WAVE_BARS = 48
private const val MS_IN_SECOND = 1000L
private const val SECONDS_IN_MINUTE = 60
private const val MIN_RECORDING_MS = 1000L
