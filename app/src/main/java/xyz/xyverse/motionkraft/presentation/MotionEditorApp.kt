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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import xyz.xyverse.motionkraft.data.model.ClipType
import xyz.xyverse.motionkraft.data.model.JedagJedugPresets
import xyz.xyverse.motionkraft.data.model.VideoClip
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionKraftAppScreen(
    onPickMedia: () -> Unit,
    onPickImage: () -> Unit,
    vm: EditorViewModel
) {
    val state by vm.uiState.collectAsState()
    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MotionKraft", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = { vm.deleteSelectedClip() }, enabled = state.selectedClipId != null) {
                        Icon(Icons.Default.Delete, "Delete", tint = if (state.selectedClipId != null) Color(0xFFEF4444) else Color.Gray)
                    }
                    IconButton(onClick = { vm.splitAtPlayhead() }) {
                        Icon(Icons.Default.ContentCut, "Split")
                    }
                    IconButton(onClick = { vm.setBottomPanel(BottomPanel.EXPORT) }) {
                        Icon(Icons.Default.FileUpload, "Export", tint = MaterialTheme.colorScheme.secondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                PreviewAndTimeline(vm = vm, state = state)
                BottomToolBar(vm = vm, state = state)
                BottomPanelContent(vm = vm, state = state)
            }
        },
        floatingActionButton = {
            Box(modifier = Modifier.padding(bottom = 220.dp, end = 4.dp)) {
                FloatingActionButton(
                    onClick = { showAddMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.Add, "Add", modifier = Modifier.size(26.dp))
                }
            }
        }
    ) { pad ->
        Box(modifier = Modifier.padding(pad)) {
            if (state.isExporting) ExportProgressDialog(progress = state.exportProgress)
            state.lastError?.let {
                AlertDialog(
                    onDismissRequest = { vm.dismissError() },
                    title = { Text("Error") },
                    text = { Text(it) },
                    confirmButton = { TextButton(onClick = { vm.dismissError() }) { Text("OK") } }
                )
            }
            state.exportOutputPath?.let {
                AlertDialog(
                    onDismissRequest = { vm.dismissExportResult() },
                    title = { Text("Export complete") },
                    text = { Text("Saved to: $it") },
                    confirmButton = { TextButton(onClick = { vm.dismissExportResult() }) { Text("OK") } }
                )
            }
            state.statusMessage?.let {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 260.dp)
                    ) {
                        Text(it, color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), fontSize = 13.sp)
                    }
                }
            }
            if (showAddMenu) {
                AlertDialog(
                    onDismissRequest = { showAddMenu = false },
                    title = { Text("Add media") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showAddMenu = false; onPickMedia() }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Videocam, null); Spacer(Modifier.width(8.dp)); Text("Add video")
                            }
                            Button(onClick = { showAddMenu = false; onPickImage() }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text("Add image")
                            }
                            OutlinedButton(onClick = {
                                showAddMenu = false
                                vm.addTextLayer("Text")
                            }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Title, null); Spacer(Modifier.width(8.dp)); Text("Add text layer")
                            }
                        }
                    },
                    confirmButton = {}
                )
            }
        }
    }
}

