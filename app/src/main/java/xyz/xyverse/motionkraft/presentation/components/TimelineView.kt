package xyz.xyverse.motionkraft.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.xyverse.motionkraft.R
import xyz.xyverse.motionkraft.data.model.ClipType
import xyz.xyverse.motionkraft.data.model.VideoClip
import xyz.xyverse.motionkraft.ui.theme.ClipPalette

private val TrackHeight: Dp = 48.dp
private val RulerHeight: Dp = 22.dp

/**
 * Multi-track timeline. One row per track, a ruler for seeking, and a playhead.
 * Tap a clip to select it, drag it to move it in time, tap or drag the ruler to scrub.
 */
@Composable
fun TimelineView(
    clips: List<VideoClip>,
    durationUs: Long,
    playheadUs: Long,
    selectedId: String?,
    pxPerSecond: Float,
    onSeek: (Long) -> Unit,
    onSelect: (String?) -> Unit,
    onMove: (String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val pxPerSecPx = with(density) { pxPerSecond.dp.toPx() }
    val pxPerSec: Dp = pxPerSecond.dp
    val tracks = (clips.maxOfOrNull { it.track } ?: 0) + 1
    val totalWidth: Dp = pxPerSec * ((durationUs / 1_000_000f) + 3f)
    val tickColor = MaterialTheme.colorScheme.outline

    fun usOf(x: Float): Long = (x / pxPerSecPx * 1_000_000f).toLong()

    Box(modifier.horizontalScroll(rememberScrollState())) {
        Column(Modifier.width(totalWidth)) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(RulerHeight)
                    .pointerInput(pxPerSecPx) {
                        detectTapGestures { offset -> onSeek(usOf(offset.x)) }
                    }
                    .pointerInput(pxPerSecPx) {
                        detectDragGestures(
                            onDragStart = { offset -> onSeek(usOf(offset.x)) },
                            onDrag = { change, _ ->
                                change.consume()
                                onSeek(usOf(change.position.x))
                            }
                        )
                    }
            ) {
                val count = (size.width / pxPerSecPx).toInt() + 1
                for (sec in 0 until count) {
                    val x = sec * pxPerSecPx
                    drawLine(
                        color = tickColor,
                        start = Offset(x, if (sec % 5 == 0) 0f else size.height * 0.5f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                }
            }
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    repeat(tracks) { track ->
                        Box(Modifier.fillMaxWidth().height(TrackHeight)) {
                            clips.filter { it.track == track }.forEach { clip ->
                                ClipBlock(
                                    clip = clip,
                                    pxPerSec = pxPerSec,
                                    pxPerSecPx = pxPerSecPx,
                                    selected = clip.id == selectedId,
                                    onSelect = onSelect,
                                    onMove = onMove
                                )
                            }
                        }
                    }
                }
                Box(
                    Modifier
                        .offset(x = pxPerSec * (playheadUs / 1_000_000f))
                        .width(2.dp)
                        .height(TrackHeight * tracks)
                        .background(ClipPalette.Playhead)
                )
            }
        }
    }
}

@Composable
private fun ClipBlock(
    clip: VideoClip,
    pxPerSec: Dp,
    pxPerSecPx: Float,
    selected: Boolean,
    onSelect: (String?) -> Unit,
    onMove: (String, Long) -> Unit
) {
    val color = when (clip.type) {
        ClipType.VIDEO -> ClipPalette.Video
        ClipType.IMAGE -> ClipPalette.Image
        ClipType.TEXT -> ClipPalette.Text
    }
    val label = when (clip.type) {
        ClipType.TEXT -> clip.text
        ClipType.VIDEO -> stringResource(R.string.clip_video)
        ClipType.IMAGE -> stringResource(R.string.clip_photo)
    }
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .offset(x = pxPerSec * (clip.startTimeUs / 1_000_000f))
            .width(pxPerSec * (clip.lengthUs / 1_000_000f).coerceAtLeast(0.2f))
            .height(TrackHeight)
            .padding(vertical = 4.dp)
            .clip(shape)
            .background(color)
            .border(if (selected) 2.dp else 0.dp, if (selected) Color.White else Color.Transparent, shape)
            .pointerInput(clip.id) {
                detectTapGestures { onSelect(clip.id) }
            }
            .pointerInput(clip.id, pxPerSecPx) {
                detectDragGestures(
                    onDragStart = { onSelect(clip.id) },
                    onDrag = { change, drag ->
                        change.consume()
                        onMove(clip.id, (drag.x / pxPerSecPx * 1_000_000f).toLong())
                    }
                )
            }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
