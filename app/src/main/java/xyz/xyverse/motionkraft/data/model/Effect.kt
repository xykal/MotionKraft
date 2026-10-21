package xyz.xyverse.motionkraft.data.model

object EffectIds {
    const val VINTAGE = "vintage"
    const val BLACK_WHITE = "black_white"
    const val CINEMATIC = "cinematic"
    const val BLUR = "blur"
    const val GLITCH = "glitch"
}

data class EffectParameter(val id: String, val min: Float, val max: Float, val default: Float)

data class EffectDefinition(val id: String, val parameters: List<EffectParameter> = emptyList())

/** An effect applied to a clip. Missing params fall back to the definition defaults. */
data class EffectInstance(
    val effectId: String,
    val enabled: Boolean = true,
    val params: Map<String, Float> = emptyMap()
)

object Effects {
    /** Effects the exporter can render. Keep in sync with FFmpegCommandBuilder. */
    val ALL: List<EffectDefinition> = listOf(
        EffectDefinition(EffectIds.VINTAGE, listOf(EffectParameter("strength", 0f, 1f, 0.5f))),
        EffectDefinition(EffectIds.BLACK_WHITE),
        EffectDefinition(EffectIds.CINEMATIC, listOf(EffectParameter("strength", 0f, 1f, 0.5f))),
        EffectDefinition(EffectIds.BLUR, listOf(EffectParameter("radius", 0f, 40f, 8f))),
        EffectDefinition(EffectIds.GLITCH, listOf(EffectParameter("intensity", 0f, 40f, 12f)))
    )

    fun definition(id: String): EffectDefinition? = ALL.firstOrNull { it.id == id }

    fun value(instance: EffectInstance, param: String): Float {
        instance.params[param]?.let { return it }
        return definition(instance.effectId)?.parameters?.firstOrNull { it.id == param }?.default ?: 0f
    }
}

/** Jedag-jedug presets. Each one generates keyframes on the clip (see JedagJedugApplier). */
object JjPresets {
    const val ZOOM_PUNCH = "zoom_punch"
    const val HEAVY_SHAKE = "heavy_shake"
    const val PULSE = "pulse"
    const val QUAKE = "quake"
    const val WHIP_PAN = "whip_pan"
    const val SPIN = "spin"
    const val GLITCH_RGB = "glitch_rgb"

    val ALL: List<String> = listOf(ZOOM_PUNCH, HEAVY_SHAKE, PULSE, QUAKE, WHIP_PAN, SPIN, GLITCH_RGB)
}
