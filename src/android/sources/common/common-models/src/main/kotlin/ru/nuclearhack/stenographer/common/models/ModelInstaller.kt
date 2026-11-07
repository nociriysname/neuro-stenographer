package ru.nuclearhack.stenographer.common.models

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Файл весов в `assets/models/` и его точный размер — по размеру проверяем, что копия целая. */
data class ModelAsset(
    val fileName: String,
    val sizeBytes: Long,
)

class ModelNotFoundException(message: String) : IOException(message)

/**
 * Доставка весов моделей на устройство.
 *
 * Эксперты ставят APK и сразу работают без интернета, поэтому веса лежат в assets (без сжатия).
 * ggml и llama.cpp открывают модель по пути через mmap, а из APK так нельзя — при первом использовании
 * файл копируется в `noBackupFilesDir/models`.
 *
 * Для разработки модель можно не вшивать в APK, а подложить через adb:
 * `adb push <файл> /sdcard/Android/data/<applicationId>/files/models/` — такой файл берётся как есть.
 */
@Inject
@SingleIn(AppScope::class)
class ModelInstaller(
    private val context: Context,
) {
    private val mutex = Mutex()

    private val installDir: File
        get() = File(context.noBackupFilesDir, "models")

    /** Есть ли модель: уже распакована, подложена через adb или лежит в APK. Дёшево, можно с главного потока. */
    fun isAvailable(asset: ModelAsset): Boolean = findReadyFile(asset) != null || isInAssets(asset)

    /** Путь к готовому файлу модели; при первом вызове копирует его из APK. */
    suspend fun install(asset: ModelAsset): File = mutex.withLock {
        withContext(Dispatchers.IO) {
            findReadyFile(asset) ?: copyFromAssets(asset)
        }
    }

    private fun findReadyFile(asset: ModelAsset): File? {
        val candidates = listOfNotNull(
            File(installDir, asset.fileName),
            context.getExternalFilesDir(MODELS_DIR)?.let { File(it, asset.fileName) },
        )
        return candidates.firstOrNull { it.isFile && it.length() == asset.sizeBytes }
    }

    private fun isInAssets(asset: ModelAsset): Boolean =
        runCatching { context.assets.list(MODELS_DIR)?.contains(asset.fileName) == true }.getOrDefault(false)

    private fun copyFromAssets(asset: ModelAsset): File {
        if (!isInAssets(asset)) {
            throw ModelNotFoundException(
                "Нет файла модели ${asset.fileName}: он не вшит в APK (tools/fetch_models.sh) и не подложен через adb",
            )
        }
        installDir.mkdirs()
        val target = File(installDir, asset.fileName)
        val partial = File(installDir, "${asset.fileName}.part")
        context.assets.open("$MODELS_DIR/${asset.fileName}").use { input ->
            partial.outputStream().use { output -> input.copyTo(output, bufferSize = COPY_BUFFER_BYTES) }
        }
        if (partial.length() != asset.sizeBytes) {
            partial.delete()
            throw IOException("Копия ${asset.fileName} повреждена: ${partial.length()} из ${asset.sizeBytes} байт")
        }
        if (!partial.renameTo(target)) {
            partial.delete()
            throw IOException("Не удалось сохранить ${target.path}")
        }
        return target
    }

    private companion object {
        const val MODELS_DIR = "models"
        const val COPY_BUFFER_BYTES = 1 shl 20
    }
}
