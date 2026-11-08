package ru.nuclearhack.stenographer.feature.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.nuclearhack.stenographer.common.uikit.components.overlay.StenoBottomSheet
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoDivider
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoListRow

/** Компонент и условия, на которых он распространяется. */
internal data class LicenseEntry(val name: String, val terms: String)

/** Модели и открытое ПО, вошедшие в приложение (список сверен с таблицей зависимостей в docs/ARCHITECTURE.md). */
internal val licenseEntries = listOf(
    LicenseEntry("GigaAM-v3", "MIT · Сбер, распознавание речи"),
    LicenseEntry("Nemotron-3-Diarization", "OpenMDW-1.1 · NVIDIA, разделение по говорящим"),
    LicenseEntry("NeMo-Speech.cpp", "Apache-2.0"),
    LicenseEntry("ggml / llama.cpp", "MIT"),
    LicenseEntry("SentencePiece", "Apache-2.0"),
    LicenseEntry("sherpa-onnx и ONNX Runtime", "Apache-2.0 / MIT"),
    LicenseEntry("Golos Text", "OFL-1.1 · шрифт интерфейса"),
    LicenseEntry("Kotlin, AndroidX, Compose", "Apache-2.0"),
    LicenseEntry("Metro", "Apache-2.0"),
)

/** Шторка «Лицензии и модели». */
@Composable
internal fun LicensesSheet(visible: Boolean, onDismiss: () -> Unit) {
    StenoBottomSheet(visible = visible, onDismissRequest = onDismiss, title = "Лицензии и модели") {
        LicensesList()
    }
}

@Composable
internal fun LicensesList(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        licenseEntries.forEachIndexed { index, entry ->
            if (index > 0) StenoDivider()
            StenoListRow(title = entry.name, subtitle = entry.terms)
        }
    }
}
