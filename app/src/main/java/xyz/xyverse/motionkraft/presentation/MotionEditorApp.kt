package xyz.xyverse.motionkraft.presentation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import xyz.xyverse.motionkraft.data.model.*
import java.io.File

// ---------- Color tokens ----------
private val MkColors = object {
    val bg = Color(0xFF0B0B10)
    val surface = Color(0xFF15151D)
    val surface2 = Color(0xFF1D1D29)
    val surface3 = Color(0xFF262633)
    val divider = Color(0xFF2A2A38)
    val primary = Color(0xFF6366F1)      // indigo
    val accent = Color(0xFF14B8A6)       // teal
    val danger = Color(0xFFEF4444)
    val text = Color(0xFFF1F1F5)
    val textDim = Color(0xFF8A8A99)
    val video = Color(0xFF6366F1)
    val image = Color(0xFF14B8A6)
    val text_ = Color(0xFFEC4899)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionKraftAppScreen(
    onPickMedia: () -> Unit,
    onPickImage: () -> Unit,
    vm: EditorViewModel
) {
    val state by vm.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme(
        background = MkColors.bg,
        surface = MkColors.surface,
        primary = MkColors.primary,
        onPrimary = Color.White,
        onSurface = MkColors.text,
        onBackground = MkColors.text
    )) {
        Box(Modifier.fillMaxSize().background(MkColors.bg)) {
            Column(Modifier.fillMaxSize()) {
                TopBar(vm, state)
                PreviewSection(vm, state)
                TimelineSection(vm, state, onAdd = { showAddSheet = true })
                ToolDock(vm, state)
                InspectorPanel(vm, state)
            }

            // Add media bottom sheet
            if (showAddSheet) {
                AddMediaSheet(
                    onDismiss = { showAddSheet = false },
                    onVideo = { showAddSheet = false; onPickMedia() },
                    onImage = { showAddSheet = false; onPickImage() },
                    onText = { showAddSheet = false; showTextDialog = true }
                )
            }
            if (showTextDialog) {
                AddTextDialog(onDismiss = { showTextDialog = false }) { text ->
                    vm.addTextLayer(text)
                    showTextDialog = false
                }
            }

            // Export overlay
            if (state.isExporting) ExportProgress(progress = state.exportProgress)
            state.lastError?.let {
                InfoDialog(title = "Export failed", body = it, onOk = { vm.dismissError() })
            }
            state.exportOutputPath?.let {
                InfoDialog(title = "Export complete", body = "Saved to:\n$it", onOk = { vm.dismissExportResult() })
            }
            state.statusMessage?.let {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Surface(
                        color = MkColors.primary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(it, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
                    }
                }
            }
        }
    }
}