@Composable
fun PreviewAndTimeline(vm: EditorViewModel, state: EditorUiState) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .background(Color.Black)
        ) {
            val firstVideo = state.project.timeline.clips
                .firstOrNull { it.type == ClipType.VIDEO && File(it.filePath ?: "").exists() }

            if (firstVideo != null) {
                VideoPreview(filePath = firstVideo.filePath!!, isPlaying = state.isPlaying, positionUs = state.currentPlayheadUs - firstVideo.startTimeUs)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                    Icon(Icons.Default.MovieCreation, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    Spacer(Modifier.height(8.dp))
                    Text("Import media to start editing", color = Color.Gray, fontSize = 13.sp)
                }
            }

            IconButton(
                onClick = { vm.togglePlay() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(58.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null,
                    tint = Color.White, modifier = Modifier.size(38.dp)
                )
            }

            Text(
                formatTimeUs(state.currentPlayheadUs) + " / " + formatTimeUs(state.totalDurationUs),
                color = Color.White, fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )

            // Transport controls
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.updatePlayhead(0) }) {
                    Icon(Icons.Default.SkipPrevious, null, tint = Color.White)
                }
                IconButton(onClick = { vm.updatePlayhead((state.currentPlayheadUs - 1_000_000L).coerceAtLeast(0)) }) {
                    Icon(Icons.Default.Replay5, null, tint = Color.White)
                }
                IconButton(onClick = { vm.updatePlayhead((state.currentPlayheadUs + 1_000_000L).coerceAtMost(state.totalDurationUs)) }) {
                    Icon(Icons.Default.Forward5, null, tint = Color.White)
                }
                IconButton(onClick = { vm.updatePlayhead(state.totalDurationUs) }) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White)
                }
            }
        }

        Spacer(Modifier.height(2.dp))
        TimelineView(vm = vm, state = state)
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
        val uri = Uri.fromFile(File(filePath))
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    LaunchedEffect(isPlaying) {
        player.playWhenReady = isPlaying
    }

    LaunchedEffect(positionUs) {
        if (positionUs in 0..player.duration) player.seekTo(positionUs)
    }

    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                this.player = player
                useController = false
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun TimelineView(vm: EditorViewModel, state: EditorUiState) {
    val total = state.totalDurationUs.coerceAtLeast(2_000_000L)
    val pxPerUs = 0.00008f * state.zoomLevel
    val widthPx = (total * pxPerUs).coerceIn(800f, 6000f)

    val maxTrack = state.project.timeline.clips.maxOfOrNull { it.track } ?: 0
    val trackHeight = 40.dp
    val trackCount = maxTrack + 1
    val rulerHeight = 20.dp

    Box(
        Modifier
            .fillMaxWidth()
            .height(rulerHeight + trackHeight * trackCount + 16.dp)
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ -> vm.applyZoom(zoom) }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val x = offset.x
                    val totalPx = size.width
                    val fraction = (x / totalPx.toFloat())
                    // Because timeline scrolls horizontally we simplify: map to full timeline width
                    // For non-scrolled view:
                    val t = (fraction * total).toLong().coerceIn(0, total)
                    vm.updatePlayhead(t)
                }
            }
    ) {
        Row(
            modifier = Modifier
                .width(widthPx.dp)
                .fillMaxHeight()
                .horizontalScroll(rememberScrollState())
        ) {
            Column {
                // Ruler with ticks every second
                Box(
                    Modifier.width(widthPx.dp).height(rulerHeight)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val seconds = total / 1_000_000
                    for (s in 0..seconds.toInt()) {
                        Box(
                            Modifier
                                .offset(x = (s * 1_000_000L * pxPerUs).dp)
                                .width(1.dp).fillMaxHeight()
                                .background(if (s % 5 == 0) Color.Gray else Color.DarkGray)
                        )
                    }
                }

                for (track in 0..maxTrack) {
                    Row(Modifier.height(trackHeight).width(widthPx.dp)) {
                        var lastEnd = 0L
                        state.project.timeline.clips
                            .filter { it.track == track }
                            .sortedBy { it.startTimeUs }
                            .forEach { clip ->
                                val gap = ((clip.startTimeUs - lastEnd) * pxPerUs).coerceAtLeast(0f)
                                if (gap > 0.5f) Spacer(Modifier.width(gap.dp))
                                ClipStrip(
                                    clip = clip,
                                    width = (clip.durationUs * pxPerUs).coerceAtLeast(32f),
                                    isSelected = state.selectedClipId == clip.id,
                                    onClick = { vm.selectClip(clip.id) }
                                )
                                lastEnd = clip.endTimeUs
                            }
                    }
                }
            }
        }

        // Playhead
        val phRatio = state.currentPlayheadUs / total.toFloat()
        Box(
            Modifier
                .offset(x = (phRatio * widthPx).dp)
                .width(2.dp).fillMaxHeight()
                .background(Color(0xFFEF4444))
        )
    }
}

