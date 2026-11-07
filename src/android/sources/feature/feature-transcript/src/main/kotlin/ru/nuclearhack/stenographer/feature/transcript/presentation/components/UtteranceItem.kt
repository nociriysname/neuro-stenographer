package ru.nuclearhack.stenographer.feature.transcript.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.format.formatTimestamp
import ru.nuclearhack.stenographer.core.model.Utterance

@Composable
internal fun UtteranceItem(utterance: Utterance, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(utterance.speaker.value, style = MaterialTheme.typography.labelLarge, color = color)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${formatTimestamp(utterance.startMs)} – ${formatTimestamp(utterance.endMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = utterance.text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 18.dp, top = 2.dp),
        )
    }
}
