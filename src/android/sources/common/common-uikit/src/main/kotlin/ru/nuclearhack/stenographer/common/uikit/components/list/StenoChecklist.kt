package ru.nuclearhack.stenographer.common.uikit.components.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.badge.StenoBadge
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/*
 * Блоки документа из макета S08 «Протокол встречи»: заголовок секции с плиткой-иконкой, пункт-решение
 * с галочкой и задача с квадратным чекбоксом и строкой «исполнитель · срок».
 */

/** Заголовок секции: плитка 32 на мягком основном, название 18/600 и необязательный счётчик справа. */
@Composable
fun StenoSectionHeader(
    icon: StenoIcons,
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(32.dp)
                .background(StenoTheme.colors.primarySoft, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            StenoIcon(icon, contentDescription = null, tint = StenoTheme.colors.primaryInk, size = 20.dp)
        }
        Spacer(Modifier.width(10.dp))
        StenoText(
            title,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = StenoTheme.typography.subtitle,
        )
        if (count != null) StenoBadge("$count")
    }
}

/** Пункт с зелёной галочкой — принятое решение. */
@Composable
fun StenoCheckItem(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StenoIcon(
            StenoIcons.Check,
            contentDescription = null,
            tint = StenoTheme.colors.success,
            size = 20.dp,
            modifier = Modifier.padding(top = 2.dp),
        )
        StenoText(text, modifier = Modifier.weight(1f), style = StenoTheme.typography.body)
    }
}

/**
 * Задача: квадратный чекбокс 22, текст и строка метаданных ([meta], например «Анна · до 12 ноября»)
 * с бейджем исполнителя [badge] слева.
 */
@Composable
fun StenoTaskItem(
    text: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    badge: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .padding(top = 1.dp)
                .size(22.dp)
                .border(2.dp, StenoTheme.colors.lineStrong, RoundedCornerShape(7.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StenoText(text, style = StenoTheme.typography.body)
            if (meta != null || badge != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    badge?.invoke()
                    if (meta != null) {
                        StenoText(meta, style = StenoTheme.typography.caption, color = StenoTheme.colors.text2)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistPreviewContent() {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StenoSectionHeader(StenoIcons.Check, "Решения")
        StenoCheckItem("Первый этап запускаем 1 декабря.")
        StenoSectionHeader(StenoIcons.Tasks, "Задачи", count = 1)
        StenoTaskItem("Подготовить смету по отделам", meta = "Анна · до 12 ноября")
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun StenoChecklistLightPreview() = StenoThemedSurface(darkTheme = false) { ChecklistPreviewContent() }

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun StenoChecklistDarkPreview() = StenoThemedSurface(darkTheme = true) { ChecklistPreviewContent() }
