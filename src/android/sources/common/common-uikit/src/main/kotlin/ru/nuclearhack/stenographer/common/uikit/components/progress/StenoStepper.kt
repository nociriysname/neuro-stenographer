package ru.nuclearhack.stenographer.common.uikit.components.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/** Состояние шага [StenoStepper]. */
enum class StenoStepState { Done, Active, Pending }

/** Шаг пошагового процесса: [caption] — пояснение под заголовком («готово · 0:08», «идёт · 64%»). */
@Immutable
data class StenoStep(val title: String, val caption: String?, val state: StenoStepState)

/** Вертикальный степпер этапов обработки: узлы 32 dp, между ними линия 2 dp (primary после выполненного шага). */
@Composable
fun StenoStepper(steps: List<StenoStep>, modifier: Modifier = Modifier) {
    val colors = StenoTheme.colors
    Column(modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            if (index > 0) {
                val done = steps[index - 1].state == StenoStepState.Done
                Box(
                    Modifier
                        .padding(start = 15.dp)
                        .size(width = 2.dp, height = 16.dp)
                        .background(if (done) colors.primary else colors.lineStrong),
                )
            }
            StepRow(index = index, step = step)
        }
    }
}

@Composable
private fun StepRow(index: Int, step: StenoStep) {
    val colors = StenoTheme.colors
    val state = when (step.state) {
        StenoStepState.Done -> "готово"
        StenoStepState.Active -> "идёт"
        StenoStepState.Pending -> "ждёт очереди"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .semantics(mergeDescendants = true) { stateDescription = state },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepNode(number = index + 1, state = step.state)
        Column(Modifier.weight(1f)) {
            StenoText(
                text = step.title,
                style = StenoTheme.typography.label,
                color = if (step.state == StenoStepState.Pending) colors.text2 else colors.text,
            )
            if (step.caption != null) {
                StenoText(
                    text = step.caption,
                    style = StenoTheme.typography.caption,
                    color = if (step.state == StenoStepState.Pending) colors.text3 else colors.text2,
                )
            }
        }
    }
}

@Composable
private fun StepNode(number: Int, state: StenoStepState) {
    val nodeShape = RoundedCornerShape(11.dp)
    val colors = StenoTheme.colors
    val (fill, border, content) = when (state) {
        StenoStepState.Done -> Triple(colors.primary, colors.primary, colors.onPrimary)
        StenoStepState.Active -> Triple(colors.primarySoft, colors.primary, colors.primaryInk)
        StenoStepState.Pending -> Triple(colors.bg, colors.lineStrong, colors.text3)
    }
    Box(
        Modifier
            .size(32.dp)
            .background(fill, nodeShape)
            .border(2.dp, border, nodeShape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        if (state == StenoStepState.Done) {
            StenoIcon(StenoIcons.Check, contentDescription = null, tint = content, size = 20.dp)
        } else {
            StenoText(
                text = number.toString(),
                style = StenoTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                color = content,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun StenoStepperLightPreview() = StenoStepperPreview(darkTheme = false)

@Preview(name = "Dark", showBackground = true)
@Composable
private fun StenoStepperDarkPreview() = StenoStepperPreview(darkTheme = true)

@Composable
private fun StenoStepperPreview(darkTheme: Boolean) {
    StenoThemedSurface(darkTheme) {
        StenoStepper(
            steps = listOf(
                StenoStep("Декодирование", "готово · 0:08", StenoStepState.Done),
                StenoStep("Кто говорит", "идёт · 64%", StenoStepState.Active),
                StenoStep("Распознавание речи", "ждёт очереди", StenoStepState.Pending),
                StenoStep("Постобработка", "ждёт очереди", StenoStepState.Pending),
            ),
            modifier = Modifier.padding(20.dp),
        )
    }
}
