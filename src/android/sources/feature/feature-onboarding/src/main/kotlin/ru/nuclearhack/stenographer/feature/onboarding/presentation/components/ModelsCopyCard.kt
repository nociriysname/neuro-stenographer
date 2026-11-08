package ru.nuclearhack.stenographer.feature.onboarding.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import ru.nuclearhack.stenographer.common.uikit.components.progress.StenoLinearProgress
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoCard
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.format.formatMegabytes
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.tabular
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.feature.onboarding.presentation.ModelRow
import ru.nuclearhack.stenographer.feature.onboarding.presentation.ModelRowStatus
import ru.nuclearhack.stenographer.feature.onboarding.presentation.ModelsState

/** Карточка прогресса копирования: заголовок со счётчиком, полоса, пояснение и строки по моделям. */
@Composable
internal fun ModelsCopyCard(
    models: ModelsState,
    rows: List<ModelRow>,
    totalBytes: Long,
    modifier: Modifier = Modifier,
) {
    val copying = models as? ModelsState.Copying
    val title = when (models) {
        ModelsState.Done -> "Модели готовы"
        ModelsState.Missing -> "Моделей нет в приложении"
        ModelsState.Failed -> "Не удалось скопировать"
        else -> "Копирование моделей"
    }
    val counter = when {
        copying != null -> "${formatMegabytes(copying.copiedBytes)} из ${formatMegabytes(copying.totalBytes)}"
        models == ModelsState.Done -> formatMegabytes(totalBytes)
        else -> null
    }
    val caption = when (models) {
        ModelsState.Done -> "Всё на месте, можно идти дальше."
        ModelsState.Missing -> "Расшифровка не заработает, пока файлов моделей нет. Продолжить можно."
        ModelsState.Failed -> "Проверьте свободное место. Модели скопируются при первом использовании."
        else -> "Это нужно один раз. Не закрывайте приложение, пока идёт копирование."
    }
    val progress = when {
        models == ModelsState.Done -> 1f
        copying != null && copying.totalBytes > 0 -> copying.copiedBytes.toFloat() / copying.totalBytes
        else -> 0f
    }
    StenoCard(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StenoText(title, modifier = Modifier.weight(1f), style = StenoTheme.typography.label)
            if (counter != null) {
                StenoText(
                    counter,
                    style = StenoTheme.typography.label.tabular(),
                    color = StenoTheme.colors.primaryInk,
                )
            }
        }
        StenoLinearProgress(
            progress = progress,
            modifier = Modifier.padding(top = StenoTheme.spacing.s3, bottom = StenoTheme.spacing.s2),
        )
        StenoText(caption, style = StenoTheme.typography.caption, color = StenoTheme.colors.text2)
        Column(Modifier.padding(top = StenoTheme.spacing.s2)) {
            rows.forEach { ModelRowItem(it) }
        }
    }
}

@Composable
private fun ModelRowItem(row: ModelRow) {
    val colors = StenoTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = StenoTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StenoTheme.spacing.s3),
    ) {
        StatusNode(row.status)
        StenoText(
            row.title,
            modifier = Modifier.weight(1f),
            style = StenoTheme.typography.bodySmall,
            color = if (row.status == ModelRowStatus.Pending) colors.text2 else colors.text,
        )
        StenoText(
            formatMegabytes(row.sizeBytes),
            style = StenoTheme.typography.caption.tabular(),
            color = colors.text2,
        )
    }
}

/** Квадратный маркер 24 dp: галочка, текущая модель, ожидание или предупреждение. */
@Composable
private fun StatusNode(status: ModelRowStatus) {
    val colors = StenoTheme.colors
    val background = when (status) {
        ModelRowStatus.Done -> colors.success
        ModelRowStatus.Active -> colors.primary
        ModelRowStatus.Pending -> colors.surface2
        ModelRowStatus.Unavailable -> colors.warningSoft
    }
    Box(
        modifier = Modifier.size(StenoTheme.spacing.s6).clip(StenoTheme.shapes.sm).background(background),
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            ModelRowStatus.Done -> StenoIcon(
                StenoIcons.Check,
                contentDescription = "Готово",
                tint = colors.onPrimary,
                size = StenoTheme.spacing.s4,
            )
            ModelRowStatus.Unavailable -> StenoIcon(
                StenoIcons.Warning,
                contentDescription = "Нет файла",
                tint = colors.warningInk,
                size = StenoTheme.spacing.s4,
            )
            ModelRowStatus.Active -> StenoIcon(
                StenoIcons.Import,
                contentDescription = "Копируется",
                tint = colors.onPrimary,
                size = StenoTheme.spacing.s4,
            )
            ModelRowStatus.Pending -> Unit
        }
    }
}

private val previewRows = listOf(
    ModelRow(EngineKind.Diarization, "Определение говорящих", 107_000_000, ModelRowStatus.Done),
    ModelRow(EngineKind.SpeechRecognition, "Распознавание речи", 232_000_000, ModelRowStatus.Active),
)

@Preview(showBackground = true)
@Composable
private fun CopyingLightPreview() = StenoThemedSurface(darkTheme = false) {
    ModelsCopyCard(
        ModelsState.Copying(150_000_000, 339_000_000),
        previewRows,
        339_000_000,
        Modifier.padding(StenoTheme.spacing.s4),
    )
}

@Preview(showBackground = true)
@Composable
private fun CopyingDarkPreview() = StenoThemedSurface(darkTheme = true) {
    ModelsCopyCard(
        ModelsState.Copying(150_000_000, 339_000_000),
        previewRows,
        339_000_000,
        Modifier.padding(StenoTheme.spacing.s4),
    )
}

@Preview(showBackground = true)
@Composable
private fun DoneLightPreview() = StenoThemedSurface(darkTheme = false) {
    ModelsCopyCard(
        ModelsState.Done,
        previewRows.map { it.copy(status = ModelRowStatus.Done) },
        339_000_000,
        Modifier.padding(StenoTheme.spacing.s4),
    )
}

@Preview(showBackground = true)
@Composable
private fun DoneDarkPreview() = StenoThemedSurface(darkTheme = true) {
    ModelsCopyCard(
        ModelsState.Done,
        previewRows.map { it.copy(status = ModelRowStatus.Done) },
        339_000_000,
        Modifier.padding(StenoTheme.spacing.s4),
    )
}
