package ru.nuclearhack.stenographer.feature.settings.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import ru.nuclearhack.stenographer.common.uikit.components.badge.StenoBadge
import ru.nuclearhack.stenographer.common.uikit.components.badge.StenoBadgeTone
import ru.nuclearhack.stenographer.common.uikit.components.bar.StenoTopBar
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButtonSize
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.control.StenoOptionCard
import ru.nuclearhack.stenographer.common.uikit.components.control.StenoSegmented
import ru.nuclearhack.stenographer.common.uikit.components.control.StenoSwitch
import ru.nuclearhack.stenographer.common.uikit.components.overlay.StenoToastHostState
import ru.nuclearhack.stenographer.common.uikit.components.overlay.StenoToastTone
import ru.nuclearhack.stenographer.common.uikit.components.overlay.rememberStenoToastHostState
import ru.nuclearhack.stenographer.common.uikit.components.progress.StenoLinearProgress
import ru.nuclearhack.stenographer.common.uikit.components.scaffold.StenoScaffold
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoCard
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoCardStyle
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoDivider
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoIconTile
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoListRow
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.format.formatMegabytes
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface
import ru.nuclearhack.stenographer.common.uikit.udf.CollectEffects
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.settings.ThemeMode
import ru.nuclearhack.stenographer.core.domain.usecase.ModelPackStatus
import ru.nuclearhack.stenographer.core.domain.usecase.ModelStorageStatus

private val themeModes = listOf(ThemeMode.System, ThemeMode.Light, ThemeMode.Dark)
private val themeLabels = listOf("Системная", "Светлая", "Тёмная")

/** Настройки (S11): распознавание, тема, эмоции, модели на устройстве, сведения о приложении. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toasts = rememberStenoToastHostState()
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is SettingsEffect.ShowMessage -> toasts.show(
                message = effect.text,
                icon = if (effect.success) StenoIcons.Check else StenoIcons.Warning,
                tone = if (effect.success) StenoToastTone.Success else StenoToastTone.Error,
            )
        }
    }

    SettingsContent(
        state = state,
        version = version,
        toasts = toasts,
        onAction = viewModel::onAction,
        onBack = onBack,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    version: String?,
    toasts: StenoToastHostState,
    onAction: (SettingsAction) -> Unit,
    onBack: () -> Unit,
    initialLicensesVisible: Boolean = false,
) {
    val spacing = StenoTheme.spacing
    var licensesVisible by rememberSaveable { mutableStateOf(initialLicensesVisible) }

    StenoScaffold(
        topBar = { StenoTopBar(title = "Настройки", onBack = onBack) },
        toastHostState = toasts,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = spacing.screen, vertical = spacing.s2),
            verticalArrangement = Arrangement.spacedBy(spacing.s5),
        ) {
            RecognitionSection(state, onAction)
            Section("Тема") {
                StenoSegmented(
                    options = themeLabels,
                    selectedIndex = themeModes.indexOf(state.themeMode),
                    onSelect = { onAction(SettingsAction.SelectTheme(themeModes[it])) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            StenoListRow(
                title = "Подсвечивать эмоции",
                subtitle = "Подсказка, не оценка человека",
                leading = { StenoIconTile(StenoIcons.Smile) },
                trailing = {
                    StenoSwitch(
                        checked = state.highlightEmotions,
                        onCheckedChange = { onAction(SettingsAction.SetHighlightEmotions(it)) },
                    )
                },
            )
            ModelsSection(state, onCheckFiles = { onAction(SettingsAction.CheckFiles) })
            Section("О приложении") {
                StenoListRow(
                    title = "Нейро-стенографист",
                    subtitle = "Версия ${version ?: "—"} · всё работает офлайн",
                    leading = { StenoIconTile(StenoIcons.Info) },
                )
                StenoListRow(
                    title = "Лицензии и модели",
                    subtitle = "Открытое ПО и условия моделей",
                    leading = { StenoIconTile(StenoIcons.Shield) },
                    trailing = {
                        StenoIcon(StenoIcons.Chevron, contentDescription = null, tint = StenoTheme.colors.text3)
                    },
                    onClick = { licensesVisible = true },
                )
            }
        }
    }
    LicensesSheet(visible = licensesVisible, onDismiss = { licensesVisible = false })
}

/** Раздел с подписью-overline над содержимым. */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(StenoTheme.spacing.s2)) {
        StenoText(
            text = title.uppercase(),
            style = StenoTheme.typography.overline,
            color = StenoTheme.colors.text2,
        )
        content()
    }
}

@Composable
private fun RecognitionSection(state: SettingsState, onAction: (SettingsAction) -> Unit) {
    val selected = state.effectivePunctuation
    Section("Распознавание речи") {
        // Показываем только варианты, модели которых есть в APK: недоступный выбирать бессмысленно.
        Column(verticalArrangement = Arrangement.spacedBy(StenoTheme.spacing.s2)) {
            if (state.punctuatedAvailable) {
                PunctuationOption(
                    title = "С пунктуацией",
                    subtitle = "Точки и запятые расставит модель",
                    available = true,
                    selected = selected == true,
                    onClick = { onAction(SettingsAction.SelectPunctuation(true)) },
                )
            }
            if (state.plainAvailable) {
                PunctuationOption(
                    title = "Без пунктуации",
                    subtitle = "Быстрее, текст без знаков препинания",
                    available = true,
                    selected = selected == false,
                    onClick = { onAction(SettingsAction.SelectPunctuation(false)) },
                )
            }
        }
    }
}

