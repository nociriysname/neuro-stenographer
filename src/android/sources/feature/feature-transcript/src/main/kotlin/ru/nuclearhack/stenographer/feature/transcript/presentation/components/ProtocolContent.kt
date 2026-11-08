package ru.nuclearhack.stenographer.feature.transcript.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import ru.nuclearhack.stenographer.common.uikit.components.badge.StenoSpeakerBadge
import ru.nuclearhack.stenographer.common.uikit.components.badge.StenoSpeakerBadgeSize
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.list.StenoCheckItem
import ru.nuclearhack.stenographer.common.uikit.components.list.StenoSectionHeader
import ru.nuclearhack.stenographer.common.uikit.components.list.StenoTaskItem
import ru.nuclearhack.stenographer.common.uikit.components.progress.StenoProgressRing
import ru.nuclearhack.stenographer.common.uikit.components.state.StenoStateAction
import ru.nuclearhack.stenographer.common.uikit.components.state.StenoStatePanel
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoDivider
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIllustrations
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.tabular
import ru.nuclearhack.stenographer.core.model.MeetingProtocol
import ru.nuclearhack.stenographer.core.model.ProtocolTask
import ru.nuclearhack.stenographer.feature.transcript.presentation.ProtocolUi
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.assigneeLabel
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.assigneeSpeakerNumber
import kotlin.math.roundToInt

/** Экран 8 «Протокол встречи»: краткое содержание, решения, задачи; копировать и экспортировать. */
@Composable
internal fun ProtocolContent(
    protocol: ProtocolUi,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
) {
    val spacing = StenoTheme.spacing
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ProtocolHeader(
            subtitle = (protocol as? ProtocolUi.Ready)?.let { "Составлен на устройстве · ${it.createdAt}" }
                ?: "Составляется на устройстве",
            onBack = onBack,
            onCopy = onCopy.takeIf { protocol is ProtocolUi.Ready },
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (protocol) {
                is ProtocolUi.Generating -> GeneratingState(protocol.progress)
                is ProtocolUi.Failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    StenoStatePanel(
                        title = "Не удалось составить протокол",
                        message = protocol.message,
                        illustration = StenoIllustrations.ErrorModel,
                        primaryAction = StenoStateAction("Повторить", onRetry, StenoIcons.Retry),
                        secondaryAction = StenoStateAction("Назад", onBack),
                    )
                }

                is ProtocolUi.Ready -> ProtocolBody(protocol)
            }
        }
        if (protocol is ProtocolUi.Ready) {
            StenoDivider()
            Column(
                Modifier.padding(start = spacing.s5, end = spacing.s5, top = spacing.s3, bottom = spacing.s5),
                verticalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                    StenoIcon(StenoIcons.Info, contentDescription = null, tint = StenoTheme.colors.text2, size = spacing.s5)
                    StenoText(
                        "Протокол пишет небольшая модель — проверьте формулировки",
                        style = StenoTheme.typography.caption,
                        color = StenoTheme.colors.text2,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s3)) {
                    StenoButton(
                        "Копировать",
                        onCopy,
                        modifier = Modifier.weight(1f),
                        variant = StenoButtonVariant.Secondary,
                        icon = StenoIcons.Copy,
                    )
                    StenoButton("Экспорт", onShare, modifier = Modifier.weight(1f), icon = StenoIcons.Share)
                }
            }
        }
    }
}

@Composable
private fun ProtocolHeader(subtitle: String, onBack: () -> Unit, onCopy: (() -> Unit)?) {
    val spacing = StenoTheme.spacing
    Row(
        Modifier.fillMaxWidth().padding(start = spacing.s1, end = spacing.s1, top = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StenoIconButton(StenoIcons.Back, "Назад", onBack, variant = StenoIconButtonVariant.Ghost)
        Column(Modifier.weight(1f).padding(horizontal = spacing.s1)) {
            StenoText(
                "Протокол встречи",
                modifier = Modifier.semantics { heading() },
                style = StenoTheme.typography.labelLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s1)) {
                StenoIcon(StenoIcons.Chip, contentDescription = null, tint = StenoTheme.colors.text2, size = spacing.s4)
                StenoText(subtitle, style = StenoTheme.typography.caption, color = StenoTheme.colors.text2)
            }
        }
        if (onCopy != null) {
            StenoIconButton(StenoIcons.Copy, "Копировать протокол", onCopy, variant = StenoIconButtonVariant.Ghost)
        }
    }
}