// ---------------- Top bar ----------------
@Composable
private fun TopBar(vm: EditorViewModel, state: EditorUiState) {
    Surface(color = MkColors.surface, tonalElevation = 0.dp) {
        Row(
            Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(
                    Brush.linearGradient(listOf(MkColors.primary, MkColors.accent))
                ), contentAlignment = Alignment.Center) {
                    Text("MK", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text("MotionKraft", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MkColors.text)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.selectedClipId != null) {
                    IconButton(onClick = { vm.deleteSelectedClip() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, null, tint = MkColors.danger, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { vm.splitAtPlayhead() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentCut, null, tint = MkColors.text, modifier = Modifier.size(20.dp))
                    }
                }
                Button(
                    onClick = { vm.setBottomPanel(BottomPanel.EXPORT) },
                    colors = ButtonDefaults.buttonColors(containerColor = MkColors.accent, contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ---------------- Preview ----------------
@Composable
private fun PreviewSection(vm: EditorViewModel, state: EditorUiState) {
    val firstVideo = state.project.timeline.clips
        .firstOrNull { it.type == ClipType.VIDEO && File(it.filePath ?: "").exists() }

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .background(Color.Black)
    ) {
        if (firstVideo != null) {
            VideoPreview(filePath = firstVideo.filePath!!, isPlaying = state.isPlaying,
                positionUs = state.currentPlayheadUs - firstVideo.startTimeUs)
        } else {
            EmptyPreview()
        }

        // Timecode top-right
        Text(
            formatTimeUs(state.currentPlayheadUs) + " / " + formatTimeUs(state.totalDurationUs),
            color = Color.White, fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Selected clip name top-left
        state.selectedClipId?.let { id ->
            val clip = state.project.timeline.getClipById(id)
            clip?.let {
                val label = when (it.type) {
                    ClipType.VIDEO -> "Video"
                    ClipType.IMAGE -> "Image"
                    ClipType.TEXT -> "\"${it.textContent.take(12)}\""
                    else -> "Clip"
                }
                Text(label, color = Color.White, fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }

        // Center play button
        IconButton(
            onClick = { vm.togglePlay() },
            modifier = Modifier.align(Alignment.Center).size(56.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null,
                tint = Color.White, modifier = Modifier.size(32.dp)
            )
        }

        // Transport bar bottom
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.updatePlayhead(0) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = { vm.updatePlayhead((state.currentPlayheadUs - 1_000_000L).coerceAtLeast(0)) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Replay5, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = { vm.updatePlayhead((state.currentPlayheadUs + 1_000_000L).coerceAtMost(state.totalDurationUs)) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Forward5, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = { vm.updatePlayhead(state.totalDurationUs) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun EmptyPreview() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.MovieCreation, null, modifier = Modifier.size(54.dp), tint = MkColors.textDim.copy(alpha = 0.7f))
            Spacer(Modifier.height(10.dp))
            Text("Start a new project", color = MkColors.textDim, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("Tap + to add video, image, or text", color = MkColors.textDim.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

@Composable
fun VideoPreview(filePath: String, isPlaying: Boolean, positionUs: Long) {
    val ctx = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            playWhenReady = isPlaying
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(filePath) {
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(filePath))))
        player.prepare()
    }
    LaunchedEffect(isPlaying) { player.playWhenReady = isPlaying }
    LaunchedEffect(positionUs) {
        val d = player.duration
        if (positionUs in 0..d) player.seekTo(positionUs)
    }
    AndroidView(
        factory = { PlayerView(it).apply { this.player = player; useController = false } },
        modifier = Modifier.fillMaxSize()
    )
}

// ---------------- Timeline ----------------
@Composable
private fun TimelineSection(vm: EditorViewModel, state: EditorUiState, onAdd: () -> Unit) {
    val total = state.totalDurationUs.coerceAtLeast(2_000_000L)
    val pxPerUs = 0.00006f * state.zoomLevel
    val rulerHeight = 26.dp
    val trackHeight = 42.dp
    val maxTrack = state.project.timeline.clips.maxOfOrNull { it.track } ?: 0
    val trackCount = (maxTrack + 1).coerceAtLeast(2) // minimum 2 empty track rows
    val timelineWidthPx = (total * pxPerUs).coerceIn(1000f, 8000f)
    val scroll = rememberScrollState()

    Surface(color = MkColors.surface2, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth()) {
            // Timeline toolbar
            Row(
                Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = { vm.applyZoom(0.8f) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ZoomOut, null, tint = MkColors.textDim, modifier = Modifier.size(16.dp))
                    }
                    Text("${(state.zoomLevel * 100).toInt()}%", color = MkColors.textDim, fontSize = 11.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    IconButton(onClick = { vm.applyZoom(1.25f) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ZoomIn, null, tint = MkColors.textDim, modifier = Modifier.size(16.dp))
                    }
                }
                Text("Timeline", color = MkColors.textDim, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.updatePlayhead(0) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.SkipPrevious, null, tint = MkColors.textDim, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, null, tint = MkColors.accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Divider(color = MkColors.divider, thickness = 0.5.dp)

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(rulerHeight + trackHeight * trackCount + 6.dp)
                    .pointerInput(Unit) { detectTransformGestures { _, _, z, _ -> vm.applyZoom(z) } }
            ) {
                val scrollState = rememberScrollState()
                Row(
                    Modifier.width(timelineWidthPx.dp).fillMaxHeight()
                        .horizontalScroll(scrollState)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val t = (offset.x / size.width.toFloat() * total).toLong().coerceIn(0, total)
                                vm.updatePlayhead(t)
                            }
                        }
                ) {
                    Column(Modifier.width(timelineWidthPx.dp)) {
                        // Ruler
                        Box(Modifier.fillMaxWidth().height(rulerHeight).background(MkColors.surface)) {
                            val seconds = total / 1_000_000
                            for (s in 0..seconds.toInt()) {
                                val x = (s * 1_000_000L * pxPerUs)
                                val major = s % 5 == 0
                                Box(Modifier.offset(x = x.dp).width(1.dp).fillMaxHeight()
                                    .background(if (major) MkColors.textDim else MkColors.divider))
                                if (major) {
                                    Text(
                                        if (s >= 60) "%d:%02d".format(s / 60, s % 60) else "$s",
                                        color = MkColors.textDim, fontSize = 9.sp,
                                        modifier = Modifier.offset(x = (x + 3).dp, y = 5.dp)
                                    )
                                }
                            }
                        }
                        Divider(color = MkColors.divider, thickness = 0.5.dp)
                        for (track in 0 until trackCount) {
                            Row(
                                Modifier.fillMaxWidth().height(trackHeight)
                                    .background(if (track % 2 == 0) MkColors.surface2 else MkColors.surface)
                            ) {
                                val clips = state.project.timeline.clips.filter { it.track == track }.sortedBy { it.startTimeUs }
                                var cursor = 0L
                                clips.forEach { clip ->
                                    val gap = clip.startTimeUs - cursor
                                    if (gap > 10_000) Spacer(Modifier.width((gap * pxPerUs).dp))
                                    TimelineClipStrip(
                                        clip = clip,
                                        width = (clip.durationUs * pxPerUs).coerceAtLeast(40f),
                                        isSelected = state.selectedClipId == clip.id,
                                        onClick = { vm.selectClip(clip.id) }
                                    )
                                    cursor = clip.endTimeUs
                                }
                            }
                            Divider(color = MkColors.divider, thickness = 0.5.dp)
                        }
                    }
                }

                // Playhead
                val ratio = state.currentPlayheadUs / total.toFloat()
                Box(Modifier.offset(x = (ratio * timelineWidthPx).dp).width(2.dp).fillMaxHeight().background(MkColors.danger))
            }
        }
    }
}

@Composable
private fun TimelineClipStrip(clip: VideoClip, width: Float, isSelected: Boolean, onClick: () -> Unit) {
    val (bg, icon, label) = when (clip.type) {
        ClipType.VIDEO -> Triple(MkColors.video, Icons.Default.Videocam, "Video")
        ClipType.IMAGE -> Triple(MkColors.image, Icons.Default.Image, "Image")
        ClipType.TEXT -> Triple(MkColors.text_, Icons.Default.Title, clip.textContent.take(8))
        else -> Triple(Color.Gray, Icons.Default.AttachFile, "Audio")
    }
    Box(
        Modifier
            .width(width.dp).fillMaxHeight()
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) Color.White else Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(14.dp))
            if (width > 70) {
                Spacer(Modifier.width(4.dp))
                Text(label, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Clip)
            }
        }
        // Left/right fade edge hints
        Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter).background(
            Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.0f), Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.0f)))
        ))
    }
}

