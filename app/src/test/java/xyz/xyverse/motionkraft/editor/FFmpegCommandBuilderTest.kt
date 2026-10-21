package xyz.xyverse.motionkraft.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xyverse.motionkraft.data.model.ClipType
import xyz.xyverse.motionkraft.data.model.ExportSettings
import xyz.xyverse.motionkraft.data.model.Interpolation
import xyz.xyverse.motionkraft.data.model.Keyframe
import xyz.xyverse.motionkraft.data.model.MographProject
import xyz.xyverse.motionkraft.data.model.Timeline
import xyz.xyverse.motionkraft.data.model.Transform
import xyz.xyverse.motionkraft.data.model.VideoClip

class FFmpegCommandBuilderTest {

    private fun project(vararg clips: VideoClip) = MographProject(
        timeline = Timeline(clips.toList()),
        exportSettings = ExportSettings(width = 540, height = 960, fps = 30, bitrate = 4_000_000L, preset = "ultrafast")
    )

    private fun build(p: MographProject, textFile: String = "/tmp/text.txt"): List<String> =
        FFmpegCommandBuilder.build(p, "/tmp/out.mp4", textFileFor = { textFile }, fontPath = "/tmp/font.ttf")

    private fun graph(args: List<String>): String = args[args.indexOf("-filter_complex") + 1]

    @Test
    fun usesH264EncoderAndWritesOutputLast() {
        val args = build(project(VideoClip(type = ClipType.VIDEO, filePath = "/a.mp4", startTimeUs = 0L, lengthUs = 2_000_000L)))
        assertEquals("libx264", args[args.indexOf("-c:v") + 1])
        assertEquals("/tmp/out.mp4", args.last())
    }

    @Test
    fun eachMediaClipGetsOneInput() {
        val args = build(project(
            VideoClip(type = ClipType.VIDEO, filePath = "/a.mp4", startTimeUs = 0L, lengthUs = 2_000_000L),
            VideoClip(type = ClipType.VIDEO, filePath = "/b.mp4", startTimeUs = 2_000_000L, lengthUs = 2_000_000L),
            VideoClip(type = ClipType.IMAGE, filePath = "/c.png", startTimeUs = 4_000_000L, lengthUs = 2_000_000L),
            VideoClip(type = ClipType.TEXT, startTimeUs = 0L, lengthUs = 2_000_000L, text = "hi", track = 1)
        ))
        assertEquals(3, args.count { it == "-i" })
    }

    @Test
    fun textComesFromFileNotInline() {
        val args = build(project(VideoClip(type = ClipType.TEXT, startTimeUs = 0L, lengthUs = 1_000_000L, text = "Hi, 50%")),
            textFile = "/tmp/t1.txt")
        val g = graph(args)
        assertTrue(g.contains("textfile=/tmp/t1.txt"))
        assertTrue(g.contains("expansion=none"))
        assertFalse(g.contains("text='"))
    }

    @Test
    fun animatedPositionIsExportedAsExpression() {
        val moving = VideoClip(
            type = ClipType.TEXT, text = "MOVE", startTimeUs = 0L, lengthUs = 2_000_000L,
            transform = Transform(positionX = listOf(Keyframe(0L, -200f), Keyframe(2_000_000L, 200f, Interpolation.EASE_OUT)))
        )
        val g = graph(build(project(moving)))
        assertTrue(g.contains("overlay=x='(main_w-overlay_w)/2+("))
        assertTrue(g.contains("gte("))
    }

    @Test
    fun audioIsOnlyMixedForClipsThatHaveAudio() {
        val silent = build(project(VideoClip(type = ClipType.VIDEO, filePath = "/a.mp4", startTimeUs = 0L, lengthUs = 1_000_000L)))
        assertTrue(graph(silent).contains("[silence]anull[aout]"))
        val withAudio = build(project(VideoClip(type = ClipType.VIDEO, filePath = "/a.mp4", hasAudio = true,
            startTimeUs = 0L, lengthUs = 1_000_000L)))
        assertTrue(graph(withAudio).contains("amix=inputs=2"))
    }
}
