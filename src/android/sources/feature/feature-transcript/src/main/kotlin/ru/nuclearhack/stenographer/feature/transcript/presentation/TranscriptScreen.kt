package ru.nuclearhack.stenographer.feature.transcript.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import ru.nuclearhack.stenographer.common.uikit.components.bar.StenoTopBar
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButton
import ru.nuclearhack.stenographer.common.uikit.components.field.StenoTextField
import ru.nuclearhack.stenographer.common.uikit.components.overlay.StenoBottomSheet
import ru.nuclearhack.stenographer.common.uikit.components.overlay.StenoToastHostState
import ru.nuclearhack.stenographer.common.uikit.components.overlay.rememberStenoToastHostState
import ru.nuclearhack.stenographer.common.uikit.components.scaffold.StenoScaffold
import ru.nuclearhack.stenographer.common.uikit.components.state.StenoStateAction
import ru.nuclearhack.stenographer.common.uikit.components.state.StenoStatePanel
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIllustrations
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.udf.CollectEffects
import ru.nuclearhack.stenographer.core.domain.repository.TranscriptFormat
import ru.nuclearhack.stenographer.core.model.AudioRef
import ru.nuclearhack.stenographer.feature.transcript.presentation.components.DetailsSheetContent
import ru.nuclearhack.stenographer.feature.transcript.presentation.components.DoneTopBar
import ru.nuclearhack.stenographer.feature.transcript.presentation.components.ExportSheetContent
import ru.nuclearhack.stenographer.feature.transcript.presentation.components.LocalSpeakerNames
import ru.nuclearhack.stenographer.feature.transcript.presentation.components.ProcessingContent
import ru.nuclearhack.stenographer.feature.transcript.presentation.components.ProtocolContent
import ru.nuclearhack.stenographer.feature.transcript.presentation.components.SearchTopBar
import ru.nuclearhack.stenographer.feature.transcript.presentation.components.TranscriptDoneContent
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.rememberTranscriptPlayer

/** Обработка записи и результат: этапы, «Диалог» / «Стенограмма», поиск и экспорт. */
@Composable
fun TranscriptScreen(
    audio: AudioRef,
    onBack: () -> Unit,
    viewModel: TranscriptViewModel =
        assistedMetroViewModel<TranscriptViewModel, TranscriptViewModel.Factory> { create(audio) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toastHostState = rememberStenoToastHostState()
    val context = LocalContext.current

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

            is TranscriptEffect.ShowMessage -> toastHostState.show(effect.text)
            is TranscriptEffect.CopyText -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Протокол встречи", effect.text))
                toastHostState.show("Протокол скопирован")
            }

            is TranscriptEffect.ShareText -> context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, effect.title)
                        .putExtra(Intent.EXTRA_TEXT, effect.text),
                    effect.title,
                ),
            )
        }
    }

    CompositionLocalProvider(LocalSpeakerNames provides state.speakerNames) {
        TranscriptContent(
            state,
            toastHostState,
            onBack = onBack,
            onAction = viewModel::onAction,
            audioUri = audio.value,
        )
    }
}

@Composable
internal fun TranscriptContent(
    state: TranscriptState,
    toastHostState: StenoToastHostState,
    onBack: () -> Unit,
    onAction: (TranscriptAction) -> Unit,
    audioUri: String? = null,
) {
    val phase = state.phase
    val protocol = state.protocol
    if (protocol != null) {
        BackHandler { onAction(TranscriptAction.ProtocolClosed) }
        StenoScaffold(toastHostState = toastHostState) {
            ProtocolContent(
                protocol = protocol,
                onBack = { onAction(TranscriptAction.ProtocolClosed) },
                onCopy = { onAction(TranscriptAction.ProtocolCopy) },
                onShare = { onAction(TranscriptAction.ProtocolShare) },
                onRetry = { onAction(TranscriptAction.ProtocolRetry) },
            )
        }
        return
    }
    BackHandler(enabled = state.search.open) { onAction(TranscriptAction.SearchClosed) }
    StenoScaffold(
        topBar = { TranscriptTopBar(state, onBack, onAction) },
        toastHostState = toastHostState,
    ) {
        when (phase) {
            TranscriptPhase.Preparing -> ProcessingContent(processing = null, onCancel = { cancel(onAction, onBack) })
            is TranscriptPhase.Processing ->
                ProcessingContent(processing = phase, onCancel = { cancel(onAction, onBack) })

            is TranscriptPhase.Done -> TranscriptDoneContent(
                done = phase,
                audioDurationMs = state.audioDurationMs,
                layout = state.layout,
                search = state.search,
                onLayoutSelect = { onAction(TranscriptAction.LayoutSelected(it)) },
                player = audioUri?.let { rememberTranscriptPlayer(it, phase.transcript.utterances) },
            )

            is TranscriptPhase.Failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                StenoStatePanel(
                    title = "Не удалось расшифровать запись",
                    message = phase.message,
                    illustration = StenoIllustrations.ErrorModel,
                    primaryAction = StenoStateAction(
                        "Повторить",
                        { onAction(TranscriptAction.Retry) },
                        StenoIcons.Retry,
                    ),
                    secondaryAction = StenoStateAction("Назад", onBack),
                )
            }
        }
    }
    if (phase is TranscriptPhase.Done) DoneSheets(state, phase, onAction)
}

