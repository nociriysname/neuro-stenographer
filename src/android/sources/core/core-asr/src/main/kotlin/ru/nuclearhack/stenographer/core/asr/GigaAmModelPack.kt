package ru.nuclearhack.stenographer.core.asr

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import ru.nuclearhack.stenographer.common.models.ModelInstaller
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.ModelPack

/** Веса GigaAM-v3 выбранного варианта: копируются при первом запуске, чтобы первая расшифровка не ждала. */
@Inject
@ContributesIntoSet(AppScope::class, binding<ModelPack>())
class GigaAmModelPack(
    private val installer: ModelInstaller,
    private val recognizer: GigaAmSpeechRecognizer,
) : ModelPack {
    private val variant: GigaAmVariant get() = recognizer.variant ?: GigaAmVariant.E2eRnnt

    override val kind = EngineKind.SpeechRecognition
    override val title = "Распознавание речи"
    override val sizeBytes: Long get() = variant.assets.sumOf { it.sizeBytes }

    override fun isInstalled() = installer.isInstalled(variant.assets)

    override fun isAvailable() = installer.isAvailable(variant.assets)

    override suspend fun install(onBytesCopied: (Long) -> Unit) {
        installer.install(variant.assets, onBytesCopied)
    }
}
