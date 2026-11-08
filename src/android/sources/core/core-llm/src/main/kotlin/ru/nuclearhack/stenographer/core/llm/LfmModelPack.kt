package ru.nuclearhack.stenographer.core.llm

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import ru.nuclearhack.stenographer.common.models.ModelInstaller
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.ModelPack

/** Веса LLM для подготовки моделей при первом запуске. */
@Inject
@ContributesIntoSet(AppScope::class, binding<ModelPack>())
class LfmModelPack(private val installer: ModelInstaller) : ModelPack {
    override val kind = EngineKind.Llm
    override val title = "Протокол встречи"
    override val sizeBytes = LfmModel.ASSET.sizeBytes

    override fun isInstalled() = installer.isInstalled(LfmModel.ASSET)

    override fun isAvailable() = installer.isAvailable(LfmModel.ASSET)

    override suspend fun install(onBytesCopied: (Long) -> Unit) {
        installer.install(LfmModel.ASSET, onBytesCopied)
    }
}
