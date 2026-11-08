package ru.nuclearhack.stenographer.core.domain.usecase

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.ModelPack

/** Состояние весов одного движка для онбординга и настроек. */
data class ModelPackStatus(
    val kind: EngineKind,
    val title: String,
    val sizeBytes: Long,
    val installed: Boolean,
    val available: Boolean,
)

data class ModelStorageStatus(val packs: List<ModelPackStatus>) {
    /** Сколько места заняли распакованные веса. */
    val usedBytes: Long get() = packs.filter { it.installed }.sumOf { it.sizeBytes }
    val allReady: Boolean get() = packs.all { it.installed }
}

sealed interface ModelPreparation {
    data class Copying(val copiedBytes: Long, val totalBytes: Long, val current: EngineKind) : ModelPreparation

    data class Done(val status: ModelStorageStatus) : ModelPreparation

    /** Каких-то весов нет даже в APK: их нужно подложить (tools/fetch_models.sh). */
    data class Missing(val packs: List<ModelPackStatus>) : ModelPreparation
}

private val PipelineOrder = EngineKind.entries

private fun Set<ModelPack>.ordered(): List<ModelPack> = sortedBy { PipelineOrder.indexOf(it.kind) }

private fun ModelPack.status() = ModelPackStatus(kind, title, sizeBytes, isInstalled(), isAvailable())

@Inject
class GetModelStorageStatusUseCase(private val packs: Set<ModelPack>) {
    operator fun invoke(): ModelStorageStatus = ModelStorageStatus(packs.ordered().map { it.status() })
}

/**
 * Копирует из APK все ещё не распакованные веса, сообщая прогресс в байтах. Повторный запуск дешёвый:
 * установленные модели пропускаются.
 */
@Inject
class PrepareModelsUseCase(private val packs: Set<ModelPack>) {
    operator fun invoke(): Flow<ModelPreparation> = channelFlow {
        val ordered = packs.ordered()
        val missing = ordered.filterNot { it.isAvailable() }.map { it.status() }
        if (missing.isNotEmpty()) {
            send(ModelPreparation.Missing(missing))
            return@channelFlow
        }
        val pending = ordered.filterNot { it.isInstalled() }
        val total = pending.sumOf { it.sizeBytes }
        var copied = 0L
        for (pack in pending) {
            send(ModelPreparation.Copying(copied, total, pack.kind))
            pack.install { delta ->
                copied += delta
                trySend(ModelPreparation.Copying(copied, total, pack.kind))
            }
        }
        send(ModelPreparation.Done(ModelStorageStatus(ordered.map { it.status() })))
    }.conflate()
}
