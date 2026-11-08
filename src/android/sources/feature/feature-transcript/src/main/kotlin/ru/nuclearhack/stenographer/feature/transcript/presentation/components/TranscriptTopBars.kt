package ru.nuclearhack.stenographer.feature.transcript.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import ru.nuclearhack.stenographer.common.uikit.components.bar.StenoTopBar
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.field.StenoSearchField
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.feature.transcript.presentation.SearchState

/** Верхняя панель готовой расшифровки: назад, поиск, «Ещё». */
@Composable
internal fun DoneTopBar(
    title: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onProtocol: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StenoTopBar(
        title = title,
        modifier = modifier,
        onBack = onBack,
        actions = {
            StenoIconButton(
                icon = StenoIcons.Search,
                contentDescription = "Поиск по тексту",
                onClick = onSearch,
                variant = StenoIconButtonVariant.Ghost,
            )
            StenoIconButton(
                icon = StenoIcons.Protocol,
                contentDescription = "Протокол встречи",
                onClick = onProtocol,
                variant = StenoIconButtonVariant.Ghost,
            )
            StenoIconButton(
                icon = StenoIcons.More,
                contentDescription = "Ещё",
                onClick = onMore,
                variant = StenoIconButtonVariant.Ghost,
            )
        },
    )
}

/** Панель поиска вместо верхней: закрыть, поле со счётчиком «2 из 5», предыдущее и следующее совпадение. */
@Composable
internal fun SearchTopBar(
    search: SearchState,
    onQueryChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = StenoTheme.spacing
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val counter = when {
        search.query.isBlank() -> null
        search.matches.isEmpty() -> "нет"
        else -> "${search.current + 1} из ${search.matches.size}"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = spacing.s16 + spacing.s1)
            .padding(start = spacing.s1, end = spacing.s2, top = spacing.s2),
        horizontalArrangement = Arrangement.spacedBy(spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StenoIconButton(
            icon = StenoIcons.Back,
            contentDescription = "Закрыть поиск",
            onClick = onClose,
            variant = StenoIconButtonVariant.Ghost,
        )
        StenoSearchField(
            query = search.query,
            onQueryChange = onQueryChange,
            modifier = Modifier.weight(1f).focusRequester(focus),
            matchCounter = counter,
        )
        StenoIconButton(
            icon = StenoIcons.Chevron,
            contentDescription = "Предыдущее совпадение",
            onClick = onPrevious,
            modifier = Modifier.rotate(-QUARTER_TURN),
            variant = StenoIconButtonVariant.Ghost,
            enabled = search.matches.isNotEmpty(),
        )
        StenoIconButton(
            icon = StenoIcons.Chevron,
            contentDescription = "Следующее совпадение",
            onClick = onNext,
            modifier = Modifier.rotate(QUARTER_TURN),
            variant = StenoIconButtonVariant.Ghost,
            enabled = search.matches.isNotEmpty(),
        )
    }
}

private const val QUARTER_TURN = 90f
