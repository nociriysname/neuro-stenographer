package ru.nuclearhack.stenographer.common.uikit.components.surface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/** Крупная карточка действия главного экрана («Записать», «Импортировать»). */
@Composable
fun StenoActionCard(
    title: String,
    subtitle: String,
    icon: StenoIcons,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: StenoActionCardStyle = StenoActionCardStyle.Primary,
) {
    val colors = StenoTheme.colors
    val primary = style == StenoActionCardStyle.Primary
    val shape = StenoTheme.shapes.xl
    val glow = Shadow(
        radius = 20.dp,
        color = colors.primary.copy(alpha = 0.26f),
        offset = DpOffset(0.dp, 8.dp),
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(shape, if (primary) glow else StenoTheme.elevation.card)
            .clip(shape)
            .background(if (primary) colors.primary else colors.bg)
            .then(if (primary) Modifier else Modifier.border(BorderStroke(1.dp, colors.lineStrong), shape))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        if (primary) {
            DecorWave(
                color = colors.onPrimary.copy(alpha = 0.22f),
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .heightIn(min = if (primary) 112.dp else 88.dp)
                .padding(horizontal = 20.dp, vertical = if (primary) 16.dp else 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val boxSize = if (primary) 56.dp else 52.dp
            Box(
                modifier = Modifier
                    .size(boxSize)
                    .clip(RoundedCornerShape(if (primary) 18.dp else 16.dp))
                    .background(if (primary) colors.onPrimary.copy(alpha = 0.16f) else colors.primarySoft),
                contentAlignment = Alignment.Center,
            ) {
                StenoIcon(
                    icon,
                    contentDescription = null,
                    tint = if (primary) colors.onPrimary else colors.primaryInk,
                    size = 32.dp,
                )
            }
            Column(Modifier.weight(1f)) {
                StenoText(
                    title,
                    style = StenoTheme.typography.subtitle,
                    color = if (primary) colors.onPrimary else colors.text,
                )
                StenoText(
                    subtitle,
                    style = StenoTheme.typography.bodySmall,
                    color = if (primary) colors.onPrimary.copy(alpha = 0.86f) else colors.text2,
                )
            }
        }
    }
}

/** Декор основной карточки из макета: приглушённая волна 104×64 в правом нижнем углу. */
@Composable
private fun DecorWave(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 104.dp, height = 64.dp)) {
        val heights = listOf(0.3f, 0.55f, 0.85f, 0.45f, 1f, 0.6f, 0.35f, 0.75f, 0.5f, 0.25f)
        val bar = 4.dp.toPx()
        val step = size.width / heights.size
        heights.forEachIndexed { index, fraction ->
            val barHeight = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(index * step + (step - bar) / 2, size.height - barHeight),
                size = Size(bar, barHeight),
                cornerRadius = CornerRadius(bar / 2),
            )
        }
    }
}

@Composable
private fun ActionCardPreview(dark: Boolean) {
    StenoThemedSurface(darkTheme = dark) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StenoActionCard("Записать", "Диктофон с расшифровкой", StenoIcons.Mic, onClick = ::previewNoop)
            StenoActionCard(
                "Импортировать",
                "Аудиофайл с телефона",
                StenoIcons.Import,
                onClick = ::previewNoop,
                style = StenoActionCardStyle.Secondary,
            )
        }
    }
}

@Preview
@Composable
private fun StenoActionCardLightPreview() = ActionCardPreview(false)

@Preview
@Composable
private fun StenoActionCardDarkPreview() = ActionCardPreview(true)

private fun previewNoop() = Unit
