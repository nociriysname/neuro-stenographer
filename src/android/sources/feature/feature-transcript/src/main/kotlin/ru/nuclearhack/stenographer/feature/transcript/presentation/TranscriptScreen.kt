package ru.nuclearhack.stenographer.feature.transcript.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import java.util.Locale
import ru.nuclearhack.stenographer.common.uikit.theme.SpeakerPalette
import ru.nuclearhack.stenographer.common.uikit.theme.StenographerTheme
import ru.nuclearhack.stenographer.common.uikit.udf.CollectEffects
import ru.nuclearhack.stenographer.core.domain.repository.TranscriptFormat
import ru.nuclearhack.stenographer.core.domain.usecase.ProcessingStage
import ru.nuclearhack.stenographer.core.domain.usecase.ProcessingTimings
import ru.nuclearhack.stenographer.core.model.AudioRef
import ru.nuclearhack.stenographer.core.model.SpeakerId
import ru.nuclearhack.stenographer.core.model.Transcript
import ru.nuclearhack.stenographer.core.model.Utterance
import ru.nuclearhack.stenographer.feature.transcript.presentation.components.UtteranceItem

@Composable
fun TranscriptScreen(
    audio: AudioRef,
    onBack: () -> Unit,
    viewModel: TranscriptViewModel =
        assistedMetroViewModel<TranscriptViewModel, TranscriptViewModel.Factory> { create(audio) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Один лаунчер на формат: mime-тип CreateDocument задаётся при создании контракта.
    val exportLaunchers = TranscriptFormat.entries.associateWith { format ->
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(format.mimeType)) { uri ->
            viewModel.onAction(TranscriptAction.ExportDestinationChosen(uri?.toString(), format))
        }
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is TranscriptEffect.ChooseExportDestination ->
                exportLaunchers.getValue(effect.format).launch(effect.suggestedName)

            is TranscriptEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
        }
    }

    TranscriptContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranscriptContent(
    state: TranscriptState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAction: (TranscriptAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.fileName ?: "Расшифровка", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } },
                actions = {
                    if (state.phase is TranscriptPhase.Done) {
                        TranscriptFormat.entries.forEach { format ->
                            TextButton(onClick = { onAction(TranscriptAction.ExportRequested(format)) }) {
                                Text(format.extension.uppercase())
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (val phase = state.phase) {
                TranscriptPhase.Preparing -> ProcessingPanel(title = "Подготовка…", progress = null)
                is TranscriptPhase.Processing -> ProcessingPanel(title = phase.stage.title(), progress = phase.progress)
                is TranscriptPhase.Done -> TranscriptList(phase)
                is TranscriptPhase.Failed -> FailurePanel(phase.message, onRetry = { onAction(TranscriptAction.Retry) })
            }
        }
    }
}

@Composable
private fun ProcessingPanel(title: String, progress: Float?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        if (progress == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Все вычисления выполняются на устройстве",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FailurePanel(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Не удалось расшифровать запись", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Повторить") }
    }
}

@Composable
private fun TranscriptList(done: TranscriptPhase.Done) {
    val speakers = done.transcript.speakers
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SummaryCard(done) }
        if (done.transcript.utterances.isEmpty()) {
            item { Text("Речь в записи не найдена", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(done.transcript.utterances) { utterance ->
            UtteranceItem(utterance, color = SpeakerPalette.colorFor(speakers.indexOf(utterance.speaker)))
        }
    }
}

@Composable
private fun SummaryCard(done: TranscriptPhase.Done) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Говорящих: ${done.transcript.speakers.size} · Реплик: ${done.transcript.utterances.size}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = String.format(
                    Locale.ROOT,
                    "Обработка %.1f с · RTF %.2f",
                    done.timings.totalMs / 1000.0,
                    done.timings.rtf,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            done.warnings.forEach { warning ->
                Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun ProcessingStage.title(): String = when (this) {
    ProcessingStage.Decoding -> "Декодирование аудио"
    ProcessingStage.Diarization -> "Определение говорящих"
    ProcessingStage.Recognition -> "Распознавание речи"
    ProcessingStage.PostProcessing -> "Постобработка (LLM)"
}

@Preview(showBackground = true)
@Composable
private fun TranscriptDonePreview() {
    val transcript = Transcript(
        listOf(
            Utterance(SpeakerId("speaker_1"), 1_250, 4_380, "Добрый день! Объявляю заседание Котовицкого совета открытым."),
            Utterance(SpeakerId("speaker_2"), 4_900, 6_100, "Мур. А протокол кто ведёт?"),
            Utterance(SpeakerId("speaker_1"), 6_300, 8_000, "Нейро-стенографист, конечно."),
        ),
    )
    StenographerTheme(dynamicColor = false) {
        TranscriptContent(
            state = TranscriptState(
                fileName = "zasedanie.m4a",
                phase = TranscriptPhase.Done(
                    transcript = transcript,
                    timings = ProcessingTimings(8_000, 120, 400, 900, 300, 1_720),
                    warnings = emptyList(),
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onAction = {},
        )
    }
}
