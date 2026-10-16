package xyz.xyverse.motionkraft.editor

import xyz.xyverse.motionkraft.data.model.*

/**
 * Menghasilkan command FFmpeg untuk merender timeline menjadi video final.
 * Ini adalah jantung dari export engine.
 */
object FFmpegCommandBuilder {

    data class ExportCommand(
        val inputs: List<String>,
        val filterComplex: String,
        val outputArgs: List<String>
    )

    fun buildCommand(project: MographProject, outputPath: String): List<String> {
        val cmd = mutableListOf<String>()
        val timeline = project.timeline
        val totalDuration = timeline.getTotalDuration()

        // Tambahkan input files (unique)
        val uniqueSources = timeline.clips
            .filter { it.filePath != null }
            .map { it.filePath!! }
            .distinct()
        val sourceIndexMap = mutableMapOf<String, Int>()
        uniqueSources.forEachIndexed { idx, path ->
            cmd.add("-i")
            cmd.add(path)
            sourceIndexMap[path] = idx
        }

        // Input warna hitam sebagai background jika dibutuhkan
        cmd.add("-f")
        cmd.add("lavfi")
        cmd.add("-i")
        cmd.add("color=c=black:s=${timeline.width}x${timeline.height}:d=${totalDuration / 1_000_000f}:r=${timeline.fps}")
        val bgInputIndex = uniqueSources.size

        cmd.add("-f")
        cmd.add("lavfi")
        cmd.add("-i")
        cmd.add("anullsrc=channel_layout=stereo:sample_rate=44100")
        val nullAudioIndex = uniqueSources.size + 1

        // Build filter complex
        val filterParts = mutableListOf<String>()
        val layeredLabels = mutableListOf<String>()
        var currentLabel = "base"

        // Background
        filterParts.add("[$bgInputIndex:v]setsar=1,format=yuv420p[$currentLabel]")

        // Urutkan klip berdasarkan track
        val sortedClips = timeline.clips.sortedBy { it.track }

        sortedClips.forEach { clip ->
            val inputIdx = sourceIndexMap[clip.filePath]
            val labelIn = "c${clip.id}_in"
            val labelTrimmed = "c${clip.id}_trim"
            val labelScaled = "c${clip.id}_scaled"
            val labelDelayed = "c${clip.id}_delayed"
            val labelTransformed = "c${clip.id}_xf"
            val labelColored = "c${clip.id}_col"
            val labelEffected = "c${clip.id}_fx"
            val labelOut = "c${clip.id}_out"

            if (inputIdx != null) {
                filterParts.add("[$inputIdx:v]setsar=1[$labelIn]")
                // Trim klip
                val trimStart = clip.trimStartUs / 1_000_000f
                val clipDurSec = clip.durationUs / 1_000_000f / clip.speed
                filterParts.add(
                    "[$labelIn]trim=start=$trimStart:end=${trimStart + clipDurSec}," +
                    "setpts=PTS-STARTPTS," +
                    "scale=${timeline.width}:${timeline.height}:force_original_aspect_ratio=decrease," +
                    "pad=${timeline.width}:${timeline.height}:(ow-iw)/2:(oh-ih)/2:color=black@0," +
                    "setsar=1[$labelTrimmed]"
                )
            } else if (clip.type == ClipType.TEXT) {
                // Text source
                val safeText = clip.textContent.replace("'", "\\'").replace(":", "\\:")
                filterParts.add(
                    "color=c=black@0:s=${timeline.width}x${timeline.height}:d=${clip.durationUs / 1_000_000f}," +
                    "drawtext=text='$safeText':fontcolor=${colorToHex(clip.textColor)}:fontsize=${clip.fontSize}:" +
                    "x=(w-text_w)/2:y=(h-text_h)/2:borderw=${clip.strokeWidth}:bordercolor=${colorToHex(clip.strokeColor)}" +
                    "[$labelTrimmed]"
                )
            } else {
                // Empty transparent
                filterParts.add(
                    "color=c=black@0:s=${timeline.width}x${timeline.height}:d=${clip.durationUs / 1_000_000f}[$labelTrimmed]"
                )
            }

            // Speed
            if (clip.speed != 1.0f) {
                filterParts.add("[$labelTrimmed]setpts=${1f/clip.speed}*PTS[$labelScaled]")
            } else {
                filterParts.add("[$labelTrimmed]null[$labelScaled]")
            }

            // Delay untuk memulai di waktu yang benar
            val delayMs = clip.startTimeUs / 1000
            filterParts.add(
                "[$labelScaled]adelay=${delayMs}|${delayMs},apad[del_${clip.id}_a];" +
                "[$labelScaled]delay=${delayMs}ms:all=1[$labelDelayed]"
            )

            // Transform (zoom, posisi, rotasi) menggunakan FFmpeg zoompan + rotate + overlay
            // Untuk MVP kita gunakan pendekatan enable expression di overlay
            val opacityExpr = if (clip.transform.opacity.size > 1) {
                buildKeyframeExpr(clip.transform.opacity, clip.startTimeUs)
            } else "1"

            // Color grading
            filterParts.add(
                "[$labelDelayed]" +
                "eq=brightness=${clip.brightness}:contrast=${clip.contrast}:saturation=${clip.saturation}" +
                "[$labelColored]"
            )

            // Apply effects filter chain
            var lastLabel = labelColored
            clip.effects.filter { it.enabled }.forEach { fx ->
                val fxLabel = "c${clip.id}_${fx.effectId}"
                val fxFilter = buildEffectFilter(fx)
                if (fxFilter != null) {
                    filterParts.add("[$lastLabel]$fxFilter[$fxLabel]")
                    lastLabel = fxLabel
                }
            }
            filterParts.add("[$lastLabel]format=yuva420p[$labelEffected]")

            // Overlay ke timeline
            val nextLabel = if (clip == sortedClips.last()) "finalv" else "lay${clip.id}"
            filterParts.add(
                "[$currentLabel][$labelEffected]overlay=x=0:y=0:enable='between(t,${clip.startTimeUs/1_000_000f},${clip.endTimeUs/1_000_000f})'" +
                ":shortest=0[$nextLabel]"
            )
            currentLabel = nextLabel
        }

        // Jika tidak ada clip, gunakan background
        if (sortedClips.isEmpty()) {
            filterParts.add("[$currentLabel]null[finalv]")
        }

        val fc = filterParts.joinToString(";")
        cmd.add("-filter_complex")
        cmd.add(fc)

        // Mapping
        cmd.add("-map")
        cmd.add("[finalv]")
        cmd.add("-map")
        cmd.add("$nullAudioIndex:a")

        // Encoding settings
        val ex = project.exportSettings
        cmd.add("-c:v")
        cmd.add(ex.videoCodec)
        cmd.add("-preset")
        cmd.add(ex.qualityPreset)
        cmd.add("-b:v")
        cmd.add("${ex.bitrate}")
        cmd.add("-r")
        cmd.add("${ex.fps}")
        cmd.add("-s")
        cmd.add("${ex.width}x${ex.height}")
        cmd.add("-pix_fmt")
        cmd.add("yuv420p")
        cmd.add("-c:a")
        cmd.add("aac")
        cmd.add("-b:a")
        cmd.add("${ex.audioBitrate}")
        cmd.add("-shortest")
        cmd.add("-y")
        cmd.add(outputPath)

        return cmd
    }

