package ru.nuclearhack.stenographer.feature.transcript.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButtonSize
import ru.nuclearhack.stenographer.common.uikit.components.control.StenoOptionCard
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.tabular
import ru.nuclearhack.stenographer.core.domain.repository.TranscriptFormat
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.exportFileName

/** Содержимое листа экспорта: выбор формата, имя файла и «Сохранить в файл». VTT в домене нет — не предлагаем. */
@Composable
internal fun ColumnScope.ExportSheetContent(
    recordingName: String?,
    selected: TranscriptFormat,
    onSelect: (TranscriptFormat) -> Unit,
    onSave: () -> Unit,
) {
    val colors = StenoTheme.colors
    val spacing = StenoTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        StenoText("Формат", style = StenoTheme.typography.label)
        TranscriptFormat.entries.forEach { format ->
            StenoOptionCard(
                title = format.extension.uppercase(),
                subtitle = format.description(),
                selected = format == selected,
                onClick = { onSelect(format) },
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StenoTheme.shapes.md)
            .background(colors.surface1)
            .border(Dp.Hairline, colors.line, StenoTheme.shapes.md)
            .padding(horizontal = spacing.s3, vertical = spacing.s3),
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StenoIcon(StenoIcons.AudioFile, contentDescription = null, tint = colors.text2)
        StenoText(
            exportFileName(recordingName, selected),
            modifier = Modifier.weight(1f),
            style = StenoTheme.typography.bodySmall.tabular(),
        )
    }
    StenoButton(
        text = "Сохранить в файл",
        onClick = onSave,
        size = StenoButtonSize.Large,
        icon = StenoIcons.Import,
        fillWidth = true,
    )
}

private fun TranscriptFormat.description(): String = when (this) {
    TranscriptFormat.Json -> "формат организаторов"
    TranscriptFormat.Text -> "простой текст"
    TranscriptFormat.Srt -> "субтитры"
}
