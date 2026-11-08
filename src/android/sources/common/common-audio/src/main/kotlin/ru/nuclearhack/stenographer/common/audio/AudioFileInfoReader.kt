package ru.nuclearhack.stenographer.common.audio

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AudioFileInfo(
    val displayName: String?,
    val durationMs: Long?,
)

@Inject
class AudioFileInfoReader(private val context: Context) {

    suspend fun read(uri: Uri): AudioFileInfo = withContext(Dispatchers.IO) {
        AudioFileInfo(displayName = displayName(uri), durationMs = durationMs(uri))
    }

    private fun displayName(uri: Uri): String? {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return uri.lastPathSegment
        return runCatching {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()
    }

    private fun durationMs(uri: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        // Длительность — только для подписи в UI: если файл её не отдаёт, показываем без неё.
        return try {
            runCatching {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            }.getOrNull()
        } finally {
            retriever.release()
        }
    }
}
