package xyz.xyverse.motionkraft.editor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID

/**
 * Copies a picked file into app storage so projects keep working if the original
 * is moved or deleted, then reads its duration and audio track.
 */
object MediaImporter {

    data class Imported(val path: String, val durationUs: Long, val hasAudio: Boolean, val isVideo: Boolean)

    private const val PHOTO_DURATION_US = 3_000_000L

    fun import(context: Context, uri: Uri, video: Boolean): Imported? {
        val dir = File(context.filesDir, "media").apply { mkdirs() }
        val mime = context.contentResolver.getType(uri)
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?: if (video) "mp4" else "jpg"
        val dest = File(dir, "${UUID.randomUUID()}.$ext")
        val copied = context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
            true
        } ?: false
        if (!copied) return null
        if (!video) return Imported(dest.absolutePath, PHOTO_DURATION_US, hasAudio = false, isVideo = false)

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(dest.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
            if (durationMs <= 0L) {
                dest.delete()
                null
            } else {
                Imported(dest.absolutePath, durationMs * 1000L, hasAudio, isVideo = true)
            }
        } catch (e: Exception) {
            dest.delete()
            null
        } finally {
            retriever.release()
        }
    }
}
