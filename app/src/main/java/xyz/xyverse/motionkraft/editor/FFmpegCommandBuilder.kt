package xyz.xyverse.motionkraft.editor

import xyz.xyverse.motionkraft.data.model.*
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Turns a project into one FFmpeg argument list. Each clip becomes a layer that is
 * trimmed and timed on the timeline, styled, transformed, then overlaid on a black
 * canvas in track order. Audio from video clips is delayed, speed-adjusted and mixed.
 *
 * Keyframes are exported as piecewise expressions with the same timing as the preview.
 * Bounce and elastic easings are exported as linear.
 */
object FFmpegCommandBuilder {

    const val DEFAULT_FONT = "/system/fonts/Roboto-Regular.ttf"

    /**
     * [textFileFor] writes a text clip's text to a file and returns its path. drawtext
     * reads that file, so the text never needs escaping inside the filter graph.
     */
    fun build(
        project: MographProject,
        outputPath: String,
        textFileFor: (VideoClip) -> String,
        fontPath: String = DEFAULT_FONT
    ): List<String> {
        val ex = project.exportSettings
        val fps = ex.fps
        val total = sec(project.timeline.durationUs)
        val clips = project.timeline.clips
            .filter { it.lengthUs > 0 && (it.type == ClipType.TEXT || it.filePath != null) }
            .sortedWith(compareBy<VideoClip>({ it.track }, { it.startTimeUs }))

        val inputArgs = ArrayList<String>()
        val inputIndex = HashMap<String, Int>()
        var mediaCount = 0
        for (c in clips) {
            if (c.type == ClipType.TEXT) continue
            inputIndex[c.id] = mediaCount++
            if (c.type == ClipType.IMAGE) {
                inputArgs += listOf("-loop", "1", "-framerate", "$fps", "-t", f(sec(c.lengthUs)))
            }
            inputArgs += listOf("-i", c.filePath.orEmpty())
        }

        val graph = ArrayList<String>()
        graph += "color=c=black:s=${ex.width}x${ex.height}:r=$fps:d=${f(total)},format=yuv420p[base]"

        val layers = ArrayList<Pair<VideoClip, String>>()
        val audioLabels = ArrayList<String>()
        clips.forEachIndexed { idx, c ->
            val label = "layer$idx"
            val chain = when (c.type) {
                ClipType.TEXT -> textChain(c, ex.width, ex.height, fps, textFileFor(c), fontPath)
                ClipType.VIDEO -> videoChain(c, inputIndex.getValue(c.id), ex.width, ex.height)
                ClipType.IMAGE -> imageChain(c, inputIndex.getValue(c.id), ex.width, ex.height)
            }
            graph += "$chain[$label]"
            layers += c to label
            if (c.type == ClipType.VIDEO && c.hasAudio) {
                val alabel = "audio$idx"
                graph += audioChain(c, inputIndex.getValue(c.id), alabel)
                audioLabels += alabel
            }
        }

        var prev = "base"
        layers.forEachIndexed { i, (clip, label) ->
            val out = "ov$i"
            val s = sec(clip.startTimeUs)
            val e = sec(clip.endTimeUs)
            val x = "(main_w-overlay_w)/2+(${keyExpr(clip.transform.positionX, s)})"
            val y = "(main_h-overlay_h)/2+(${keyExpr(clip.transform.positionY, s)})"
            graph += "[$prev][$label]overlay=x='$x':y='$y':eof_action=pass:" +
                "enable='between(t,${f(s)},${f(e)})'[$out]"
            prev = out
        }
        graph += "[$prev]format=yuv420p[vout]"

        graph += "anullsrc=channel_layout=stereo:sample_rate=44100:d=${f(total)}[silence]"
        if (audioLabels.isEmpty()) {
            graph += "[silence]anull[aout]"
        } else {
            val mixIns = "[silence]" + audioLabels.joinToString("") { "[$it]" }
            graph += "${mixIns}amix=inputs=${audioLabels.size + 1}:duration=first:" +
                "dropout_transition=0:normalize=0[aout]"
        }

        return listOf("-hide_banner", "-y") + inputArgs + listOf(
            "-filter_complex", graph.joinToString(";"),
            "-map", "[vout]", "-map", "[aout]",
            "-c:v", "libx264", "-preset", ex.preset, "-b:v", "${ex.bitrate}",
            "-r", "$fps", "-pix_fmt", "yuv420p",
            "-c:a", "aac", "-b:a", "192k",
            "-t", f(total), "-movflags", "+faststart",
            outputPath
        )
    }

