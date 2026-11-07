package ru.nuclearhack.stenographer.core.data.export

import android.content.Context
import android.net.Uri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.nuclearhack.stenographer.core.domain.repository.TranscriptExporter
import ru.nuclearhack.stenographer.core.domain.repository.TranscriptFormat
import ru.nuclearhack.stenographer.core.model.DocumentRef
import ru.nuclearhack.stenographer.core.model.Transcript

/** Пишет экспорт в файл, который пользователь выбрал в системном диалоге «Сохранить как» (SAF). */
@Inject
@ContributesBinding(AppScope::class)
class TranscriptExporterImpl(
    private val context: Context,
) : TranscriptExporter {

    override suspend fun export(transcript: Transcript, format: TranscriptFormat, destination: DocumentRef) {
        val content = TranscriptFormatter.format(transcript, format)
        withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openOutputStream(Uri.parse(destination.value), "wt")
                ?: throw IOException("Не удалось открыть файл для записи: ${destination.value}")
            stream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        }
    }
}
