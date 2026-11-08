package ru.nuclearhack.stenographer.feature.transcript.presentation.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoListRow
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.feature.transcript.presentation.TranscriptPhase

/** Лист «Ещё»: переход к экспорту и подробная сводка обработки (время по этапам, RTF, предупреждения). */
@Composable
internal fun ColumnScope.DetailsSheetContent(
    done: TranscriptPhase.Done,
    onExport: () -> Unit,
    onProtocol: () -> Unit,
) {
    StenoListRow(
        title = "Протокол встречи",
        subtitle = "Решения и задачи — локальная модель",
        leading = { StenoIcon(StenoIcons.Protocol, contentDescription = null, tint = StenoTheme.colors.text2) },
        trailing = { StenoIcon(StenoIcons.Chevron, contentDescription = null, tint = StenoTheme.colors.text3) },
        onClick = onProtocol,
    )
    StenoListRow(
        title = "Экспорт",
        subtitle = "JSON, TXT, SRT",
        leading = { StenoIcon(StenoIcons.Share, contentDescription = null, tint = StenoTheme.colors.text2) },
        trailing = { StenoIcon(StenoIcons.Chevron, contentDescription = null, tint = StenoTheme.colors.text3) },
        onClick = onExport,
    )
    ProcessingSummary(done, detailed = true)
}
