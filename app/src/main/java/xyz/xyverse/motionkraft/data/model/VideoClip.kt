package xyz.xyverse.motionkraft.data.model

import android.net.Uri

/**
 * Mewakili satu klip di timeline (video, gambar, atau text).
 */
data class VideoClip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ClipType,
    val sourceUri: Uri? = null,
    val filePath: String? = null,

    // Waktu dalam timeline (microseconds)
    var startTimeUs: Long,
    var endTimeUs: Long,
    val trimStartUs: Long = 0L,
    val trimEndUs: Long = 0L,

    // Layer track: 0 = paling bawah, makin besar makin di depan
    val track: Int = 0,

    // Transform & visual properties
    var transform: Transform = Transform(),
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val blendMode: BlendMode = BlendMode.NORMAL,

    // Effects dan filter yang terpasang
    val effects: MutableList<EffectInstance> = mutableListOf(),

    // Color grading
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val hue: Float = 0f,

    // Khusus teks
    val textContent: String = "",
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val fontSize: Float = 48f,
    val fontPath: String? = null,
    val strokeColor: Int = 0xFF000000.toInt(),
    val strokeWidth: Float = 0f
) {
    val durationUs: Long get() = endTimeUs - startTimeUs
}

enum class ClipType {
    VIDEO, IMAGE, TEXT, AUDIO, SHAPE
}

enum class BlendMode {
    NORMAL, MULTIPLY, SCREEN, OVERLAY, ADD, DARKEN, LIGHTEN, DIFFERENCE
}
