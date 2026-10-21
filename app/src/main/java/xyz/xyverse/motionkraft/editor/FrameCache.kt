package xyz.xyverse.motionkraft.editor

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever

/**
 * Preview-only frame source. Keeps a few decoders open so scrubbing does not reopen
 * files. Frames are scaled down; the exporter always renders at full resolution.
 */
object FrameCache {

    private const val MAX_OPEN = 3
    private const val PREVIEW_WIDTH = 540

    private val open = LinkedHashMap<String, MediaMetadataRetriever>()

    @Synchronized
    fun frame(path: String, timeUs: Long): Bitmap? {
        val retriever = try {
            open.remove(path) ?: MediaMetadataRetriever().apply { setDataSource(path) }
        } catch (e: Exception) {
            return null
        }
        open[path] = retriever
        while (open.size > MAX_OPEN) {
            val oldest = open.keys.first()
            open.remove(oldest)?.release()
        }
        val frame = try {
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            null
        } ?: return null
        return if (frame.width <= PREVIEW_WIDTH) {
            frame
        } else {
            val h = frame.height * PREVIEW_WIDTH / frame.width
            Bitmap.createScaledBitmap(frame, PREVIEW_WIDTH, h, true)
        }
    }
}
