package xyz.xyverse.motionkraft.effects

import xyz.xyverse.motionkraft.data.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Builds keyframes for beat-synced presets. Times are relative to the clip start.
 * Pure data in, pure data out: the caller stores the result.
 */
object JedagJedugApplier {

    data class Result(val transform: Transform, val effects: List<EffectInstance>)

    fun build(presetId: String, durationUs: Long, bpm: Float, intensity: Float): Result {
        val dur = max(durationUs, 1_000L)
        val beat = (60_000_000f / bpm.coerceIn(40f, 220f)).toLong()
        val i = intensity.coerceIn(0f, 1f)
        val k = Keys()
        var effects: List<EffectInstance> = emptyList()
        when (presetId) {
            JjPresets.ZOOM_PUNCH -> punch(k, dur, beat, 1f + 0.15f + i * 0.4f)
            JjPresets.HEAVY_SHAKE -> shake(k, dur, beat, i)
            JjPresets.PULSE -> pulse(k, dur, beat, 1f + i * 0.2f)
            JjPresets.QUAKE -> {
                shake(k, dur, beat, i * 0.6f)
                punch(k, dur, beat, 1f + i * 0.2f)
            }
            JjPresets.WHIP_PAN -> whip(k, dur)
            JjPresets.SPIN -> spin(k, dur, i)
            JjPresets.GLITCH_RGB ->
                effects = listOf(EffectInstance(EffectIds.GLITCH, params = mapOf("intensity" to 40f * i)))
        }
        return Result(k.toTransform(), effects)
    }

    private class Keys {
        val sx = ArrayList<Keyframe>()
        val sy = ArrayList<Keyframe>()
        val px = ArrayList<Keyframe>()
        val py = ArrayList<Keyframe>()
        val rot = ArrayList<Keyframe>()

        fun toTransform() = Transform(
            positionX = if (px.isEmpty()) Transform.constant(0f) else px.toList(),
            positionY = if (py.isEmpty()) Transform.constant(0f) else py.toList(),
            scaleX = if (sx.isEmpty()) Transform.constant(1f) else sx.toList(),
            scaleY = if (sy.isEmpty()) Transform.constant(1f) else sy.toList(),
            rotation = if (rot.isEmpty()) Transform.constant(0f) else rot.toList()
        )
    }

    private fun punch(k: Keys, dur: Long, beat: Long, peak: Float) {
        val step = max(beat, 1L)
        var t = 0L
        while (t < dur) {
            val up = min(t + step / 6, dur)
            val down = min(t + step / 2, dur)
            for (list in listOf(k.sx, k.sy)) {
                list += Keyframe(t, 1f, Interpolation.LINEAR)
                list += Keyframe(up, peak, Interpolation.EASE_OUT)
                list += Keyframe(down, 1f, Interpolation.EASE_IN_OUT)
            }
            t += step
        }
        k.sx += Keyframe(dur, 1f)
        k.sy += Keyframe(dur, 1f)
    }

    private fun pulse(k: Keys, dur: Long, beat: Long, peak: Float) {
        val step = max(beat, 1L)
        var t = 0L
        while (t < dur) {
            val mid = min(t + step / 2, dur)
            for (list in listOf(k.sx, k.sy)) {
                list += Keyframe(t, 1f, Interpolation.LINEAR)
                list += Keyframe(mid, peak, Interpolation.EASE_IN_OUT)
            }
            t += step
        }
        k.sx += Keyframe(dur, 1f)
        k.sy += Keyframe(dur, 1f)
    }

    private fun shake(k: Keys, dur: Long, beat: Long, i: Float) {
        val step = max(beat / 8, 1L)
        val amp = 20f + i * 60f
        val rotAmp = 2f + i * 6f
        var t = 0L
        while (t < dur) {
            k.px += Keyframe(t, Random.nextFloat() * amp * 2 - amp)
            k.py += Keyframe(t, Random.nextFloat() * amp * 2 - amp)
            k.rot += Keyframe(t, Random.nextFloat() * rotAmp * 2 - rotAmp)
            t += step
        }
        k.px += Keyframe(dur, 0f)
        k.py += Keyframe(dur, 0f)
        k.rot += Keyframe(dur, 0f)
    }

    private fun whip(k: Keys, dur: Long) {
        val whipUs = min(400_000L, dur / 3).coerceAtLeast(1L)
        k.px += Keyframe(0L, -1500f, Interpolation.EASE_OUT)
        k.px += Keyframe(whipUs, 0f)
        k.rot += Keyframe(0L, -15f, Interpolation.EASE_OUT)
        k.rot += Keyframe(whipUs, 0f)
        k.sx += Keyframe(0L, 1.5f, Interpolation.EASE_OUT)
        k.sx += Keyframe(whipUs, 1f)
        k.sy += Keyframe(0L, 1.5f, Interpolation.EASE_OUT)
        k.sy += Keyframe(whipUs, 1f)
    }

    private fun spin(k: Keys, dur: Long, i: Float) {
        val turnsPerSec = 0.5f + i * 2f
        k.rot += Keyframe(0L, 0f)
        k.rot += Keyframe(dur, (dur / 1_000_000f) * 360f * turnsPerSec)
        val boost = 1f + i * 0.3f
        k.sx += Keyframe(0L, boost)
        k.sx += Keyframe(dur, 1f)
        k.sy += Keyframe(0L, boost)
        k.sy += Keyframe(dur, 1f)
    }
}