// ---------------- Tool dock ----------------
private data class Tool(val panel: BottomPanel, val label: String, val icon: ImageVector)

@Composable
private fun ToolDock(vm: EditorViewModel, state: EditorUiState) {
    val tools = listOf(
        Tool(BottomPanel.EFFECTS, "Effects", Icons.Default.AutoAwesome),
        Tool(BottomPanel.COLOR, "Color", Icons.Default.Palette),
        Tool(BottomPanel.TRANSFORM, "Transform", Icons.Default.Transform),
        Tool(BottomPanel.TEXT, "Text", Icons.Default.Title),
        Tool(BottomPanel.SPEED, "Speed", Icons.Default.Speed),
        Tool(BottomPanel.TRANSITION, "Trans.", Icons.Default.SwitchVideo),
        Tool(BottomPanel.AUDIO, "Audio", Icons.Default.MusicNote)
    )
    Surface(color = MkColors.surface) {
        LazyRow(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items(tools) { t ->
                val sel = state.bottomPanel == t.panel
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp).clickable { vm.setBottomPanel(t.panel) }
                ) {
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (sel) MkColors.primary.copy(alpha = 0.18f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(t.icon, t.label, tint = if (sel) MkColors.primary else MkColors.textDim, modifier = Modifier.size(20.dp))
                    }
                    Text(t.label, fontSize = 9.sp, color = if (sel) MkColors.primary else MkColors.textDim, fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal)
                }
            }
        }
    }
}

