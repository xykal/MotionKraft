package xyz.xyverse.motionkraft.effects

import xyz.xyverse.motionkraft.data.model.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Menerapkan preset jedag jedug ke clip: mengenerate keyframe otomatis
 * berdasarkan beat/BPM yang diinginkan.
 *
 * Semua animasi ditulis sebagai keyframe agar bisa diedit kembali di curve editor,
 * sama seperti Alight Motion / After Effects.
 */
object JedagJedugApplier {

    private data class KeyBuilder(
        val scaleX: MutableList<Keyframe> = mutableListOf(),
        val scaleY: MutableList<Keyframe> = mutableListOf(),
        val posX: MutableList<Keyframe> = mutableListOf(),
        val posY: MutableList<Keyframe> = mutableListOf(),
        val rot: MutableList<Keyframe> = mutableListOf(),
        val op: MutableList<Keyframe> = mutableListOf()
    )

    fun applyPreset(clip: VideoClip, presetId: String, bpm: Float = 120f, intensity: Float = 0.5f) {
        val b = KeyBuilder()
        when (presetId) {
            JedagJedugPresets.ZOOM_PUNCH.first -> buildZoomPunch(b, clip.durationUs, bpm, intensity)
            JedagJedugPresets.HEAVY_SHAKE.first -> buildHeavyShake(b, clip.durationUs, bpm, intensity)
            JedagJedugPresets.STROBE_FLASH.first -> buildStrobe(b, clip.durationUs, bpm, intensity)
            JedagJedugPresets.WHIP_PAN.first -> buildWhipPan(b, clip.durationUs)
            JedagJedugPresets.SPIN_360.first -> buildSpin(b, clip.durationUs, intensity)
            JedagJedugPresets.PULSE.first -> buildPulse(b, clip.durationUs, bpm, intensity)
            JedagJedugPresets.EARTHQUAKE.first -> buildQuake(b, clip.durationUs, bpm, intensity)
            JedagJedugPresets.GLITCH_RGB.first -> {
                clip.effects.add(
                    EffectInstance(
                        effectId = JedagJedugPresets.GLITCH_RGB.first,
                        params = mutableMapOf("intensity" to intensity * 30f)
                    )
                )
            }
        }
        applyKeysToClip(clip, b)
    }

    private fun beatIntervalUs(bpm: Float): Long = (60_000_000f / bpm).toLong()

    private fun applyKeysToClip(clip: VideoClip, b: KeyBuilder) {
        // Ganti keyframe transform dengan yang baru dibangun.
        // Karena keyframe bersifat aditif, kita reset property yang dibangun preset,
        // dan mempertahankan property lain yang sudah ada.
        clip.transform = clip.transform.copy(
            scaleX = if (b.scaleX.isNotEmpty()) b.scaleX.toList() else clip.transform.scaleX,
            scaleY = if (b.scaleY.isNotEmpty()) b.scaleY.toList() else clip.transform.scaleY,
            positionX = if (b.posX.isNotEmpty()) b.posX.toList() else clip.transform.positionX,
            positionY = if (b.posY.isNotEmpty()) b.posY.toList() else clip.transform.positionY,
            rotation = if (b.rot.isNotEmpty()) b.rot.toList() else clip.transform.rotation,
            opacity = if (b.op.isNotEmpty()) b.op.toList() else clip.transform.opacity
        )
    }

    // ========== PRESET BUILDERS ==========

    private fun buildZoomPunch(b: KeyBuilder, dur: Long, bpm: Float, intensity: Float) {
        val interval = beatIntervalUs(bpm)
        val zoomPeak = 1f + 0.15f + intensity * 0.4f
        var t = 0L
        while (t < dur) {
            b.scaleX.add(Keyframe(t, 1f, Interpolation.LINEAR))
            b.scaleY.add(Keyframe(t, 1f, Interpolation.LINEAR))
            val punchTime = (t + interval / 6).coerceAtMost(dur)
            b.scaleX.add(Keyframe(punchTime, zoomPeak, Interpolation.EASE_OUT))
            b.scaleY.add(Keyframe(punchTime, zoomPeak, Interpolation.EASE_OUT))
            val settleTime = (t + interval / 2).coerceAtMost(dur)
            b.scaleX.add(Keyframe(settleTime, 1f, Interpolation.EASE_IN_OUT))
            b.scaleY.add(Keyframe(settleTime, 1f, Interpolation.EASE_IN_OUT))
            t += interval
        }
        b.scaleX.add(Keyframe(dur, 1f))
        b.scaleY.add(Keyframe(dur, 1f))
    }

