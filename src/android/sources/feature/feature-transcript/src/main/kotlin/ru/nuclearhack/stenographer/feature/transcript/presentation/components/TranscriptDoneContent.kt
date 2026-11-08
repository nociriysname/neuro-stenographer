package ru.nuclearhack.stenographer.feature.transcript.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import ru.nuclearhack.stenographer.common.uikit.components.audio.StenoMiniPlayer
import ru.nuclearhack.stenographer.common.uikit.components.control.StenoSegmented
import ru.nuclearhack.stenographer.common.uikit.components.state.StenoStatePanel
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.components.timeline.StenoRailMarker
import ru.nuclearhack.stenographer.common.uikit.components.timeline.StenoTimelineRail
import ru.nuclearhack.stenographer.common.uikit.format.formatTimestamp
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIllustrations
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.core.model.Utterance
import ru.nuclearhack.stenographer.feature.transcript.presentation.SearchState
import ru.nuclearhack.stenographer.feature.transcript.presentation.TranscriptLayout
import ru.nuclearhack.stenographer.feature.transcript.presentation.TranscriptPhase
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.TranscriptPlayer
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.buildTimeline
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.playingUtterance
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.utteranceIndexAt
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.viewportRange

/**
 * Результат: переключатель «Диалог» / «Стенограмма», лента спикеров слева и список реплик.
 * Тап и перетаскивание по ленте прокручивают список к реплике на этом моменте записи.
 */
@Composable
internal fun TranscriptDoneContent(
    done: TranscriptPhase.Done,
    audioDurationMs: Long?,
    layout: TranscriptLayout,
    search: SearchState,
    onLayoutSelect: (TranscriptLayout) -> Unit,
    modifier: Modifier = Modifier,
    player: TranscriptPlayer? = null,
) {
    val spacing = StenoTheme.spacing
    val utterances = done.transcript.utterances
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.screen),
            verticalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            StenoSegmented(
                options = TranscriptLayout.entries.map { it.title },
                selectedIndex = layout.ordinal,
                onSelect = { onLayoutSelect(TranscriptLayout.entries[it]) },
                modifier = Modifier.padding(top = spacing.s1),
            )
            StenoText(
                "${formatTimestamp(audioDurationMs ?: 0L)} · говорящих: ${done.transcript.speakers.size} · " +
                    "реплик: ${utterances.size}",
                style = StenoTheme.typography.caption,
                color = StenoTheme.colors.text2,
            )
        }
        if (utterances.isEmpty()) {
            StenoStatePanel(
                title = "Речь в записи не найдена",
                message = "Попробуйте другую запись или проверьте уровень громкости.",
                illustration = StenoIllustrations.EmptyHistory,
                modifier = Modifier.weight(1f),
            )
        } else {
            ConversationList(done, audioDurationMs, layout, search, Modifier.weight(1f), player)
            if (player != null) {
                val timeline = remember(done.transcript, audioDurationMs) {
                    buildTimeline(done.transcript, audioDurationMs)
                }
                StenoMiniPlayer(
                    segments = timeline.segments,
                    durationMs = timeline.durationMs,
                    positionMs = player.positionMs,
                    playing = player.playing,
                    onPlayPause = player::toggle,
                    onSeek = player::seekTo,
                    speed = player.speed,
                    onSpeedClick = player::cycleSpeed,
                    skipSilence = player.skipSilence,
                    onSkipSilenceToggle = player::toggleSkipSilence,
                )
            }
        }
    }
}

@Composable
private fun ConversationList(
    done: TranscriptPhase.Done,
    audioDurationMs: Long?,
    layout: TranscriptLayout,
    search: SearchState,
    modifier: Modifier = Modifier,
    player: TranscriptPlayer? = null,
) {
    val colors = StenoTheme.colors
    val spacing = StenoTheme.spacing
    val utterances = done.transcript.utterances
    val timeline = remember(done.transcript, audioDurationMs) { buildTimeline(done.transcript, audioDurationMs) }
    val matchesByUtterance = remember(search.matches) {
        search.matches.withIndex().groupBy { it.value.utteranceIndex }
    }
    val markers = remember(search.matches, utterances, colors) {
        search.matches.map { it.utteranceIndex }.distinct()
            .map { StenoRailMarker(utterances[it].startMs, colors.accent) }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val viewport by remember(utterances, timeline) {
        derivedStateOf { listState.viewport(utterances, timeline.durationMs) }
    }
    val currentUtterance = search.matches.getOrNull(search.current)?.utteranceIndex
    val playingIndex = player?.takeIf { it.playing || it.positionMs > 0 }
        ?.let { playingUtterance(utterances, it.positionMs) }

    // Во время проигрывания список следует за звучащей репликой.
    LaunchedEffect(playingIndex) {
        if (player?.playing == true && playingIndex != null) listState.animateScrollToItem(playingIndex)
    }

    LaunchedEffect(search.current, search.matches) {
        currentUtterance?.let { listState.animateScrollToItem(it) }
    }

    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Row(modifier.padding(top = spacing.s2)) {
        StenoTimelineRail(
            segments = timeline.segments,
            durationMs = timeline.durationMs,
            modifier = Modifier.padding(start = spacing.s2, top = spacing.s1, bottom = navigationBottom + spacing.s3),
            viewportStartMs = viewport.first,
            viewportEndMs = viewport.last,
            positionMs = player?.positionMs?.takeIf { player.playing || it > 0 },
            markers = markers,
            onSeek = { ms ->
                player?.seekTo(ms)
                scope.launch { listState.scrollToItem(utteranceIndexAt(utterances, ms)) }
            },
            width = if (layout == TranscriptLayout.Dialog) spacing.s10 + spacing.s1 else spacing.s4,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = spacing.s3,
                end = spacing.s4,
                top = spacing.s1,
                bottom = spacing.s6 + navigationBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(
                if (layout == TranscriptLayout.Dialog) spacing.s3 else spacing.s1,
            ),
        ) {
            itemsIndexed(utterances, key = { index, _ -> index }) { index, utterance ->
                val text = highlightedText(utterance.text, matchesByUtterance[index], search.current)
                val speakerIndex = timeline.speakerIndexByUtterance[index]
                val current = index == currentUtterance || index == playingIndex
                // Тап по реплике — проиграть её с начала.
                val itemModifier = if (player != null) {
                    Modifier.clickable(onClickLabel = "Проиграть реплику") { player.playFrom(utterance.startMs) }
                } else {
                    Modifier
                }
                if (layout == TranscriptLayout.Dialog) {
                    DialogMessage(utterance, text, speakerIndex, current = current, modifier = itemModifier)
                } else {
                    StenogramItem(utterance, text, speakerIndex, current = current, modifier = itemModifier)
                }
            }
            item(key = SUMMARY_KEY) { ProcessingSummary(done, detailed = false, Modifier.padding(top = spacing.s3)) }
        }
    }
}

private const val SUMMARY_KEY = "summary"

private fun LazyListState.viewport(
    utterances: List<Utterance>,
    durationMs: Long,
): LongRange {
    val visible = layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) return 0L..0L
    return viewportRange(utterances, visible.first().index, visible.last().index, durationMs)
}