@Composable
fun ClipStrip(clip: VideoClip, width: Float, isSelected: Boolean, onClick: () -> Unit) {
    val bg = when (clip.type) {
        ClipType.VIDEO -> MaterialTheme.colorScheme.primary
        ClipType.IMAGE -> MaterialTheme.colorScheme.tertiary
        ClipType.TEXT -> MaterialTheme.colorScheme.secondary
        else -> Color.Gray
    }
    val label = when (clip.type) {
        ClipType.VIDEO -> "V"
        ClipType.IMAGE -> "IMG"
        ClipType.TEXT -> clip.textContent.take(10)
        else -> "?"
    }
    Box(
        Modifier
            .width(width.dp).fillMaxHeight()
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .border(if (isSelected) 2.dp else 0.dp, Color.White, RoundedCornerShape(5.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
fun BottomToolBar(vm: EditorViewModel, state: EditorUiState) {
    data class ToolDef(val panel: BottomPanel, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val tools = listOf(
        ToolDef(BottomPanel.EFFECTS, "Effects", Icons.Default.AutoAwesome),
        ToolDef(BottomPanel.COLOR, "Color", Icons.Default.Palette),
        ToolDef(BottomPanel.TRANSFORM, "Transform", Icons.Default.Transform),
        ToolDef(BottomPanel.TEXT, "Text", Icons.Default.Title),
        ToolDef(BottomPanel.TRANSITION, "Trans.", Icons.Default.SwitchVideo),
        ToolDef(BottomPanel.SPEED, "Speed", Icons.Default.Speed),
        ToolDef(BottomPanel.AUDIO, "Audio", Icons.Default.MusicNote)
    )
    Surface(tonalElevation = 6.dp) {
        LazyRow(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items(tools) { t ->
                val sel = state.bottomPanel == t.panel
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(68.dp).clickable { vm.setBottomPanel(t.panel) }) {
                    Icon(t.icon, t.label, tint = if (sel) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(24.dp))
                    Text(t.label, fontSize = 10.sp, color = if (sel) MaterialTheme.colorScheme.primary else Color.Gray)
                }
            }
        }
    }
}

@Composable
fun BottomPanelContent(vm: EditorViewModel, state: EditorUiState) {
    if (state.bottomPanel == BottomPanel.NONE) { Spacer(Modifier.height(0.dp)); return }
    Surface(Modifier.fillMaxWidth().height(220.dp), color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
        when (state.bottomPanel) {
            BottomPanel.EFFECTS -> EffectsPanel(vm, state)
            BottomPanel.COLOR -> ColorPanel(vm, state)
            BottomPanel.TRANSFORM -> TransformPanel(vm, state)
            BottomPanel.TEXT -> TextPanel(vm, state)
            BottomPanel.SPEED -> SpeedPanel(vm, state)
            BottomPanel.EXPORT -> ExportPanel(vm, state)
            else -> Box(Modifier.padding(16.dp)) { Text("Coming soon", color = Color.Gray) }
        }
    }
}

@Composable
fun EffectsPanel(vm: EditorViewModel, state: EditorUiState) {
    val jj = JedagJedugPresets.ALL_EFFECTS.filter { it.category == xyz.xyverse.motionkraft.data.model.EffectCategory.JJEDAG_JEDUG || it.category == xyz.xyverse.motionkraft.data.model.EffectCategory.TRANSITION }
    val filters = JedagJedugPresets.ALL_EFFECTS.filter { it.category != xyz.xyverse.motionkraft.data.model.EffectCategory.JJEDAG_JEDUG && it.category != xyz.xyverse.motionkraft.data.model.EffectCategory.TRANSITION }
    Column(Modifier.padding(10.dp)) {
        Text("Beat Effects", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(jj) { fx -> EffectTile(fx.name, fx.previewColor) { vm.applyJedagJedug(fx.id) } }
        }
        Spacer(Modifier.height(10.dp))
        Text("Filters", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { fx ->
                EffectTile(fx.name, fx.previewColor) {
                    state.selectedClipId?.let { id ->
                        state.project.timeline.getClipById(id)?.effects?.add(
                            xyz.xyverse.motionkraft.data.model.EffectInstance(fx.id)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EffectTile(name: String, color: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.width(64.dp)) {
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(Color(color), Color(color).copy(alpha = 0.6f)))),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(name, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
fun ColorPanel(vm: EditorViewModel, state: EditorUiState) {
    val clip = state.selectedClipId?.let { state.project.timeline.getClipById(it) }
    Column(Modifier.padding(14.dp)) {
        Text("Color", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        if (clip == null) {
            Text("Select a clip first", color = Color.Gray, fontSize = 12.sp)
        } else {
            Text("Brightness", fontSize = 11.sp, color = Color.Gray)
            Slider(clip.brightness, { vm.adjustColor(brightness = it) }, valueRange = -1f..1f)
            Text("Contrast", fontSize = 11.sp, color = Color.Gray)
            Slider(clip.contrast, { vm.adjustColor(contrast = it) }, valueRange = 0f..2f)
            Text("Saturation", fontSize = 11.sp, color = Color.Gray)
            Slider(clip.saturation, { vm.adjustColor(saturation = it) }, valueRange = 0f..3f)
        }
    }
}

@Composable
fun TransformPanel(vm: EditorViewModel, state: EditorUiState) {
    Column(Modifier.padding(14.dp)) {
        Text("Transform", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Keyframe curve editor, position, scale, and rotation controls are scheduled for the next release.",
            fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { }) { Text("Flip H") }
            OutlinedButton(onClick = { }) { Text("Flip V") }
            OutlinedButton(onClick = { }) { Text("Reset") }
        }
    }
}

@Composable
fun TextPanel(vm: EditorViewModel, state: EditorUiState) {
    var text by remember { mutableStateOf("") }
    Column(Modifier.padding(14.dp)) {
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Text") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = { if (text.isNotBlank()) vm.addTextLayer(text) }, enabled = text.isNotBlank()) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add text layer")
        }
    }
}

@Composable
fun SpeedPanel(vm: EditorViewModel, state: EditorUiState) {
    val clip = state.selectedClipId?.let { state.project.timeline.getClipById(it) }
    Column(Modifier.padding(14.dp)) {
        Text("Speed", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        if (clip == null) {
            Text("Select a video clip first", color = Color.Gray, fontSize = 12.sp)
        } else {
            val sp = clip.speed
            Text(String.format("%.2fx", sp), fontSize = 12.sp)
            Slider(sp, { vm.setClipSpeed(it) }, valueRange = 0.1f..4f, steps = 38)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 3f)) { s ->
                    FilterChip(
                        selected = Math.abs(sp - s) < 0.05f,
                        onClick = { vm.setClipSpeed(s) },
                        label = { Text("${s}x", fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExportPanel(vm: EditorViewModel, state: EditorUiState) {
    val ex = state.project.exportSettings
    Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Export", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Text("Resolution", fontSize = 11.sp, color = Color.Gray)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(720 to 1280, 1080 to 1920, 1440 to 2560, 2160 to 3840)) { (w, h) ->
                FilterChip(selected = ex.width == w, onClick = { vm.updateExportSettings(width = w, height = h) },
                    label = { Text("${h}p", fontSize = 11.sp) })
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Frame rate", fontSize = 11.sp, color = Color.Gray)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(24, 30, 60)) { f ->
                FilterChip(selected = ex.fps == f, onClick = { vm.updateExportSettings(fps = f) },
                    label = { Text("${f}fps", fontSize = 11.sp) })
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Bitrate: ${ex.bitrate / 1_000_000} Mbps", fontSize = 11.sp, color = Color.Gray)
        Slider(ex.bitrate.toFloat(), { vm.updateExportSettings(bitrate = it.toLong()) },
            valueRange = 4_000_000f..50_000_000f)
        Spacer(Modifier.height(10.dp))
        Button(onClick = { vm.exportVideo() }, enabled = !state.isExporting, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FileUpload, null); Spacer(Modifier.width(8.dp))
            Text(if (state.isExporting) "Exporting..." else "Start export")
        }
        Text("${state.project.timeline.clips.size} clips on timeline", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun ExportProgressDialog(progress: Float) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Exporting video", fontSize = 16.sp) },
        text = {
            Column {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text("${(progress * 100).toInt()}%", fontSize = 12.sp)
            }
        },
        confirmButton = { }
    )
}

fun formatTimeUs(us: Long): String {
    val totalSeconds = us / 1_000_000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    val cs = (us % 1_000_000) / 10_000
    return String.format("%02d:%02d.%02d", m, s, cs)
}
