package xyz.xyverse.motionkraft.data.model

/**
 * Definisi efek/filter yang bisa diterapkan ke klip.
 */
data class EffectDefinition(
    val id: String,
    val name: String,
    val category: EffectCategory,
    val parameters: List<EffectParameter>,
    val previewColor: Int = 0xFF6366F1.toInt()
)

data class EffectInstance(
    val effectId: String,
    var enabled: Boolean = true,
    val params: MutableMap<String, Float> = mutableMapOf(),
    val startTimeUs: Long = 0L,
    val endTimeUs: Long = Long.MAX_VALUE
)

data class EffectParameter(
    val id: String,
    val name: String,
    val minValue: Float,
    val maxValue: Float,
    val defaultValue: Float
)

enum class EffectCategory {
    TRANSITION, COLOR, DISTORT, BLUR, GLOW, STYLIZE, JJEDAG_JEDUG, AUDIO
}

/**
 * Library preset efek jedag jedug populer.
 * Apply directly to a clip in one tap.
 */
object JedagJedugPresets {

    // Zoom punch: cepat zoom in lalu sedikit zoom out (ketukan beat)
    val ZOOM_PUNCH = "jj_zoom_punch" to "Zoom Punch"

    // Shake: getar kencang (beat drop)
    val HEAVY_SHAKE = "jj_heavy_shake" to "Heavy Shake"

    // Flash: layar putih berkedip cepat
    val STROBE_FLASH = "jj_strobe" to "Strobe Flash"

    // Glitch RGB split
    val GLITCH_RGB = "jj_glitch" to "Glitch RGB"

    // Whip pan: geser cepat
    val WHIP_PAN = "jj_whip" to "Whip Pan"

    // Rotate spin 360
    val SPIN_360 = "jj_spin" to "Spin 360"

    // Scale pulse
    val PULSE = "jj_pulse" to "Pulse Beat"

    // Earthquake
    val EARTHQUAKE = "jj_quake" to "Earthquake"

    // Zoom blur transisi
    val ZOOM_BLUR = "jj_zoom_blur" to "Zoom Blur"

    val ALL_EFFECTS: List<EffectDefinition> = listOf(
        EffectDefinition(
            id = ZOOM_PUNCH.first,
            name = ZOOM_PUNCH.second,
            category = EffectCategory.JJEDAG_JEDUG,
            parameters = listOf(
                EffectParameter("intensity", "Intensitas", 0.1f, 1.0f, 0.5f),
                EffectParameter("speed", "Kecepatan", 0.1f, 1.0f, 0.5f)
            )
        ),
        EffectDefinition(
            id = HEAVY_SHAKE.first,
            name = HEAVY_SHAKE.second,
            category = EffectCategory.JJEDAG_JEDUG,
            parameters = listOf(
                EffectParameter("intensity", "Intensitas", 5f, 50f, 25f),
                EffectParameter("frequency", "Frekuensi", 1f, 30f, 15f)
            )
        ),
        EffectDefinition(
            id = STROBE_FLASH.first,
            name = STROBE_FLASH.second,
            category = EffectCategory.JJEDAG_JEDUG,
            parameters = listOf(
                EffectParameter("frequency", "Frekuensi", 2f, 30f, 10f),
                EffectParameter("opacity", "Opacity", 0.2f, 1.0f, 0.8f)
            )
        ),
        EffectDefinition(
            id = GLITCH_RGB.first,
            name = GLITCH_RGB.second,
            category = EffectCategory.JJEDAG_JEDUG,
            parameters = listOf(
                EffectParameter("intensity", "Intensitas", 5f, 40f, 15f)
            )
        ),
        EffectDefinition(
            id = WHIP_PAN.first,
            name = WHIP_PAN.second,
            category = EffectCategory.TRANSITION,
            parameters = listOf(
                EffectParameter("direction", "Arah (0=kanan,1=kiri,2=atas,3=bawah)", 0f, 3f, 0f)
            )
        ),
        EffectDefinition(
            id = SPIN_360.first,
            name = SPIN_360.second,
            category = EffectCategory.JJEDAG_JEDUG,
            parameters = listOf(
                EffectParameter("speed", "Kecepatan", 0.2f, 3f, 1f),
                EffectParameter("scaleBoost", "Zoom boost", 1f, 1.5f, 1.2f)
            )
        ),
        EffectDefinition(
            id = PULSE.first,
            name = PULSE.second,
            category = EffectCategory.JJEDAG_JEDUG,
            parameters = listOf(
                EffectParameter("scaleAmount", "Besar zoom", 0.05f, 0.5f, 0.15f),
                EffectParameter("bpm", "BPM ketukan", 60f, 180f, 120f)
            )
        ),
        EffectDefinition(
            id = EARTHQUAKE.first,
            name = EARTHQUAKE.second,
            category = EffectCategory.JJEDAG_JEDUG,
            parameters = listOf(
                EffectParameter("intensity", "Intensitas", 2f, 30f, 12f),
                EffectParameter("rotation", "Rotasi", 0f, 5f, 2f)
            )
        ),
        EffectDefinition(
            id = ZOOM_BLUR.first,
            name = ZOOM_BLUR.second,
            category = EffectCategory.TRANSITION,
            parameters = listOf(
                EffectParameter("intensity", "Intensitas", 0f, 50f, 20f)
            )
        ),
        // Filter warna
        EffectDefinition(
            id = "filter_vintage", name = "Vintage", category = EffectCategory.COLOR,
            parameters = listOf(EffectParameter("strength", "Kekuatan", 0f, 1f, 0.5f))
        ),
        EffectDefinition(
            id = "filter_blackwhite", name = "Hitam Putih", category = EffectCategory.COLOR,
            parameters = listOf(EffectParameter("strength", "Kekuatan", 0f, 1f, 1f))
        ),
        EffectDefinition(
            id = "filter_cinematic", name = "Cinematic Teal-Orange", category = EffectCategory.COLOR,
            parameters = listOf(EffectParameter("strength", "Kekuatan", 0f, 1f, 0.5f))
        ),
        EffectDefinition(
            id = "blur_gaussian", name = "Gaussian Blur", category = EffectCategory.BLUR,
            parameters = listOf(EffectParameter("radius", "Radius", 0f, 50f, 10f))
        ),
        EffectDefinition(
            id = "glow_outer", name = "Outer Glow", category = EffectCategory.GLOW,
            parameters = listOf(
                EffectParameter("radius", "Radius", 0f, 30f, 10f),
                EffectParameter("intensity", "Intensitas", 0f, 1f, 0.6f)
            )
        )
    )

    fun getById(id: String): EffectDefinition? = ALL_EFFECTS.find { it.id == id }
}