    private fun videoChain(c: VideoClip, input: Int, w: Int, h: Int): String {
        val speed = speedOf(c)
        val head = "[$input:v]trim=start=${f(sec(c.sourceStartUs))}:duration=${f(sec(c.lengthUs) * speed)}," +
            "setpts=(PTS-STARTPTS)/${f(speed)}+${f(sec(c.startTimeUs))}/TB"
        return (listOf(head) + styleFilters(c, w, h) + transformFilters(c)).joinToString(",")
    }

    private fun imageChain(c: VideoClip, input: Int, w: Int, h: Int): String {
        val head = "[$input:v]setpts=PTS-STARTPTS+${f(sec(c.startTimeUs))}/TB"
        return (listOf(head) + styleFilters(c, w, h) + transformFilters(c)).joinToString(",")
    }

    private fun textChain(c: VideoClip, w: Int, h: Int, fps: Int, textFile: String, font: String): String {
        val head = listOf(
            "color=c=black@0:s=${w}x${h}:r=$fps:d=${f(sec(c.lengthUs))}",
            "format=rgba",
            "drawtext=fontfile=${esc(font)}:textfile=${esc(textFile)}:expansion=none:fontcolor=${hex(c.textColor)}:" +
                "fontsize=${c.fontSize.roundToInt()}:x=(w-text_w)/2:y=(h-text_h)/2:" +
                "borderw=${c.strokeWidth.roundToInt()}:bordercolor=${hex(c.strokeColor)}",
            "setpts=PTS-STARTPTS+${f(sec(c.startTimeUs))}/TB"
        )
        return (head + transformFilters(c)).joinToString(",")
    }

    private fun audioChain(c: VideoClip, input: Int, label: String): String {
        val speed = speedOf(c)
        val delayMs = c.startTimeUs / 1000
        return "[$input:a]atrim=start=${f(sec(c.sourceStartUs))}:duration=${f(sec(c.lengthUs) * speed)}," +
            "asetpts=PTS-STARTPTS,atempo=${f(speed)},volume=${f(c.volume.coerceIn(0f, 2f))}," +
            "adelay=$delayMs:all=1[$label]"
    }

    private fun styleFilters(c: VideoClip, w: Int, h: Int): List<String> {
        val out = mutableListOf(
            "scale=$w:$h:force_original_aspect_ratio=decrease",
            "setsar=1",
            "eq=brightness=${f(c.brightness.coerceIn(-1f, 1f))}:" +
                "contrast=${f(c.contrast.coerceIn(0f, 2f))}:saturation=${f(c.saturation.coerceIn(0f, 3f))}"
        )
        c.effects.filter { it.enabled }.mapNotNullTo(out) { effectFilter(it) }
        out += "format=rgba"
        out += "pad=$w:$h:(ow-iw)/2:(oh-ih)/2:color=0x00000000"
        return out
    }

    private fun transformFilters(c: VideoClip): List<String> {
        val t = c.transform
        val s = sec(c.startTimeUs)
        val out = ArrayList<String>()
        if (isScaled(t.scaleX) || isScaled(t.scaleY)) {
            out += "scale=w='trunc(iw*(${keyExpr(t.scaleX, s)})/2)*2':" +
                "h='trunc(ih*(${keyExpr(t.scaleY, s)})/2)*2':eval=frame"
        }
        if (isRotated(t.rotation)) {
            out += "rotate=a='(${keyExpr(t.rotation, s)})*PI/180':ow=iw:oh=ih:c=0x00000000"
        }
        // Opacity is exported as a constant (the first keyframe); animated opacity is preview-only.
        val opacity = Transform.sample(t.opacity, 0L).coerceIn(0f, 1f)
        if (opacity < 0.999f) out += "colorchannelmixer=aa=${f(opacity)}"
        return out
    }