// ---------------- Inspector panel (the bottom sheet-like content) ----------------
@Composable
private fun InspectorPanel(vm: EditorViewModel, state: EditorUiState) {
    val panel = state.bottomPanel
    if (panel == BottomPanel.NONE) {
        Spacer(Modifier.navigationBarsPadding())
        return
    }
    Surface(color = MkColors.surface2, modifier = Modifier.fillMaxWidth().height(230.dp), tonalElevation = 6.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(panelLabel(panel), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MkColors.text)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.setBottomPanel(BottomPanel.NONE) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, tint = MkColors.textDim, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            when (panel) {
                BottomPanel.EFFECTS -> EffectsPanel(vm, state)
                BottomPanel.COLOR -> ColorPanel(vm, state)
                BottomPanel.TRANSFORM -> TransformPanel()
                BottomPanel.TEXT -> TextPanel(onAdd = { vm.addTextLayer(it) })
                BottomPanel.SPEED -> SpeedPanel(vm, state)
                BottomPanel.EXPORT -> ExportPanel(vm, state)
                else -> Box(Modifier.padding(top = 20.dp)) {
                    Text("Coming soon", color = MkColors.textDim, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun panelLabel(p: BottomPanel) = when (p) {
    BottomPanel.EFFECTS -> "Effects"
    BottomPanel.COLOR -> "Color"
    BottomPanel.TRANSFORM -> "Transform"
    BottomPanel.TEXT -> "Text"
    BottomPanel.SPEED -> "Speed"
    BottomPanel.TRANSITION -> "Transition"
    BottomPanel.AUDIO -> "Audio"
    BottomPanel.EXPORT -> "Export"
    else -> ""
}

// ---------- Panel implementations ----------
@Composable
private fun EffectsPanel(vm: EditorViewModel, state: EditorUiState) {
    val jj = JedagJedugPresets.ALL_EFFECTS.filter { it.category == EffectCategory.JJEDAG_JEDUG || it.category == EffectCategory.TRANSITION }
    val filters = JedagJedugPresets.ALL_EFFECTS.filter { it.category != EffectCategory.JJEDAG_JEDUG && it.category != EffectCategory.TRANSITION }
    Column {
        Text("Beat Effects", color = MkColors.textDim, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(jj) { fx -> EffectTile(fx.name, fx.previewColor) { vm.applyJedagJedug(fx.id) } }
        }
        Spacer(Modifier.height(10.dp))
        Text("Filters & Blur", color = MkColors.textDim, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { fx ->
                EffectTile(fx.name, fx.previewColor) {
                    state.selectedClipId?.let { id ->
                        state.project.timeline.getClipById(id)?.effects?.add(EffectInstance(fx.id))
                    }
                }
            }
        }
    }
}

@Composable
private fun EffectTile(name: String, color: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.width(68.dp)) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(Color(color), Color(color).copy(alpha = 0.6f))))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(name, fontSize = 9.5.sp, color = MkColors.text, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ColorPanel(vm: EditorViewModel, state: EditorUiState) {
    val clip = state.selectedClipId?.let { state.project.timeline.getClipById(it) }
    if (clip == null) {
        EmptySelection("Select a clip to adjust color")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LabeledSlider("Brightness", clip.brightness, -1f..1f) { vm.adjustColor(brightness = it) }
        LabeledSlider("Contrast", clip.contrast, 0f..2f) { vm.adjustColor(contrast = it) }
        LabeledSlider("Saturation", clip.saturation, 0f..3f) { vm.adjustColor(saturation = it) }
    }
}

@Composable
private fun TransformPanel() {
    EmptySelection("Keyframe transform and curve editor are coming in the next build.")
}

@Composable
private fun SpeedPanel(vm: EditorViewModel, state: EditorUiState) {
    val clip = state.selectedClipId?.let { state.project.timeline.getClipById(it) }
    if (clip == null) {
        EmptySelection("Select a video clip to adjust speed")
        return
    }
    val sp = clip.speed
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Speed", color = MkColors.text, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(String.format("%.2fx", sp), color = MkColors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(sp, { vm.setClipSpeed(it) }, valueRange = 0.1f..4f, steps = 38,
            colors = SliderDefaults.colors(thumbColor = MkColors.accent, activeTrackColor = MkColors.accent))
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 3f)) { s ->
                FilterChip(
                    selected = Math.abs(sp - s) < 0.05f,
                    onClick = { vm.setClipSpeed(s) },
                    label = { Text("${s}x", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MkColors.accent.copy(alpha = 0.2f),
                        selectedLabelColor = MkColors.accent
                    )
                )
            }
        }
    }
}

@Composable
private fun TextPanel(onAdd: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Add text layer to timeline at playhead", color = MkColors.textDim, fontSize = 12.sp)
        var text by remember { mutableStateOf("") }
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            label = { Text("Text", fontSize = 12.sp) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MkColors.accent, cursorColor = MkColors.accent)
        )
        Button(
            onClick = { if (text.isNotBlank()) onAdd(text) },
            enabled = text.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = MkColors.accent, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Add layer") }
    }
}

