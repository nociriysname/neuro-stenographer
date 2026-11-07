package ru.nuclearhack.stenographer.feature.home.presentation

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import ru.nuclearhack.stenographer.common.uikit.theme.StenographerTheme
import ru.nuclearhack.stenographer.common.uikit.udf.CollectEffects
import ru.nuclearhack.stenographer.core.domain.engine.EngineInfo
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.model.AudioRef

@Composable
fun HomeScreen(
    onOpenTranscript: (AudioRef) -> Unit,
    viewModel: HomeViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // Постоянный доступ к файлу — чтобы потом открыть расшифровку из истории без повторного выбора.
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        viewModel.onAction(HomeAction.AudioPicked(uri?.toString()))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is HomeEffect.OpenTranscript -> onOpenTranscript(effect.audio)
        }
    }

    HomeContent(state = state, onPickAudio = { pickAudio.launch(arrayOf("audio/*")) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(state: HomeState, onPickAudio: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Нейро-стенографист") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Расшифровка разговора прямо на телефоне: кто говорил, когда и что. Без интернета.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onPickAudio, modifier = Modifier.fillMaxWidth()) {
                Text("Выбрать аудиофайл")
            }
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text("Записать разговор — скоро")
            }
            EnginesCard(state.engines)
        }
    }
}

@Composable
private fun EnginesCard(engines: List<EngineInfo>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Модели на устройстве", style = MaterialTheme.typography.titleMedium)
            engines.forEach { engine ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (engine.isReady) "●" else "○",
                        color = if (engine.isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("${engine.kind.title()}: ${engine.modelName}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${engine.runtime} · ${if (engine.isReady) "готова" else "не подключена"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun EngineKind.title(): String = when (this) {
    EngineKind.Diarization -> "Диаризация"
    EngineKind.SpeechRecognition -> "Распознавание"
    EngineKind.Llm -> "LLM"
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    StenographerTheme(dynamicColor = false) {
        HomeContent(
            state = HomeState(
                engines = listOf(
                    EngineInfo(EngineKind.Diarization, "Nemotron-3-Diarization q8_0", "NeMo-Speech.cpp (ggml)", isReady = true),
                    EngineInfo(EngineKind.SpeechRecognition, "GigaAM-v3 e2e_rnnt", "sherpa-onnx", isReady = true),
                    EngineInfo(EngineKind.Llm, "Qwen3-1.7B Q4_K_M", "llama.cpp", isReady = false),
                ),
            ),
            onPickAudio = {},
        )
    }
}