    private fun effectFilter(fx: EffectInstance): String? = when (fx.effectId) {
        EffectIds.VINTAGE -> {
            val s = Effects.value(fx, "strength").coerceIn(0f, 1f)
            "eq=saturation=${f(1f - 0.7f * s)}:contrast=1.1," +
                "colorbalance=rs=${f(0.1f * s)}:gs=${f(0.05f * s)}:bs=${f(-0.1f * s)}"
        }
        EffectIds.BLACK_WHITE -> "hue=s=0"
        EffectIds.CINEMATIC -> {
            val s = Effects.value(fx, "strength").coerceIn(0f, 1f)
            "colorbalance=rs=${f(-0.1f * s)}:gs=${f(0.03f * s)}:bs=${f(0.1f * s)}:" +
                "rh=${f(0.1f * s)}:bh=${f(-0.1f * s)}"
        }
        EffectIds.BLUR -> {
            val r = Effects.value(fx, "radius").coerceIn(0f, 40f)
            if (r > 0.01f) "gblur=sigma=${f(r)}" else null
        }
        EffectIds.GLITCH -> {
            val i = Effects.value(fx, "intensity").coerceIn(0f, 40f).roundToInt()
            if (i > 0) "chromashift=cbh=$i:crh=${-i}" else null
        }
        else -> null
    }

    /**
     * Piecewise expression of the keyframes over time t (timeline seconds). [startSec] is
     * the clip start, so keyframe times are clip-relative.
     */
    private fun keyExpr(keys: List<Keyframe>, startSec: Double): String {
        val k = keys.sortedBy { it.timeUs }.distinctBy { it.timeUs }
        if (k.isEmpty()) return "0"
        if (k.size == 1) return f(k[0].value)
        val tt = "(t-${f(startSec)})"
        val parts = ArrayList<String>()
        parts += "lt($tt,${f(sec(k[0].timeUs))})*${f(k[0].value)}"
        for (i in 0 until k.size - 1) {
            val a = k[i]
            val b = k[i + 1]
            val ta = sec(a.timeUs)
            val tb = sec(b.timeUs)
            val span = (tb - ta).coerceAtLeast(1e-6)
            val x = "(($tt-${f(ta)})/${f(span)})"
            val value = if (a.interpolation == Interpolation.HOLD) {
                f(a.value)
            } else {
                "(${f(a.value)}+${f(b.value - a.value)}*${ease(a.interpolation, x)})"
            }
            parts += "gte($tt,${f(ta)})*lt($tt,${f(tb)})*$value"
        }
        parts += "gte($tt,${f(sec(k.last().timeUs))})*${f(k.last().value)}"
        return parts.joinToString("+")
    }

    private fun ease(type: Interpolation, x: String): String = when (type) {
        Interpolation.EASE_IN -> "($x*$x*$x)"
        Interpolation.EASE_OUT -> "(1-(1-$x)*(1-$x)*(1-$x))"
        Interpolation.EASE_IN_OUT ->
            "(lt($x,0.5)*4*$x*$x*$x+gte($x,0.5)*(1-(2-2*$x)*(2-2*$x)*(2-2*$x)/2))"
        else -> x
    }

    private fun isScaled(keys: List<Keyframe>): Boolean = keys.size > 1 || keys.any { it.value != 1f }

    private fun isRotated(keys: List<Keyframe>): Boolean = keys.size > 1 || keys.any { it.value != 0f }

    private fun speedOf(c: VideoClip): Float = c.speed.coerceIn(0.5f, 2f)

    private fun sec(us: Long): Double = us / 1_000_000.0

    private fun f(v: Double): String = String.format(Locale.US, "%.6f", v)

    private fun f(v: Float): String = f(v.toDouble())

    private fun hex(argb: Int): String = String.format(Locale.US, "0x%06X", argb and 0xFFFFFF)

    /** Escapes a value for use inside a filter option. */
    private fun esc(v: String): String = buildString {
        for (ch in v) {
            if (ch in "\\:'[],;") append('\\')
            append(ch)
        }
    }
}
