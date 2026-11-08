package ru.nuclearhack.stenographer.core.domain.engine

/**
 * Веса одного движка, которые при первом запуске копируются из APK в память приложения.
 * Реализации живут рядом с движками (core-asr, core-diarization) и собираются Metro во множество.
 */
interface ModelPack {
    val kind: EngineKind
    val title: String
    val sizeBytes: Long

    /** Веса уже распакованы (или подложены через adb) — движок стартует без копирования. */
    fun isInstalled(): Boolean

    /** Веса есть хотя бы в APK — их можно установить. */
    fun isAvailable(): Boolean

    /** Копирует веса; [onBytesCopied] получает прирост скопированных байт. */
    suspend fun install(onBytesCopied: (Long) -> Unit)
}
