package xyz.xyverse.motionkraft.data.model

import kotlin.math.PI
import kotlin.math.sin

/**
 * 2D transform of a clip. Every property is a list of keyframes whose times are
 * measured from the start of the clip on the timeline. One keyframe means a constant.
 * Position: canvas pixels from the centre. Scale: factor (1 = 100%). Rotation: degrees,
 * clockwise. Opacity: 0..1.
 */
data class Transform(
    val positionX: List<Keyframe> = Transform.constant(0f),
    val positionY: List<Keyframe> = Transform.constant(0f),
    val scaleX: List<Keyframe> = Transform.constant(1f),
    val scaleY: List<Keyframe> = Transform.constant(1f),
    val rotation: List<Keyframe> = Transform.constant(0f),
    val opacity: List<Keyframe> = Transform.constant(1f)
) {
    fun keys(property: TransformProperty): List<Keyframe> = when (property) {
        TransformProperty.POSITION_X -> positionX
        TransformProperty.POSITION_Y -> positionY
        TransformProperty.SCALE_X -> scaleX
        TransformProperty.SCALE_Y -> scaleY
        TransformProperty.ROTATION -> rotation
        TransformProperty.OPACITY -> opacity
    }

    fun valueAt(property: TransformProperty, timeUs: Long): Float = sample(keys(property), timeUs)

    /** Copy where [property] is a single constant value (removes its animation). */
    fun withConstant(property: TransformProperty, value: Float): Transform {
        val k = constant(value)
        return when (property) {
            TransformProperty.POSITION_X -> copy(positionX = k)
            TransformProperty.POSITION_Y -> copy(positionY = k)
            TransformProperty.SCALE_X -> copy(scaleX = k)
            TransformProperty.SCALE_Y -> copy(scaleY = k)
            TransformProperty.ROTATION -> copy(rotation = k)
            TransformProperty.OPACITY -> copy(opacity = k)
        }
    }

    companion object {
        fun constant(value: Float): List<Keyframe> = listOf(Keyframe(0L, value))

        /** Value of an animated property at [timeUs]. Holds the ends, interpolates between keys. */
        fun sample(keys: List<Keyframe>, timeUs: Long): Float {
            if (keys.isEmpty()) return 0f
            val s = keys.sortedBy { it.timeUs }
            if (timeUs <= s.first().timeUs) return s.first().value
            if (timeUs >= s.last().timeUs) return s.last().value
            for (i in 0 until s.size - 1) {
                val a = s[i]
                val b = s[i + 1]
                if (timeUs < a.timeUs || timeUs >= b.timeUs) continue
                if (a.interpolation == Interpolation.HOLD) return a.value
                val span = (b.timeUs - a.timeUs).coerceAtLeast(1L)
                val t = (timeUs - a.timeUs).toFloat() / span
                return a.value + (b.value - a.value) * ease(a.interpolation, t)
            }
            return s.last().value
        }

        /** Easing curve: maps progress t in 0..1 to eased progress. */
        fun ease(type: Interpolation, t: Float): Float = when (type) {
            Interpolation.LINEAR, Interpolation.HOLD -> t
            Interpolation.EASE_IN -> t * t * t
            Interpolation.EASE_OUT -> 1f - (1f - t) * (1f - t) * (1f - t)
            Interpolation.EASE_IN_OUT ->
                if (t < 0.5f) 4f * t * t * t else 1f - (2f - 2f * t) * (2f - 2f * t) * (2f - 2f * t) / 2f
            Interpolation.BOUNCE -> bounceOut(t)
            Interpolation.ELASTIC -> elasticOut(t)
        }

        private fun bounceOut(t: Float): Float {
            val n1 = 7.5625f
            val d1 = 2.75f
            return when {
                t < 1f / d1 -> n1 * t * t
                t < 2f / d1 -> { val x = t - 1.5f / d1; n1 * x * x + 0.75f }
                t < 2.5f / d1 -> { val x = t - 2.25f / d1; n1 * x * x + 0.9375f }
                else -> { val x = t - 2.625f / d1; n1 * x * x + 0.984375f }
            }
        }

        private fun elasticOut(t: Float): Float {
            if (t <= 0f) return 0f
            if (t >= 1f) return 1f
            val decay = Math.pow(2.0, (-10.0 * t)).toFloat()
            return decay * sin((t * 10f - 0.75f) * (2f * PI.toFloat() / 3f)) + 1f
        }
    }
}

enum class TransformProperty {
    POSITION_X, POSITION_Y, SCALE_X, SCALE_Y, ROTATION, OPACITY
}