    private fun buildEffectFilter(fx: EffectInstance): String? {
        return when (fx.effectId) {
            "blur_gaussian" -> {
                val r = fx.params["radius"] ?: 10f
                "gblur=sigma=$r"
            }
            "filter_blackwhite" -> "hue=s=0"
            "filter_vintage" -> {
                val s = fx.params["strength"] ?: 0.5f
                "eq=saturation=${1f - s*0.7f}:contrast=1.1,colorbalance=rs=$s*0.1:gs=$s*0.05:bs=-$s*0.1"
            }
            "filter_cinematic" -> "curves=r='0/0 0.5/0.4 1/1':g='0/0 0.5/0.45 1/0.9':b='0/0 0.5/0.55 1/1'"
            "glow_outer" -> "gblur=sigma=10,overlay=0:0"
            "jj_glitch" -> {
                val i = fx.params["intensity"] ?: 15f
                "rgbashift=rh=$i:gv=-$i:bw=$i"
            }
            "jj_zoom_blur" -> "zoompan=z='min(zoom+0.0015,1.5)':d=15:s=1080x1920"
            else -> null
        }
    }

    private fun buildKeyframeExpr(keys: List<Keyframe>, offsetUs: Long): String {
        // Build FFmpeg expression untuk evaluasi opacity per frame
        if (keys.size <= 1) return "${keys.firstOrNull()?.value ?: 1f}"
        val sorted = keys.sortedBy { it.timeUs }
        val parts = mutableListOf<String>()
        for (i in 0 until sorted.size - 1) {
            val a = sorted[i]
            val b = sorted[i + 1]
            val t1 = (a.timeUs + offsetUs) / 1_000_000f
            val t2 = (b.timeUs + offsetUs) / 1_000_000f
            parts.add("between(t,$t1,$t2)*(${a.value}+(t-$t1)*${(b.value-a.value)/(t2-t1)})")
        }
        return parts.joinToString("+")
    }

    private fun colorToHex(color: Int): String {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val a = (color shr 24) and 0xFF
        return String.format("0x%02X%02X%02X@%.2f", r, g, b, a / 255f)
    }
}
