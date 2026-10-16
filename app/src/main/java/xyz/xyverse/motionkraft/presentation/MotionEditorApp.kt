package xyz.xyverse.motionkraft.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import xyz.xyverse.motionkraft.data.model.JedagJedugPresets
import xyz.xyverse.motionkraft.data.model.VideoClip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionKraftApp() {
    val vm: EditorViewModel = viewModel()
    val state by vm.uiState.collectAsState()

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { importMediaFromUri(vm, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("MotionKraft", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = { vm.setBottomPanel(BottomPanel.EXPORT) }) {
                        Icon(Icons.Default.FileUpload, "Export", tint = MaterialTheme.colorScheme.secondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                // Preview & timeline area
                PreviewAndTimeline(
                    vm = vm,
                    state = state
                )
                // Bottom tools
                BottomToolBar(vm = vm, state = state)
                // Panel content
                BottomPanelContent(vm = vm, state = state)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 200.dp)
            ) {
                Icon(Icons.Default.Add, "Add Media")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isExporting) {
                ExportProgressDialog(progress = state.exportProgress)
            }
            state.lastError?.let {
                AlertDialog(
                    onDismissRequest = { vm.undoError() },
                    title = { Text("Error") },
                    text = { Text(it) },
                    confirmButton = {
                        TextButton(onClick = { vm.undoError() }) { Text("OK") }
                    }
                )
            }
            state.exportOutputUri?.let {
                AlertDialog(
                    onDismissRequest = { vm.uiState.value.exportOutputUri?.let { } },
                    title = { Text("Export complete") },
                    text = { Text("Video saved to Movies/MotionKraft") },
                    confirmButton = {
                        TextButton(onClick = { vm.uiState.value.exportOutputUri.let { } }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PreviewAndTimeline(vm: EditorViewModel, state: EditorUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Preview player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (state.project.timeline.clips.isNotEmpty()) {
                // Simple preview placeholder, di versi penuh ini akan pakai ExoPlayer dengan OpenGL preview
                VideoPreview(vm, state)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MovieCreation,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Import media to start editing", color = Color.Gray)
                    Text("Tap + to add video or images", color = Color.Gray, fontSize = 12.sp)
                }
            }

            // Play button overlay
            IconButton(
                onClick = { vm.togglePlay() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Playhead timecode
            Text(
                text = formatTimeUs(state.currentPlayheadUs) + " / " + formatTimeUs(state.project.timeline.getTotalDuration()),
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        // Timeline
        TimelineView(vm = vm, state = state)
    }
}

@Composable
fun VideoPreview(vm: EditorViewModel, state: EditorUiState) {
    val ctx = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            playWhenReady = state.isPlaying
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(state.isPlaying) {
        player.playWhenReady = state.isPlaying
    }

    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                this.player = player
                useController = false
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun TimelineView(vm: EditorViewModel, state: EditorUiState) {
    val totalDuration = state.project.timeline.getTotalDuration()
    val pxPerUs = 0.0001f * state.zoomLevel // pixels per microsecond
    val timelineWidth = (totalDuration * pxPerUs).coerceAtLeast(2000f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    vm.applyZoom(zoom)
                }
            }
    ) {
        // Tracks & clips
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .width(timelineWidth.dp)
                .horizontalScroll(scrollState)
                .fillMaxHeight()
                .padding(vertical = 8.dp)
        ) {
            // Time ruler
            Box(
                modifier = Modifier
                    .width(timelineWidth.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(Modifier.height(4.dp))

            // Clip strips per track
            val maxTrack = state.project.timeline.clips.maxOfOrNull { it.track } ?: 0
            Column {
                for (track in 0..maxTrack) {
                    Row(modifier = Modifier.height(44.dp).width(timelineWidth.dp)) {
                        state.project.timeline.clips
                            .filter { it.track == track }
                            .forEach { clip ->
                                val startX = (clip.startTimeUs * pxPerUs)
                                val width = (clip.durationUs * pxPerUs).coerceAtLeast(40f)
                                ClipStrip(
                                    clip = clip,
                                    width = width,
                                    isSelected = state.selectedClipId == clip.id,
                                    onClick = { vm.selectClip(clip.id) }
                                )
                                Spacer(Modifier.width(startX.dp))
                            }
                    }
                }
            }
        }

        // Playhead
        val playheadX = (state.currentPlayheadUs * pxPerUs)
        Box(
            modifier = Modifier
                .offset(x = playheadX.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.Red)
        )
    }
}

@Composable
fun ClipStrip(clip: VideoClip, width: Float, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = when (clip.type) {
        xyz.xyverse.motionkraft.data.model.ClipType.VIDEO -> MaterialTheme.colorScheme.primary
        xyz.xyverse.motionkraft.data.model.ClipType.IMAGE -> MaterialTheme.colorScheme.tertiary
        xyz.xyverse.motionkraft.data.model.ClipType.TEXT -> MaterialTheme.colorScheme.secondary
        else -> Color.Gray
    }
    Box(
        modifier = Modifier
            .width(width.dp)
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(
                if (isSelected) 2.dp else 0.dp,
                Color.White,
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (clip.type) {
                xyz.xyverse.motionkraft.data.model.ClipType.VIDEO -> "Video"
                xyz.xyverse.motionkraft.data.model.ClipType.IMAGE -> "Image"
                xyz.xyverse.motionkraft.data.model.ClipType.TEXT -> "${clip.textContent.take(8)}"
                else -> "Clip"
            },
            color = Color.White,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
fun BottomToolBar(vm: EditorViewModel, state: EditorUiState) {
    data class ToolDef(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

    val tools = listOf(
        BottomPanel.EFFECTS to ToolDef("Effects", Icons.Default.AutoAwesome),
        BottomPanel.COLOR to ToolDef("Filters", Icons.Default.Palette),
        BottomPanel.TRANSFORM to ToolDef("Transform", Icons.Default.Transform),
        BottomPanel.TEXT to ToolDef("Text", Icons.Default.Title),
        BottomPanel.TRANSITION to ToolDef("Transition", Icons.Default.SwitchVideo),
        BottomPanel.SPEED to ToolDef("Speed", Icons.Default.Speed),
        BottomPanel.AUDIO to ToolDef("Audio", Icons.Default.MusicNote)
    )

    Surface(tonalElevation = 8.dp) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items(tools) { entry ->
                val panel = entry.first
                val def = entry.second
                val label = def.label
                val icon = def.icon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(70.dp)
                        .clickable { vm.setBottomPanel(panel) }
                ) {
                    val selected = state.bottomPanel == panel
                    Icon(
                        icon, label,
                        tint = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        label, fontSize = 10.sp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun BottomPanelContent(vm: EditorViewModel, state: EditorUiState) {
    if (state.bottomPanel == BottomPanel.NONE) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        when (state.bottomPanel) {
            BottomPanel.EFFECTS -> EffectsPanel(vm, state)
            BottomPanel.COLOR -> ColorPanel(vm, state)
            BottomPanel.TRANSFORM -> TransformPanel(vm, state)
            BottomPanel.TEXT -> TextPanel(vm, state)
            BottomPanel.TRANSITION -> Text("Transisi (transisi antar klip)").let { }
            BottomPanel.SPEED -> SpeedPanel(vm, state)
            BottomPanel.AUDIO -> Text("Audio Panel")
            BottomPanel.EXPORT -> ExportPanel(vm, state)
            else -> {}
        }
    }
}

@Composable
fun EffectsPanel(vm: EditorViewModel, state: EditorUiState) {
    val jjEffects = JedagJedugPresets.ALL_EFFECTS.filter { it.category != xyz.xyverse.motionkraft.data.model.EffectCategory.COLOR && it.category != xyz.xyverse.motionkraft.data.model.EffectCategory.BLUR && it.category != xyz.xyverse.motionkraft.data.model.EffectCategory.GLOW }
    val colorEffects = JedagJedugPresets.ALL_EFFECTS.filter { it.category == xyz.xyverse.motionkraft.data.model.EffectCategory.COLOR || it.category == xyz.xyverse.motionkraft.data.model.EffectCategory.BLUR || it.category == xyz.xyverse.motionkraft.data.model.EffectCategory.GLOW }

    Column(modifier = Modifier.padding(12.dp)) {
        Text("Jedag Jedug Effects", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(jjEffects) { effect ->
                EffectCard(effect.name, effect.previewColor) {
                    vm.applyJedagJedug(effect.id, bpm = 120f, intensity = 0.6f)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Filters & Blur", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(colorEffects) { effect ->
                EffectCard(effect.name, effect.previewColor) {
                    state.selectedClipId?.let {
                        val clip = state.project.timeline.getClipById(it)
                        clip?.effects?.add(xyz.xyverse.motionkraft.data.model.EffectInstance(effect.id))
                    }
                }
            }
        }
    }
}

@Composable
fun EffectCard(name: String, color: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(color), Color(color).copy(alpha = 0.6f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
        }
        Spacer(Modifier.height(4.dp))
        Text(name, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
fun ColorPanel(vm: EditorViewModel, state: EditorUiState) {
    val clip = state.selectedClipId?.let { state.project.timeline.getClipById(it) }
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Color", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (clip == null) {
            Text("Select a clip first", color = Color.Gray)
        } else {
            Text("Brightness")
            Slider(
                value = clip.brightness,
                onValueChange = { vm.adjustColor(brightness = it) },
                valueRange = -1f..1f
            )
            Text("Contrast")
            Slider(
                value = clip.contrast,
                onValueChange = { vm.adjustColor(contrast = it) },
                valueRange = 0f..2f
            )
            Text("Saturation")
            Slider(
                value = clip.saturation,
                onValueChange = { vm.adjustColor(saturation = it) },
                valueRange = 0f..3f
            )
        }
    }
}

@Composable
fun TransformPanel(vm: EditorViewModel, state: EditorUiState) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Transform", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Curve editor for manual keyframe control is scheduled for an upcoming release", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { /* mirror */ }) { Text("Flip H") }
            Button(onClick = { /* flip */ }) { Text("Flip V") }
            Button(onClick = { /* reset */ }) { Text("Reset") }
        }
    }
}

@Composable
fun TextPanel(vm: EditorViewModel, state: EditorUiState) {
    var text by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Text") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { if (text.isNotBlank()) vm.addTextLayer(text) }) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(4.dp))
            Text("Add Text Layer")
        }
    }
}

@Composable
fun SpeedPanel(vm: EditorViewModel, state: EditorUiState) {
    val clip = state.selectedClipId?.let { state.project.timeline.getClipById(it) }
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Speed", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (clip == null) {
            Text("Select a video clip first", color = Color.Gray)
        } else {
            val speed = clip.speed
            Text("Kecepatan: ${String.format("%.1fx", speed)}")
            Slider(
                value = speed,
                onValueChange = { vm.setClipSpeed(it) },
                valueRange = 0.1f..4f,
                steps = 38
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 3f)) { s ->
                    FilterChip(
                        selected = speed == s,
                        onClick = { vm.setClipSpeed(s) },
                        label = { Text("${s}x") }
                    )
                }
            }
        }
    }
}

@Composable
fun ExportPanel(vm: EditorViewModel, state: EditorUiState) {
    val ex = state.project.exportSettings
    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Export Video", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("Resolution: ${ex.width}x${ex.height}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(720 to 1280, 1080 to 1920, 1440 to 2560, 2160 to 3840).forEach { (w, h) ->
                FilterChip(
                    selected = ex.width == w,
                    onClick = { vm.updateExportSettings(width = w, height = h) },
                    label = { Text("${h}p") }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("FPS: ${ex.fps}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(24, 30, 60).forEach { f ->
                FilterChip(
                    selected = ex.fps == f,
                    onClick = { vm.updateExportSettings(fps = f) },
                    label = { Text("${f}fps") }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Bitrate: ${ex.bitrate / 1_000_000} Mbps")
        Slider(
            value = ex.bitrate.toFloat(),
            onValueChange = { vm.updateExportSettings(bitrate = it.toLong()) },
            valueRange = 4_000_000f..50_000_000f
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { vm.exportVideo() },
            enabled = !state.isExporting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FileUpload, null)
            Spacer(Modifier.width(8.dp))
            Text(if (state.isExporting) "Exporting" else "Start Export")
        }
    }
}

@Composable
fun ExportProgressDialog(progress: Float) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Exporting Video") },
        text = {
            Column {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("${(progress * 100).toInt()}%")
            }
        },
        confirmButton = {}
    )
}

fun importMediaFromUri(vm: EditorViewModel, uri: Uri) {
    // Simpan implementation - akan copy file ke app storage dan detect duration
    // Untuk MVP: asumsikan video file
    vm.importVideo(uri.toString(), 5_000_000) // dummy duration 5s
}

fun formatTimeUs(us: Long): String {
    val totalSeconds = us / 1_000_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val ms = (us % 1_000_000) / 10_000
    return String.format("%02d:%02d.%02d", minutes, seconds, ms)
}

fun EditorViewModel.applyZoom(factor: Float) {
    val current = uiState.value.zoomLevel
    val newZoom = (current * factor).coerceIn(0.5f, 5f)
    setZoom(newZoom)
}
