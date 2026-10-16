package xyz.xyverse.motionkraft.data.model

/**
 * Timeline proyek: berisi semua klip, audio, dan metadata.
 */
data class Timeline(
    var durationUs: Long = 10_000_000L, // 10 detik default
    var fps: Int = 30,
    var width: Int = 1080,
    var height: Int = 1920, // Portrait 9:16 default untuk reels/TikTok
    var backgroundColor: Int = 0xFF000000.toInt(),
    val clips: MutableList<VideoClip> = mutableListOf()
) {
    fun getClipsAt(timeUs: Long): List<VideoClip> =
        clips.filter { timeUs in it.startTimeUs until it.endTimeUs }.sortedBy { it.track }

    fun getClipById(id: String): VideoClip? = clips.find { it.id == id }

    fun getTotalDuration(): Long {
        if (clips.isEmpty()) return durationUs
        return maxOf(durationUs, clips.maxOf { it.endTimeUs })
    }

    fun addClip(clip: VideoClip) {
        clips.add(clip)
    }

    fun removeClip(clipId: String) {
        clips.removeAll { it.id == clipId }
    }
}

/**
 * Proyek utuh yang bisa disave/load sebagai file json.
 */
data class MographProject(
    val version: Int = 1,
    var name: String = "Untitled Project",
    val createdAt: Long = System.currentTimeMillis(),
    var modifiedAt: Long = System.currentTimeMillis(),
    val timeline: Timeline = Timeline(),
    val masterVolume: Float = 1.0f,
    var exportSettings: ExportSettings = ExportSettings()
)

data class ExportSettings(
    var width: Int = 1080,
    var height: Int = 1920,
    var fps: Int = 30,
    var bitrate: Long = 16_000_000, // 16 Mbps
    var videoCodec: String = "h264",
    var audioBitrate: Int = 192_000,
    var qualityPreset: String = "medium" // ultrafast, fast, medium, slow
)