@Composable
private fun ExportPanel(vm: EditorViewModel, state: EditorUiState) {
    val ex = state.project.exportSettings
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${state.project.timeline.clips.size} clip(s) on timeline", color = MkColors.textDim, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        Text("Resolution", color = MkColors.textDim, fontSize = 10.sp, letterSpacing = 0.6.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(720 to 1280, 1080 to 1920, 1440 to 2560, 2160 to 3840)) { (w, h) ->
                Chip(selected = ex.width == w, label = "${h}p") { vm.updateExportSettings(width = w, height = h) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Frame rate", color = MkColors.textDim, fontSize = 10.sp, letterSpacing = 0.6.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(24, 30, 60)) { f ->
                Chip(selected = ex.fps == f, label = "${f}fps") { vm.updateExportSettings(fps = f) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bitrate", color = MkColors.textDim, fontSize = 10.sp, letterSpacing = 0.6.sp)
            Spacer(Modifier.weight(1f))
            Text("${ex.bitrate / 1_000_000} Mbps", color = MkColors.text, fontSize = 11.sp)
        }
        Slider(ex.bitrate.toFloat(), { vm.updateExportSettings(bitrate = it.toLong()) },
            valueRange = 4_000_000f..50_000_000f,
            colors = SliderDefaults.colors(thumbColor = MkColors.accent, activeTrackColor = MkColors.accent))
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = { vm.exportVideo() }, enabled = !state.isExporting,
            colors = ButtonDefaults.buttonColors(containerColor = MkColors.accent, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (state.isExporting) "Exporting..." else "Start export", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------- Reusable small components ----------
@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPoint<Float>, onValue: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = MkColors.text, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(String.format("%.2f", value), color = MkColors.textDim, fontSize = 11.sp)
        }
        Slider(value, onValue, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = MkColors.primary, activeTrackColor = MkColors.primary))
    }
}

@Composable
private fun Chip(selected: Boolean, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MkColors.primary.copy(alpha = 0.18f) else MkColors.surface3,
        contentColor = if (selected) MkColors.primary else MkColors.textDim,
        modifier = Modifier.border(1.dp, if (selected) MkColors.primary else Color.Transparent, RoundedCornerShape(8.dp))
    ) {
        Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun EmptySelection(msg: String) {
    Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.TouchApp, null, tint = MkColors.textDim.copy(alpha = 0.6f), modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(6.dp))
            Text(msg, color = MkColors.textDim, fontSize = 12.sp)
        }
    }
}

// ---------- Dialogs / sheets ----------
@Composable
private fun AddMediaSheet(onDismiss: () -> Unit, onVideo: () -> Unit, onImage: () -> Unit, onText: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MkColors.surface, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Add to timeline", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MkColors.text)
                Spacer(Modifier.height(14.dp))
                SheetItem(Icons.Default.Videocam, "Video", "Import from gallery", MkColors.video) { onVideo() }
                SheetItem(Icons.Default.Image, "Image", "Photo or still frame", MkColors.image) { onImage() }
                SheetItem(Icons.Default.Title, "Text", "Overlay text layer", MkColors.text_) { onText() }
            }
        }
    }
}

@Composable
private fun SheetItem(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = MkColors.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MkColors.textDim, fontSize = 11.sp)
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = MkColors.textDim, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AddTextDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MkColors.surface, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Add text", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MkColors.text)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Text content") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MkColors.accent, cursorColor = MkColors.accent)
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = MkColors.textDim) }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = { if (text.isNotBlank()) onConfirm(text) },
                        enabled = text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MkColors.accent, contentColor = Color.Black)
                    ) { Text("Add") }
                }
            }
        }
    }
}

@Composable
private fun ExportProgress(progress: Float) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(14.dp), color = MkColors.surface, modifier = Modifier.width(260.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(progress = { progress }, color = MkColors.accent, strokeWidth = 4.dp, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(14.dp))
                Text("Exporting video", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MkColors.text)
                Spacer(Modifier.height(4.dp))
                Text("${(progress * 100).toInt()}%", color = MkColors.textDim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun InfoDialog(title: String, body: String, onOk: () -> Unit) {
    Dialog(onDismissRequest = onOk) {
        Surface(shape = RoundedCornerShape(14.dp), color = MkColors.surface, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MkColors.text)
                Spacer(Modifier.height(8.dp))
                Text(body, color = MkColors.textDim, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onOk, colors = ButtonDefaults.buttonColors(containerColor = MkColors.accent, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}

fun formatTimeUs(us: Long): String {
    val totalSeconds = us / 1_000_000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    val cs = (us % 1_000_000) / 10_000
    return String.format("%02d:%02d.%02d", m, s, cs)
}
