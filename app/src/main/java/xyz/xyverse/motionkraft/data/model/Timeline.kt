package xyz.xyverse.motionkraft.data.model

import java.util.UUID

/** Clips on tracks. Track 0 is the bottom layer; higher tracks draw on top. */
data class Timeline(val clips: List<VideoClip> = emptyList()) {

    /** Project length in microseconds. Never shorter than [MIN_DURATION_US]. */
    val durationUs: Long
        get() = maxOf(MIN_DURATION_US, clips.maxOfOrNull { it.endTimeUs } ?: 0L)

    fun clip(id: String): VideoClip? = clips.firstOrNull { it.id == id }

    fun endOfTrack(track: Int): Long =
        clips.filter { it.track == track }.maxOfOrNull { it.endTimeUs } ?: 0L

    fun add(clip: VideoClip): Timeline = copy(clips = clips + clip)

    fun replace(clip: VideoClip): Timeline =
        copy(clips = clips.map { if (it.id == clip.id) clip else it })

    fun remove(id: String): Timeline = copy(clips = clips.filterNot { it.id == id })

    companion object {
        const val MIN_DURATION_US = 5_000_000L
    }
}

data class ExportSettings(
    val width: Int = 1080,
    val height: Int = 1920,
    val fps: Int = 30,
    val bitrate: Long = 12_000_000L,
    val preset: String = "medium"
)

data class MographProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val timeline: Timeline = Timeline(),
    val exportSettings: ExportSettings = ExportSettings()
)
