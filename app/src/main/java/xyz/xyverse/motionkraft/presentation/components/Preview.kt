package xyz.xyverse.motionkraft.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.xyverse.motionkraft.data.model.ClipType
import xyz.xyverse.motionkraft.data.model.TransformProperty
import xyz.xyverse.motionkraft.data.model.VideoClip
import xyz.xyverse.motionkraft.editor.FrameCache
import java.io.File

/**
 * Shows every clip active at the playhead, stacked by track, with its transform applied.
 * The export uses the same timing and transform rules, so the preview matches the output.
 */
@Composable
fun PreviewPanel(
    canvasWidth: Int,
    canvasHeight: Int,
    playheadUs: Long,
    clips: List<VideoClip>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        val ratio = canvasWidth.toFloat() / canvasHeight.toFloat()
        val frameW: Dp = minOf(maxWidth, maxHeight * ratio)
        val frameH: Dp = frameW / ratio
        val framePx = with(density) { frameW.toPx() }
        val pxPerCanvasPx = framePx / canvasWidth
        Box(
            Modifier.size(frameW, frameH)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black)
        ) {
            clips.forEach { clip -> ClipLayer(clip, playheadUs, pxPerCanvasPx) }
        }
    }
}

@Composable
private fun ClipLayer(clip: VideoClip, playheadUs: Long, pxPerCanvasPx: Float) {
    val local = (playheadUs - clip.startTimeUs).coerceAtLeast(0L)
    val t = clip.transform
    val layer = Modifier.fillMaxSize().graphicsLayer {
        scaleX = t.valueAt(TransformProperty.SCALE_X, local)
        scaleY = t.valueAt(TransformProperty.SCALE_Y, local)
        rotationZ = t.valueAt(TransformProperty.ROTATION, local)
        alpha = t.valueAt(TransformProperty.OPACITY, local).coerceIn(0f, 1f)
        translationX = t.valueAt(TransformProperty.POSITION_X, local) * pxPerCanvasPx
        translationY = t.valueAt(TransformProperty.POSITION_Y, local) * pxPerCanvasPx
    }
    Box(layer, contentAlignment = Alignment.Center) {
        when (clip.type) {
            ClipType.IMAGE -> clip.filePath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            ClipType.VIDEO -> clip.filePath?.let { path -> VideoFrame(path, clip, local) }
            ClipType.TEXT -> Text(
                text = clip.text,
                color = Color(clip.textColor),
                fontSize = (clip.fontSize * pxPerCanvasPx).sp,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(color = Color(clip.strokeColor), blurRadius = clip.strokeWidth * pxPerCanvasPx)
                )
            )
        }
    }
}

@Composable
private fun VideoFrame(path: String, clip: VideoClip, localUs: Long) {
    val sourceUs = clip.sourceStartUs + (localUs * clip.speed).toLong()
    val bucketUs = sourceUs / 150_000L
    val frame: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, path, bucketUs) {
        value = withContext(Dispatchers.IO) {
            FrameCache.frame(path, bucketUs * 150_000L)?.asImageBitmap()
        }
    }
    frame?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}
