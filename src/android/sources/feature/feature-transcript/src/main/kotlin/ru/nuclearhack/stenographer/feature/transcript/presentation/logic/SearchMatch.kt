package ru.nuclearhack.stenographer.feature.transcript.presentation.logic

import ru.nuclearhack.stenographer.core.model.Utterance

/** Совпадение поиска: реплика [utteranceIndex] и диапазон символов `[start, end)` в её тексте. */
data class SearchMatch(val utteranceIndex: Int, val start: Int, val end: Int)

/** Все вхождения [query] без учёта регистра, в порядке следования реплик. Пустой запрос — пустой список. */
fun findMatches(utterances: List<Utterance>, query: String): List<SearchMatch> {
    val needle = query.trim()
    if (needle.isEmpty()) return emptyList()
    return buildList {
        utterances.forEachIndexed { index, utterance ->
            var from = 0
            while (true) {
                val at = utterance.text.indexOf(needle, from, ignoreCase = true)
                if (at < 0) break
                add(SearchMatch(index, at, at + needle.length))
                from = at + needle.length
            }
        }
    }
}