/**
 * Вариант распознавания. Если его модели нет, вместо [StenoOptionCard] рисуется плоская «погасшая» карточка:
 * в UI-ките у карточки выбора нет состояния «недоступна».
 */
@Composable
private fun PunctuationOption(
    title: String,
    subtitle: String,
    available: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (available) {
        StenoOptionCard(title = title, subtitle = subtitle, selected = selected, onClick = onClick)
        return
    }
    val colors = StenoTheme.colors
    StenoCard(
        modifier = Modifier.fillMaxWidth().semantics { disabled() },
        style = StenoCardStyle.Flat,
    ) {
        StenoText(title, style = StenoTheme.typography.labelLarge, color = colors.text3)
        StenoText(subtitle, style = StenoTheme.typography.bodySmall, color = colors.text3)
        StenoBadge("Модель не установлена", tone = StenoBadgeTone.Warning, icon = StenoIcons.Warning)
    }
}

@Composable
private fun ModelsSection(state: SettingsState, onCheckFiles: () -> Unit) {
    val spacing = StenoTheme.spacing
    val ready = state.storage.allReady
    Section("Нейросети на устройстве") {
        StenoListRow(
            title = "Место под модели",
            subtitle = "Память телефона · занято ${formatMegabytes(state.storage.usedBytes)}",
            leading = { StenoIconTile(StenoIcons.Chip) },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StenoBadge(
                text = if (ready) {
                    "Модели готовы"
                } else {
                    "Готово моделей: ${state.installedModels} из ${state.totalModels}"
                },
                tone = if (ready) StenoBadgeTone.Success else StenoBadgeTone.Warning,
                icon = if (ready) StenoIcons.Check else StenoIcons.Warning,
            )
            StenoButton(
                text = if (state.checking) "Проверяем…" else "Проверить файлы",
                onClick = onCheckFiles,
                variant = StenoButtonVariant.Text,
                size = StenoButtonSize.Small,
                enabled = !state.checking,
            )
        }
        if (state.checking) StenoLinearProgress(progress = state.checkProgress, modifier = Modifier.fillMaxWidth())
    }
}

private val previewStorage = ModelStorageStatus(
    packs = listOf(
        ModelPackStatus(
            kind = EngineKind.Diarization,
            title = "Диаризация",
            sizeBytes = 150L * 1024 * 1024,
            installed = true,
            available = true,
        ),
        ModelPackStatus(
            kind = EngineKind.SpeechRecognition,
            title = "GigaAM-v3",
            sizeBytes = 262L * 1024 * 1024,
            installed = true,
            available = true,
        ),
    ),
)

private val previewState = SettingsState(
    punctuation = true,
    themeMode = ThemeMode.System,
    highlightEmotions = false,
    punctuatedAvailable = true,
    plainAvailable = true,
    storage = previewStorage,
)

private fun previewNoopAction(@Suppress("UNUSED_PARAMETER") action: SettingsAction) = Unit

private fun previewNoop() = Unit

@Composable
private fun SettingsPreview(dark: Boolean, state: SettingsState, licenses: Boolean = false) {
    StenoThemedSurface(darkTheme = dark) {
        SettingsContent(
            state = state,
            version = "1.0",
            toasts = rememberStenoToastHostState(),
            onAction = ::previewNoopAction,
            onBack = ::previewNoop,
            initialLicensesVisible = licenses,
        )
    }
}

@Preview(name = "Светлая", showBackground = true, heightDp = 900)
@Composable
private fun SettingsLightPreview() = SettingsPreview(dark = false, state = previewState)

@Preview(name = "Тёмная", showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsDarkPreview() = SettingsPreview(
    dark = true,
    state = previewState.copy(
        punctuation = false,
        themeMode = ThemeMode.Dark,
        highlightEmotions = true,
        punctuatedAvailable = false,
    ),
)

@Preview(name = "Модели не готовы, проверка", showBackground = true, heightDp = 900)
@Composable
private fun SettingsCheckingPreview() = SettingsPreview(
    dark = false,
    state = previewState.copy(
        storage = ModelStorageStatus(previewStorage.packs.map { it.copy(installed = false) }),
        checking = true,
        checkProgress = 0.4f,
    ),
)

@Preview(name = "Лицензии", showBackground = true, heightDp = 900)
@Composable
private fun SettingsLicensesPreview() = SettingsPreview(dark = false, state = previewState, licenses = true)

@Preview(name = "Список лицензий", showBackground = true)
@Composable
private fun LicensesListPreview() {
    StenoThemedSurface(darkTheme = false) {
        StenoDivider()
        LicensesList(Modifier.padding(horizontal = StenoTheme.spacing.screen))
    }
}
