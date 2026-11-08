package ru.nuclearhack.stenographer.feature.home.presentation

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch
import ru.nuclearhack.stenographer.common.uikit.components.badge.StenoBadge
import ru.nuclearhack.stenographer.common.uikit.components.badge.StenoBadgeTone
import ru.nuclearhack.stenographer.common.uikit.components.badge.StenoStatusDot
import ru.nuclearhack.stenographer.common.uikit.components.bar.StenoTopBar
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.overlay.StenoToastHostState
import ru.nuclearhack.stenographer.common.uikit.components.overlay.rememberStenoToastHostState
import ru.nuclearhack.stenographer.common.uikit.components.scaffold.StenoScaffold
import ru.nuclearhack.stenographer.common.uikit.components.state.StenoStatePanel
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoActionCard
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoActionCardStyle
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoCard
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoDivider
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoListRow
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIllustrations
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface
import ru.nuclearhack.stenographer.common.uikit.udf.CollectEffects
import ru.nuclearhack.stenographer.core.domain.engine.EngineInfo
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.model.AudioRef
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenTranscript: (AudioRef) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // Постоянный доступ к файлу — чтобы потом открыть расшифровку из истории без повторного выбора.
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        viewModel.onAction(HomeAction.AudioPicked(uri?.toString()))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is HomeEffect.OpenTranscript -> onOpenTranscript(effect.audio)
        }
    }

    val toasts = rememberStenoToastHostState()
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))) }

    HomeContent(
        state = state,
        today = today,
        toasts = toasts,
        onPickAudio = { pickAudio.launch(arrayOf("audio/*")) },
        onRecord = { scope.launch { toasts.show("Запись появится в следующей версии", icon = StenoIcons.Mic) } },
        onOpenSettings = onOpenSettings,
    )
}

/** Содержимое главного экрана: два действия, готовность моделей и подсказка о пустой истории. */
@Composable
private fun HomeContent(
    state: HomeState,
    today: String,
    toasts: StenoToastHostState,
    onPickAudio: () -> Unit,
    onRecord: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val spacing = StenoTheme.spacing
    StenoScaffold(
        topBar = {
            StenoTopBar(title = "Главная", eyebrow = today) {
                StenoIconButton(
                    icon = StenoIcons.Settings,
                    contentDescription = "Настройки",
                    onClick = onOpenSettings,
                    variant = StenoIconButtonVariant.Ghost,
                )
            }
        },
        toastHostState = toasts,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = spacing.screen, vertical = spacing.s4),
            verticalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            PrivacyBadges(state.engines)
            StenoActionCard(
                title = "Записать разговор",
                subtitle = "Скоро: диктофон с паузой в любой момент",
                icon = StenoIcons.Record,
                onClick = onRecord,
                modifier = Modifier.padding(top = spacing.s2),
            )
            StenoActionCard(
                title = "Импортировать файл",
                subtitle = "MP3, M4A, WAV, OGG, FLAC",
                icon = StenoIcons.Import,
                onClick = onPickAudio,
                style = StenoActionCardStyle.Secondary,
            )
            EnginesCard(state.engines, modifier = Modifier.padding(top = spacing.s2))
            StenoStatePanel(
                title = "Расшифровок пока нет",
                message = "Импортируйте аудиофайл — готовая расшифровка с разбивкой по говорящим появится здесь.",
                illustration = StenoIllustrations.EmptyHistory,
                modifier = Modifier.padding(top = spacing.s2),
            )
        }
    }
}

/** Плашки приватности и готовности моделей. */
@Composable
private fun PrivacyBadges(engines: List<EngineInfo>) {
    val ready = engines.isNotEmpty() && engines.all { it.isReady }
    Column(verticalArrangement = Arrangement.spacedBy(StenoTheme.spacing.s2)) {
        StenoBadge("На устройстве, без интернета", tone = StenoBadgeTone.Primary, icon = StenoIcons.Shield)
        StenoBadge(
            text = if (ready) "Модели готовы" else "Часть моделей не подключена",
            tone = if (ready) StenoBadgeTone.Success else StenoBadgeTone.Warning,
            icon = if (ready) StenoIcons.Check else StenoIcons.Warning,
        )
    }
}

@Composable
private fun EnginesCard(engines: List<EngineInfo>, modifier: Modifier = Modifier) {
    StenoCard(modifier = modifier.fillMaxWidth()) {
        StenoText("Модели на устройстве", style = StenoTheme.typography.subtitle)
        engines.forEachIndexed { index, engine ->
            if (index > 0) StenoDivider()
            StenoListRow(
                title = "${engine.kind.title()}: ${engine.modelName}",
                subtitle = "${engine.runtime} · ${if (engine.isReady) "готова" else "не подключена"}",
                leading = {
                    StenoStatusDot(color = if (engine.isReady) StenoTheme.colors.success else StenoTheme.colors.warning)
                },
            )
        }
    }
}

private fun EngineKind.title(): String = when (this) {
    EngineKind.Diarization -> "Диаризация"
    EngineKind.SpeechRecognition -> "Распознавание"
    EngineKind.Llm -> "LLM"
}

private val previewState = HomeState(
    engines = listOf(
        EngineInfo(EngineKind.Diarization, "Nemotron-3-Diarization q8_0", "NeMo-Speech.cpp (ggml)", isReady = true),
        EngineInfo(EngineKind.SpeechRecognition, "GigaAM-v3 e2e_rnnt", "sherpa-onnx", isReady = true),
        EngineInfo(EngineKind.Llm, "Qwen3.5-2B Q4_K_M", "llama.cpp", isReady = false),
    ),
)

private fun previewNoop() = Unit

@Composable
private fun HomePreview(dark: Boolean) {
    StenoThemedSurface(darkTheme = dark) {
        HomeContent(
            state = previewState,
            today = "8 ноября",
            toasts = rememberStenoToastHostState(),
            onPickAudio = ::previewNoop,
            onRecord = ::previewNoop,
            onOpenSettings = ::previewNoop,
        )
    }
}

@Preview(name = "Светлая", showBackground = true, heightDp = 800)
@Composable
private fun HomeLightPreview() = HomePreview(dark = false)

@Preview(
    name = "Тёмная",
    showBackground = true,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeDarkPreview() = HomePreview(dark = true)

@Preview(name = "360 dp", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun HomeNarrowPreview() = HomePreview(dark = false)
