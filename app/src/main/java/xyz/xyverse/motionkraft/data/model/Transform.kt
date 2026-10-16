package xyz.xyverse.motionkraft.data.model

/**
 * Menyimpan data transformasi 2D untuk sebuah klip pada satu waktu.
 * Semua animasi (jedag jedug, zoom, geser) menggunakan keyframe di property ini.
 */
data class Transform(
    // Posisi (pixels relatif terhadap center)
    val positionX: List<Keyframe> = listOf(Keyframe(0L, 0f)),
    val positionY: List<Keyframe> = listOf(Keyframe(0L, 0f)),

    // Skala (1.0f = normal, 2.0f = 2x zoom)
    val scaleX: List<Keyframe> = listOf(Keyframe(0L, 1f)),
    val scaleY: List<Keyframe> = listOf(Keyframe(0L, 1f)),

    // Rotasi (derajat)
    val rotation: List<Keyframe> = listOf(Keyframe(0L, 0f)),

    // Opacity (0 = transparan, 1 = opaque)
    val opacity: List<Keyframe> = listOf(Keyframe(0L, 1f)),

    // Anchor point (normalized 0-1, default center)
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.5f
) {
    fun getValueAt(property: TransformProperty, timeUs: Long): Float {
        val keys = when (property) {
            TransformProperty.POSITION_X -> positionX
            TransformProperty.POSITION_Y -> positionY
            TransformProperty.SCALE_X -> scaleX
            TransformProperty.SCALE_Y -> scaleY
            TransformProperty.ROTATION -> rotation
            TransformProperty.OPACITY -> opacity
        }
        return interpolate(keys, timeUs)
    }

    private fun interpolate(keys: List<Keyframe>, timeUs: Long): Float {
        if (keys.isEmpty()) return 0f
        if (keys.size == 1) return keys[0].value
        val sorted = keys.sortedBy { it.timeUs }
        if (timeUs <= sorted.first().timeUs) return sorted.first().value
        if (timeUs >= sorted.last().timeUs) return sorted.last().value

        // Temukan dua keyframe yang mengapit timeUs
        for (i in 0 until sorted.size - 1) {
            val a = sorted[i]
            val b = sorted[i + 1]
            if (timeUs in a.timeUs..b.timeUs) {
                val t = (timeUs - a.timeUs).toFloat() / (b.timeUs - a.timeUs).toFloat()
                return interpolateValue(a.value, b.value, t, a.interpolation)
            }
        }
        return sorted.last().value
    }

    private fun interpolateValue(a: Float, b: Float, t: Float, type: Interpolation): Float {
        val eased = when (type) {
            Interpolation.LINEAR -> t
            Interpolation.EASE_IN -> t * t * t
            Interpolation.EASE_OUT -> 1f - (1f - t) * (1f - t) * (1f - t)
            Interpolation.EASE_IN_OUT -> {
                if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).let { it * it * it } / 2f
            }
            Interpolation.HOLD -> 0f
            Interpolation.BOUNCE -> {
                val n1 = 7.5625f; val d1 = 2.75f
                when {
                    t < 1f/d1 -> n1 * t * t
                    t < 2f/d1 -> n1 * (t - 1.5f/d1).let { it * it } + 0.75f
                    t < 2.5f/d1 -> n1 * (t - 2.25f/d1).let { it * it } + 0.9375f
                    else -> n1 * (t - 2.625f/d1).let { it * it } + 0.984375f
                }
            }
            Interpolation.ELASTIC -> {
                val p = 0.3f
                val s = p / 4f
                val t2 = t - 1f
                -(Math.pow(2.0, (10f * t2).toDouble()).toFloat()) *
                        Math.sin(((t2 - s) * (2f * Math.PI) / p).toDouble()).toFloat() + 1f
            }
        }
        return a + (b - a) * eased
    }
}

enum class TransformProperty {
    POSITION_X, POSITION_Y, SCALE_X, SCALE_Y, ROTATION, OPACITY
}
