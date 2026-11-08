package ru.nuclearhack.stenographer.common.models

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Файл весов в `assets/models/` и его точный размер — по размеру проверяем, что копия целая.
 * [fileName] может содержать подкаталог: модель из нескольких файлов лежит в своей папке.
 */
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
 * `adb push <файл> /sdcard/Android/data/<applicationId>/files/models/` — такой файл берётся как есть
 * (`tools/fetch_models.sh --adb` делает это для всех моделей).
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

    /** Файл уже распакован или подложен через adb: движок стартует без копирования. */
    fun isInstalled(asset: ModelAsset): Boolean = findReadyFile(asset) != null

    /**
     * Путь к готовому файлу модели; при первом вызове копирует его из APK.
     * [onBytesCopied] получает прирост скопированных байт — для прогресса в онбординге.
     */
    suspend fun install(asset: ModelAsset, onBytesCopied: (Long) -> Unit = {}): File = mutex.withLock {
        withContext(Dispatchers.IO) {
            findReadyFile(asset) ?: copyFromAssets(asset, onBytesCopied)
        }
    }

    /** Модель из нескольких файлов: все должны быть на месте. */
    fun isAvailable(assets: List<ModelAsset>): Boolean = assets.all(::isAvailable)

    fun isInstalled(assets: List<ModelAsset>): Boolean = assets.all(::isInstalled)

    suspend fun install(assets: List<ModelAsset>, onBytesCopied: (Long) -> Unit = {}): List<File> =
        assets.map { install(it, onBytesCopied) }

    private fun findReadyFile(asset: ModelAsset): File? {
        val candidates = listOfNotNull(
            File(installDir, asset.fileName),
            context.getExternalFilesDir(MODELS_DIR)?.let { File(it, asset.fileName) },
        )
        return candidates.firstOrNull { it.isFile && it.length() == asset.sizeBytes }
    }

    private fun isInAssets(asset: ModelAsset): Boolean {
        val path = "$MODELS_DIR/${asset.fileName}"
        val dir = path.substringBeforeLast('/')
        val name = path.substringAfterLast('/')
        return runCatching { context.assets.list(dir)?.contains(name) == true }.getOrDefault(false)
    }

    private suspend fun copyFromAssets(asset: ModelAsset, onBytesCopied: (Long) -> Unit): File {
        if (!isInAssets(asset)) {
            throw ModelNotFoundException(
                "Нет файла модели ${asset.fileName}: он не вшит в APK (tools/fetch_models.sh) и не подложен через adb",
            )
        }
        val target = File(installDir, asset.fileName)
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, "${target.name}.part")
        context.assets.open("$MODELS_DIR/${asset.fileName}").use { input ->
            partial.outputStream().use { output -> copyWithProgress(input, output, onBytesCopied) }
        }
        commit(partial, target, asset.sizeBytes)
        return target
    }

    /** Копирует поток блоками по мегабайту, сообщает прирост и прерывается при отмене корутины. */
    private suspend fun copyWithProgress(input: InputStream, output: OutputStream, onBytesCopied: (Long) -> Unit) {
        val buffer = ByteArray(COPY_BUFFER_BYTES)
        var read = input.read(buffer)
        while (read >= 0) {
            currentCoroutineContext().ensureActive()
            output.write(buffer, 0, read)
            onBytesCopied(read.toLong())
            read = input.read(buffer)
        }
    }

    /** Переименовывает докачанную копию в целевой файл, если она целая. Неполная копия удаляется. */
    private fun commit(partial: File, target: File, expectedBytes: Long) {
        val error = when {
            partial.length() != expectedBytes ->
                "Копия ${target.name} повреждена: ${partial.length()} из $expectedBytes байт"
            !partial.renameTo(target) -> "Не удалось сохранить ${target.path}"
            else -> return
        }
        partial.delete()
        throw IOException(error)
    }

    private companion object {
        const val MODELS_DIR = "models"
        const val COPY_BUFFER_BYTES = 1 shl 20
    }
}