    private fun buildHeavyShake(b: KeyBuilder, dur: Long, bpm: Float, intensity: Float) {
        val stepsPerBeat = 8
        val interval = beatIntervalUs(bpm) / stepsPerBeat
        val shake = 20f + intensity * 60f
        val rotAmt = 2f + intensity * 6f
        var t = 0L
        while (t < dur) {
            b.posX.add(Keyframe(t, Random.nextFloat() * shake * 2 - shake))
            b.posY.add(Keyframe(t, Random.nextFloat() * shake * 2 - shake))
            b.rot.add(Keyframe(t, Random.nextFloat() * rotAmt * 2 - rotAmt))
            t += interval
        }
        b.posX.add(Keyframe(dur, 0f))
        b.posY.add(Keyframe(dur, 0f))
        b.rot.add(Keyframe(dur, 0f))
    }

    private fun buildStrobe(b: KeyBuilder, dur: Long, bpm: Float, intensity: Float) {
        val interval = beatIntervalUs(bpm) / 2
        val dark = 1f - intensity * 0.8f
        var t = 0L
        var bright = true
        while (t < dur) {
            b.op.add(Keyframe(t, if (bright) 1f else dark, Interpolation.LINEAR))
            bright = !bright
            t += interval
        }
        b.op.add(Keyframe(dur, 1f))
    }

    private fun buildWhipPan(b: KeyBuilder, dur: Long) {
        val whipDur = minOf(400_000L, dur / 3)
        // Mulai dari luar layar kiri, masuk ke tengah dengan ease out
        b.posX.add(Keyframe(0L, -1500f, Interpolation.EASE_OUT))
        b.posY.add(Keyframe(0L, 0f))
        b.posX.add(Keyframe(whipDur, 0f, Interpolation.EASE_OUT))
        b.posY.add(Keyframe(whipDur, 0f))
        b.scaleX.add(Keyframe(0L, 1.5f))
        b.scaleY.add(Keyframe(0L, 1.5f))
        b.scaleX.add(Keyframe(whipDur, 1f, Interpolation.EASE_OUT))
        b.scaleY.add(Keyframe(whipDur, 1f, Interpolation.EASE_OUT))
        b.rot.add(Keyframe(0L, -15f, Interpolation.EASE_OUT))
        b.rot.add(Keyframe(whipDur, 0f, Interpolation.EASE_OUT))
    }

    private fun buildSpin(b: KeyBuilder, dur: Long, intensity: Float) {
        val speed = 0.5f + intensity * 2f // rotasi per detik
        val totalRot = (dur / 1_000_000f) * 360f * speed
        b.rot.add(Keyframe(0L, 0f, Interpolation.LINEAR))
        b.rot.add(Keyframe(dur, totalRot % 360, Interpolation.LINEAR))
        // scale boost
        val sc = 1f + intensity * 0.3f
        b.scaleX.add(Keyframe(0L, sc))
        b.scaleY.add(Keyframe(0L, sc))
        b.scaleX.add(Keyframe(dur, 1f, Interpolation.LINEAR))
        b.scaleY.add(Keyframe(dur, 1f, Interpolation.LINEAR))
    }

    private fun buildPulse(b: KeyBuilder, dur: Long, bpm: Float, intensity: Float) {
        val interval = beatIntervalUs(bpm)
        val peak = 1f + intensity * 0.2f
        var t = 0L
        while (t < dur) {
            b.scaleX.add(Keyframe(t, 1f, Interpolation.LINEAR))
            b.scaleY.add(Keyframe(t, 1f, Interpolation.LINEAR))
            val mid = (t + interval / 2).coerceAtMost(dur)
            b.scaleX.add(Keyframe(mid, peak, Interpolation.EASE_IN_OUT))
            b.scaleY.add(Keyframe(mid, peak, Interpolation.EASE_IN_OUT))
            t += interval
        }
        b.scaleX.add(Keyframe(dur, 1f))
        b.scaleY.add(Keyframe(dur, 1f))
    }

    private fun buildQuake(b: KeyBuilder, dur: Long, bpm: Float, intensity: Float) {
        // Quake = shake + sesekali zoom punch di beat
        buildHeavyShake(b, dur, bpm, intensity * 0.6f)
        val interval = beatIntervalUs(bpm)
        val zoomPeak = 1f + intensity * 0.2f
        var t = 0L
        while (t < dur) {
            b.scaleX.add(Keyframe(t, 1f, Interpolation.LINEAR))
            b.scaleY.add(Keyframe(t, 1f, Interpolation.LINEAR))
            val punch = (t + interval/6).coerceAtMost(dur)
            b.scaleX.add(Keyframe(punch, zoomPeak, Interpolation.EASE_OUT))
            b.scaleY.add(Keyframe(punch, zoomPeak, Interpolation.EASE_OUT))
            val settle = (t + interval/2).coerceAtMost(dur)
            b.scaleX.add(Keyframe(settle, 1f, Interpolation.EASE_IN_OUT))
            b.scaleY.add(Keyframe(settle, 1f, Interpolation.EASE_IN_OUT))
            t += interval
        }
    }
}
