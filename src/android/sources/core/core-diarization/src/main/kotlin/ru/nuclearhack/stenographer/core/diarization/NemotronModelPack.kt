package ru.nuclearhack.stenographer.core.diarization

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import ru.nuclearhack.stenographer.common.models.ModelInstaller
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.ModelPack

/** Веса Nemotron-3-Diarization для подготовки моделей при первом запуске. */
@Inject
@ContributesIntoSet(AppScope::class, binding<ModelPack>())
class NemotronModelPack(private val installer: ModelInstaller) : ModelPack {
    override val kind = EngineKind.Diarization
    override val title = "Определение говорящих"
    override val sizeBytes = NemotronDiarizer.MODEL.sizeBytes

    override fun isInstalled() = installer.isInstalled(NemotronDiarizer.MODEL)

    override fun isAvailable() = installer.isAvailable(NemotronDiarizer.MODEL)

    override suspend fun install(onBytesCopied: (Long) -> Unit) {
        installer.install(NemotronDiarizer.MODEL, onBytesCopied)
    }
}
