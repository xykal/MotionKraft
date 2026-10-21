package xyz.xyverse.motionkraft.data.model

import java.util.UUID

/**
 * One clip on the timeline. Times are microseconds. [startTimeUs] and [lengthUs] are
 * timeline values; [sourceStartUs] is where playback starts inside the source file.
 * The source covers [lengthUs] * speed of material.
 */
data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val type: ClipType,
    val filePath: String? = null,
    val hasAudio: Boolean = false,
    val startTimeUs: Long,
    val lengthUs: Long,
    val sourceStartUs: Long = 0L,
    val track: Int = 0,
    val speed: Float = 1f,
    val volume: Float = 1f,
    val transform: Transform = Transform(),
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val effects: List<EffectInstance> = emptyList(),
    val text: String = "",
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val strokeColor: Int = 0xFF000000.toInt(),
    val strokeWidth: Float = 0f,
    val fontSize: Float = 72f
) {
    val endTimeUs: Long get() = startTimeUs + lengthUs
}

enum class ClipType { VIDEO, IMAGE, TEXT }