private fun cancel(onAction: (TranscriptAction) -> Unit, onBack: () -> Unit) {
    onAction(TranscriptAction.Cancel)
    onBack()
}

@Composable
private fun TranscriptTopBar(state: TranscriptState, onBack: () -> Unit, onAction: (TranscriptAction) -> Unit) {
    val title = state.fileName ?: "Расшифровка"
    when {
        state.phase is TranscriptPhase.Done && state.search.open -> SearchTopBar(
            search = state.search,
            onQueryChange = { onAction(TranscriptAction.QueryChanged(it)) },
            onPrevious = { onAction(TranscriptAction.PreviousMatch) },
            onNext = { onAction(TranscriptAction.NextMatch) },
            onClose = { onAction(TranscriptAction.SearchClosed) },
        )

        state.phase is TranscriptPhase.Done -> DoneTopBar(
            title = title,
            onBack = onBack,
            onSearch = { onAction(TranscriptAction.SearchOpened) },
            onProtocol = { onAction(TranscriptAction.ProtocolOpened) },
            onMore = { onAction(TranscriptAction.SheetOpened(TranscriptSheet.Details)) },
        )

        state.phase is TranscriptPhase.Failed -> StenoTopBar(title = title, onBack = onBack)
        else -> StenoTopBar(title = title, eyebrow = "Расшифровка")
    }
}

@Composable
private fun DoneSheets(state: TranscriptState, done: TranscriptPhase.Done, onAction: (TranscriptAction) -> Unit) {
    StenoBottomSheet(
        visible = state.sheet == TranscriptSheet.Export,
        onDismissRequest = { onAction(TranscriptAction.SheetDismissed) },
        title = "Экспорт расшифровки",
    ) {
        ExportSheetContent(
            recordingName = state.fileName,
            selected = state.exportFormat,
            onSelect = { onAction(TranscriptAction.ExportFormatSelected(it)) },
            onSave = { onAction(TranscriptAction.ExportConfirmed) },
        )
    }
    StenoBottomSheet(
        visible = state.sheet == TranscriptSheet.Details,
        onDismissRequest = { onAction(TranscriptAction.SheetDismissed) },
        title = "Ещё",
    ) {
        DetailsSheetContent(
            done,
            onExport = { onAction(TranscriptAction.SheetOpened(TranscriptSheet.Export)) },
            onProtocol = { onAction(TranscriptAction.ProtocolOpened) },
            onRenameSpeaker = { onAction(TranscriptAction.SpeakerRenameOpened(it)) },
        )
    }
    RenameSpeakerSheet(state, onAction)
}

/** Шторка S07: имя говорящего. Пустое поле — вернуть «Спикер N». */
@Composable
private fun RenameSpeakerSheet(state: TranscriptState, onAction: (TranscriptAction) -> Unit) {
    val index = state.renamingSpeaker
    var name by remember(index) { mutableStateOf(index?.let { state.speakerNames[it] }.orEmpty()) }
    StenoBottomSheet(
        visible = index != null,
        onDismissRequest = { onAction(TranscriptAction.SpeakerRenameDismissed) },
        title = "Как зовут говорящего?",
    ) {
        if (index != null) {
            StenoTextField(
                value = name,
                onValueChange = { name = it },
                label = "Имя",
                placeholder = "Спикер ${index + 1}",
            )
            StenoText(
                "Имя появится в репликах и протоколе. В JSON для проверки остаётся speaker_${index + 1}.",
                style = StenoTheme.typography.caption,
                color = StenoTheme.colors.text2,
            )
            StenoButton(
                "Сохранить",
                onClick = { onAction(TranscriptAction.SpeakerRenamed(index, name)) },
                fillWidth = true,
            )
        }
    }
}
