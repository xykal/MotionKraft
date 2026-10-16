package xyz.xyverse.motionkraft.data.model

/**
 * Keyframe untuk animasi property pada waktu tertentu.
 * Mendukung interpolasi linear, ease in/out, hold, dan bounce.
 */
data class Keyframe(
    val timeUs: Long,
    val value: Float,
    val interpolation: Interpolation = Interpolation.LINEAR
)

enum class Interpolation {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    HOLD,
    BOUNCE,
    ELASTIC
}