@Composable
private fun GeneratingState(progress: Float) {
    val spacing = StenoTheme.spacing
    Column(
        Modifier.fillMaxSize().padding(horizontal = spacing.s8),
        verticalArrangement = Arrangement.spacedBy(spacing.s4, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StenoProgressRing(progress = progress) {
            StenoText(
                "${(progress * 100).roundToInt()}%",
                style = StenoTheme.typography.subtitle.tabular(),
                textAlign = TextAlign.Center,
            )
        }
        StenoText("Составляем протокол", style = StenoTheme.typography.subtitle, textAlign = TextAlign.Center)
        StenoText(
            "Локальная модель читает расшифровку и выписывает решения и задачи. Интернет не нужен — " +
                "обычно это занимает около минуты.",
            style = StenoTheme.typography.bodySmall,
            color = StenoTheme.colors.text2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProtocolBody(ready: ProtocolUi.Ready) {
    val p: MeetingProtocol = ready.protocol
    val spacing = StenoTheme.spacing
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.s5),
    ) {
        if (p.title.isNotBlank()) {
            StenoText(p.title, modifier = Modifier.padding(top = spacing.s2), style = StenoTheme.typography.title)
        }
        Section {
            StenoSectionHeader(StenoIcons.Protocol, "Краткое содержание")
            StenoText(p.summary.ifBlank { "Модель не выделила содержание." }, style = StenoTheme.typography.body)
        }
        StenoDivider()
        Section {
            StenoSectionHeader(StenoIcons.Check, "Решения")
            if (p.decisions.isEmpty()) EmptyLine("Решений не зафиксировано")
            p.decisions.forEach { StenoCheckItem(it) }
        }
        StenoDivider()
        Section {
            StenoSectionHeader(StenoIcons.Tasks, "Задачи", count = p.tasks.size)
            if (p.tasks.isEmpty()) EmptyLine("Поручений не прозвучало")
            p.tasks.forEach { TaskRow(it, ready.speakerNumbers) }
        }
        if (p.keyPoints.isNotEmpty()) {
            StenoDivider()
            Section {
                StenoSectionHeader(StenoIcons.Info, "Ключевые моменты")
                p.keyPoints.forEach {
                    StenoText("•  $it", style = StenoTheme.typography.bodySmall, color = StenoTheme.colors.text2)
                }
            }
        }
    }
}

@Composable
private fun Section(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = StenoTheme.spacing.s4),
        verticalArrangement = Arrangement.spacedBy(StenoTheme.spacing.s2),
    ) { content() }
}

@Composable
private fun EmptyLine(text: String) {
    StenoText(text, style = StenoTheme.typography.bodySmall, color = StenoTheme.colors.text3)
}

@Composable
private fun TaskRow(task: ProtocolTask, speakerNumbers: Map<String, Int>) {
    val assignee = task.assignee
    val meta = listOfNotNull(assignee?.let(::assigneeLabel), task.due?.let { "до ${it.removePrefix("до ").trim()}" })
        .joinToString(" · ")
        .ifEmpty { null }
    val number = assignee?.let(::assigneeSpeakerNumber)
    StenoTaskItem(
        text = task.task,
        meta = meta,
        badge = assignee?.let {
            {
                // Спикер из диаризации — его цвет и номер; прозвучавшее имя — инициал в нейтральном цвете.
                val colorIndex = number?.let { n -> (speakerNumbers["speaker_$n"] ?: n) - 1 } ?: NEUTRAL_SPEAKER_COLOR
                StenoSpeakerBadge(
                    label = number?.toString() ?: assigneeLabel(it).take(1).uppercase(),
                    colorIndex = colorIndex,
                    size = StenoSpeakerBadgeSize.Small,
                )
            }
        },
    )
}

/** Восьмой цвет палитры спикеров — серый: для исполнителей, названных по имени. */
private const val NEUTRAL_SPEAKER_COLOR = 7
