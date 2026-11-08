package ru.nuclearhack.stenographer.feature.onboarding.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButtonSize
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.format.formatMegabytes
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIllustrations
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.feature.onboarding.presentation.ModelRowStatus
import ru.nuclearhack.stenographer.feature.onboarding.presentation.ModelsState
import ru.nuclearhack.stenographer.feature.onboarding.presentation.OnboardingState
import ru.nuclearhack.stenographer.feature.onboarding.presentation.OnboardingStep

private data class Feature(val icon: StenoIcons, val title: String, val caption: String)

private class PageSpec(
    val illustration: StenoIllustrations,
    val title: String,
    val body: String,
    val features: List<Feature>,
)

/** Содержимое одного шага: иллюстрация, заголовок, текст и список преимуществ либо карточка копирования. */
@Composable
internal fun OnboardingStepPage(
    step: OnboardingStep,
    state: OnboardingState,
    onRetryCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = pageSpec(step, state)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StenoTheme.spacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = spec.illustration.painter,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(StenoTheme.spacing.s16 * 3),
            contentScale = ContentScale.Fit,
        )
        StenoText(
            spec.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = StenoTheme.spacing.s2)
                .semantics { heading() },
            style = StenoTheme.typography.display,
        )
        StenoText(
            spec.body,
            modifier = Modifier.fillMaxWidth().padding(top = StenoTheme.spacing.s2),
            color = StenoTheme.colors.text2,
        )
        Column(Modifier.fillMaxWidth().padding(top = StenoTheme.spacing.s4)) {
            if (step == OnboardingStep.Models) {
                ModelsCopyCard(state.models, state.rows, state.totalBytes)
                if (state.models == ModelsState.Failed) {
                    StenoButton(
                        text = "Повторить",
                        onClick = onRetryCopy,
                        modifier = Modifier.padding(top = StenoTheme.spacing.s3),
                        variant = StenoButtonVariant.Secondary,
                        size = StenoButtonSize.Medium,
                        icon = StenoIcons.Retry,
                        fillWidth = true,
                    )
                }
            } else {
                spec.features.forEach { OnboardingFeatureRow(it.icon, it.title, it.caption) }
            }
        }
    }
}

private fun pageSpec(step: OnboardingStep, state: OnboardingState): PageSpec = when (step) {
    OnboardingStep.Welcome -> PageSpec(
        StenoIllustrations.Listen,
        "Расшифровка разговоров",
        "Кто говорил, когда и что — прямо на телефоне, на русском языке.",
        listOf(
            Feature(StenoIcons.Speaker, "Кто говорит", "голоса разделены по людям"),
            Feature(StenoIcons.Wave, "Когда и что", "текст с таймкодами и словами"),
            Feature(StenoIcons.AudioFile, "Файл или микрофон", "расшифруйте запись или запишите разговор"),
        ),
    )
    OnboardingStep.Privacy -> PageSpec(
        StenoIllustrations.Private,
        "Всё остаётся у вас",
        "Никаких серверов и аккаунтов. Данные не покидают устройство.",
        listOf(
            Feature(StenoIcons.Chip, "Всё считается на телефоне", "нейросети работают локально"),
            Feature(StenoIcons.NoCloud, "Ничего не уходит в облако", "ни звук, ни текст"),
            Feature(StenoIcons.Shield, "Работает без интернета", "хоть в самолёте, хоть в подвале"),
        ),
    )
    OnboardingStep.Models -> modelsSpec(state)
    OnboardingStep.Microphone -> PageSpec(
        StenoIllustrations.Microphone,
        "Нужен микрофон",
        "Чтобы записывать разговоры, приложению нужен доступ к микрофону. Спросим один раз.",
        listOf(
            Feature(StenoIcons.Mic, "Только во время записи", "фоном микрофон не слушает"),
            Feature(StenoIcons.Shield, "Звук остаётся у вас", "файл хранится только на устройстве"),
            Feature(StenoIcons.Info, "Доступ можно отозвать", "в настройках Android в любой момент"),
        ),
    )
}

private fun modelsSpec(state: OnboardingState): PageSpec {
    val failed = state.models == ModelsState.Missing || state.models == ModelsState.Failed
    return when {
        state.models == ModelsState.Missing -> PageSpec(
            StenoIllustrations.ErrorModel,
            "Моделей нет в приложении",
            "В этой сборке не нашлось файлов нейросетей.",
            emptyList(),
        )
        failed -> PageSpec(
            StenoIllustrations.ErrorModel,
            "Копирование прервалось",
            "Модели не удалось записать в память приложения.",
            emptyList(),
        )
        state.models == ModelsState.Done && state.rows.all { it.status == ModelRowStatus.Done } ->
            PageSpec(
                StenoIllustrations.Models,
                "Модели на месте",
                "Нейросети уже в памяти приложения (${formatMegabytes(state.totalBytes)}). Копировать больше не нужно.",
                emptyList(),
            )
        else -> PageSpec(
            StenoIllustrations.Models,
            "Подготовка моделей",
            "При первом запуске приложение копирует около ${formatMegabytes(state.totalBytes)} нейросетей. " +
                "Это нужно один раз.",
            emptyList(),
        )
    }
}
