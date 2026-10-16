package xyz.xyverse.motionkraft.presentation.screens

import android.graphics.SurfaceTexture
import android.net.Uri
import android.view.TextureView
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import xyz.xyverse.motionkraft.data.model.*
import xyz.xyverse.motionkraft.presentation.*
import xyz.xyverse.motionkraft.presentation.components.*
import java.io.File

// Dock tools: semua yang diminta
private data class DockTool(val panel: BottomPanel, val label: String, val icon: ImageVector, val pro: Boolean = false)

@Composable
fun EditorScreen(
    vm: EditorViewModel,
    onBack: () -> Unit,
    onPickMedia: () -> Unit,
    onPickImage: () -> Unit,
    onGoVip: () -> Unit
) {
    val state by vm.uiState.collectAsState()
    val vip by vm.isVip.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }

    val dock = listOf(
        DockTool(BottomPanel.EFFECTS, "Cine3D", Icons.Default.ViewInAr, pro = true),
        DockTool(BottomPanel.EFFECTS, "JJ", Icons.Default.FlashOn),
        DockTool(BottomPanel.EFFECTS, "Filter", Icons.Default.Filter),
        DockTool(BottomPanel.EFFECTS, "Efek", Icons.Default.AutoAwesome),
        DockTool(BottomPanel.TEXT, "Lirik", Icons.Default.QueueMusic, pro = true),
        DockTool(BottomPanel.TRANSFORM, "Morph", Icons.Default.Transform, pro = true),
        DockTool(BottomPanel.TRANSFORM, "Graph", Icons.Default.Timeline, pro = true),
        DockTool(BottomPanel.AUDIO, "Audio", Icons.Default.GraphicEq),
        DockTool(BottomPanel.TEXT, "Teks", Icons.Default.Title),
        DockTool(BottomPanel.COLOR, "Warna", Icons.Default.Palette),
        DockTool(BottomPanel.SPEED, "Speed", Icons.Default.Speed),
        DockTool(BottomPanel.EXPORT, "Export", Icons.Default.FileUpload)
    )

    Box(Modifier.fillMaxSize().background(Mk.bg)) {
        Column(Modifier.fillMaxSize()) {
            // Top bar (ungu gradient)
            Row(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Mk.purple700, Mk.bg2)))
                    .height(54.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(2.dp))
                Column {
                    Text(state.project.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${state.project.timeline.clips.size} klip", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                }
                Spacer(Modifier.weight(1f))
                if (state.selectedClipId != null) {
                    IconButton(onClick = { vm.splitAtPlayhead() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentCut, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { vm.deleteSelectedClip() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Mk.red, modifier = Modifier.size(20.dp))
                    }
                }
                MkButton("Export", modifier = Modifier.width(100.dp)) {
                    vm.setBottomPanel(BottomPanel.EXPORT)
                }
            }

            // Preview (custom TextureView + controls)
            EditorPreview(vm, state)

            // Timeline
            MkTimeline(vm, state, onAdd = { showAdd = true })

            // Dock
            LazyRow(
                Modifier.fillMaxWidth().background(Mk.bg2).padding(vertical = 8.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(dock) { t ->
                    ToolTile(t.icon, t.label, selected = state.bottomPanel == t.panel, pro = t.pro && !vip) {
                        if (t.pro && !vip) onGoVip() else vm.setBottomPanel(t.panel)
                    }
                }
            }

            // Inspector
            when (state.bottomPanel) {
                BottomPanel.EFFECTS -> EffectsInspector(vm, state, vip = vip, onVip = onGoVip)
                BottomPanel.COLOR -> ColorInspector(vm, state)
                BottomPanel.SPEED -> SpeedInspector(vm, state)
                BottomPanel.TEXT -> TextInspector(onAdd = { vm.addTextLayer(it) })
                BottomPanel.EXPORT -> ExportInspector(vm, state)
                BottomPanel.AUDIO -> ComingPanel("Audio editor & beat detection hadir segera di update berikutnya.")
                BottomPanel.TRANSFORM -> ComingPanel("Morph transition dan motion graph adalah fitur VIP. Upgrade untuk unlock!", cta = { onGoVip() })
                BottomPanel.TRANSITION -> ComingPanel("Transisi antar klip.")
                else -> Spacer(Modifier.height(8.dp).navigationBarsPadding())
            }
        }

        if (showAdd) {
            AddSheet(onDismiss = { showAdd = false }, onVideo = { showAdd = false; onPickMedia() }, onImage = { showAdd = false; onPickImage() }, onText = { showAdd = false; showTextDialog = true })
        }
        if (showTextDialog) {
            TextDialog(onDismiss = { showTextDialog = false }) { txt ->
                vm.addTextLayer(txt); showTextDialog = false
            }
        }
        if (state.isExporting) ExportProg(state.exportProgress)
        state.lastError?.let { InfoDialog("Export gagal", it) { vm.dismissError() } }
        state.exportOutputPath?.let { InfoDialog("Export selesai", "Tersimpan di:\n$it") { vm.dismissExportResult() } }
        state.statusMessage?.let {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Surface(color = Mk.purple, shape = RoundedCornerShape(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                    Text(it, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
                }
            }
        }
    }
}

// ---------------- Custom video surface (NO stock PlayerView) ----------------
@Composable
private fun EditorPreview(vm: EditorViewModel, state: EditorUiState) {
    val ctx = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            playWhenReady = state.isPlaying
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }
    var surfaceTexture by remember { mutableStateOf<SurfaceTexture?>(null) }
    val firstVideo = state.project.timeline.clips.firstOrNull { it.type == ClipType.VIDEO && File(it.filePath ?: "").exists() }

    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(firstVideo?.filePath) {
        if (firstVideo != null) {
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(firstVideo.filePath!!))))
            player.prepare()
        }
    }
    LaunchedEffect(state.isPlaying) { player.playWhenReady = state.isPlaying }
    LaunchedEffect(state.currentPlayheadUs, firstVideo?.id) {
        if (firstVideo != null) {
            val local = state.currentPlayheadUs - firstVideo.startTimeUs
            if (local in 0..(player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)) player.seekTo(local)
        }
    }
    LaunchedEffect(surfaceTexture) {
        val st = surfaceTexture ?: return@LaunchedEffect
        val surface = android.view.Surface(st)
        player.setVideoSurface(surface)
    }

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .background(Color.Black)
    ) {
        if (firstVideo != null) {
            AndroidView(
                factory = { TextureView(it).apply { surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(s: SurfaceTexture, w: Int, h: Int) { surfaceTexture = s }
                    override fun onSurfaceTextureSizeChanged(s: SurfaceTexture, w: Int, h: Int) {}
                    override fun onSurfaceTextureDestroyed(s: SurfaceTexture): Boolean { surfaceTexture = null; return true }
                    override fun onSurfaceTextureUpdated(s: SurfaceTexture) {}
                }} },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            EmptyPreview()
        }

        // Scanlines / vignette for cinematic feel
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.radialGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                center = center, radius = size.maxDimension / 1.4f
            ))
        }

        // Timecode
        Text(
            fmt(state.currentPlayheadUs) + " / " + fmt(state.totalDurationUs),
            color = Color.White, fontSize = 10.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        state.selectedClipId?.let { id ->
            val c = state.project.timeline.getClipById(id)
            c?.let {
                val lab = when (it.type) {
                    ClipType.VIDEO -> "VIDEO"
                    ClipType.IMAGE -> "GAMBAR"
                    ClipType.TEXT -> it.textContent.take(14)
                    else -> "CLIP"
                }
                Text(lab, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp,
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                        .background(Brush.linearGradient(listOf(Mk.purple, Mk.pink)), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }

        // Center play toggle
        Box(Modifier.align(Alignment.Center).size(62.dp).clip(CircleShape)
            .background(
                Brush.radialGradient(listOf(Mk.purple.copy(alpha = 0.5f), Mk.purple.copy(alpha = 0f)))
            ),
            contentAlignment = Alignment.Center) {
            Box(Modifier.size(54.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                .border(1.dp, Mk.purple400.copy(alpha = 0.5f), CircleShape)
                .clickable { vm.togglePlay() },
                contentAlignment = Alignment.Center) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null, tint = Color.White, modifier = Modifier.size(30.dp)
                )
            }
        }

        // Bottom transport bar (custom gradient, rounded pill buttons)
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Triple(Icons.Default.SkipPrevious, "start") { vm.updatePlayhead(0) },
                Triple(Icons.Default.Replay5, "-10s") { vm.updatePlayhead((state.currentPlayheadUs - 10_000_000L).coerceAtLeast(0)) },
                Triple(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "play") { vm.togglePlay() },
                Triple(Icons.Default.Forward5, "+10s") { vm.updatePlayhead((state.currentPlayheadUs + 10_000_000L).coerceAtMost(state.totalDurationUs)) },
                Triple(Icons.Default.SkipNext, "end") { vm.updatePlayhead(state.totalDurationUs) }
            ).forEach { (ic, _, act) ->
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable { act() },
                    contentAlignment = Alignment.Center) {
                    Icon(ic, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyPreview() {
    Box(Modifier.fillMaxSize().background(
        Brush.radialGradient(listOf(Mk.bg3, Mk.bg))
    ), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(80.dp).clip(CircleShape).background(Mk.purple.copy(alpha = 0.12f)).border(2.dp, Mk.purple.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MovieCreation, null, tint = Mk.purple300, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Mulai project baru", color = Mk.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text("Tap + untuk tambah video, gambar, atau teks", color = Mk.dim, fontSize = 12.sp)
        }
    }
}

// ---------------- Timeline (custom) ----------------
@Composable
private fun MkTimeline(vm: EditorViewModel, state: EditorUiState, onAdd: () -> Unit) {
    val total = state.totalDurationUs.coerceAtLeast(2_000_000L)
    val pxPerUs = 0.00007f * state.zoomLevel
    val rulerH = 26.dp
    val trackH = 40.dp
    val maxTrack = state.project.timeline.clips.maxOfOrNull { it.track } ?: 0
    val trackCount = (maxTrack + 1).coerceAtLeast(3)
    val widthPx = (total * pxPerUs).coerceIn(1000f, 9000f)
    val scroll = rememberScrollState()

    Surface(color = Mk.bg2) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.applyZoom(0.85f) }, Modifier.size(28.dp)) { Icon(Icons.Default.ZoomOut, null, tint = Mk.dim, Modifier.size(16.dp)) }
                    Text("${(state.zoomLevel*100).toInt()}%", color = Mk.dim, fontSize = 10.sp, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                    IconButton(onClick = { vm.applyZoom(1.18f) }, Modifier.size(28.dp)) { Icon(Icons.Default.ZoomIn, null, tint = Mk.dim, Modifier.size(16.dp)) }
                }
                Spacer(Modifier.weight(1f))
                Text("TIMELINE", color = Mk.dim, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.updatePlayhead(0) }, Modifier.size(28.dp)) { Icon(Icons.Default.SkipPrevious, null, tint = Mk.dim, Modifier.size(16.dp)) }
                IconButton(onClick = onAdd, Modifier.size(28.dp)) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Mk.purple, Mk.pink))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, null, tint = Color.White, Modifier.size(16.dp))
                    }
                }
            }
            androidx.compose.foundation.layout.Divider(color = Mk.line, thickness = 0.5.dp)

            Box(Modifier.fillMaxWidth().height(rulerH + trackH * trackCount + 6.dp)
                .pointerInput(Unit) { detectTransformGestures { _, _, z, _ -> vm.applyZoom(z) } }) {
                Row(Modifier.width(widthPx.dp).fillMaxHeight().horizontalScroll(scroll)
                    .pointerInput(Unit) {
                        detectTapGestures { off ->
                            val t = (off.x / size.width.toFloat() * total).toLong().coerceIn(0, total)
                            vm.updatePlayhead(t); vm.selectClip(null)
                        }
                    }) {
                    Column(Modifier.width(widthPx.dp)) {
                        Box(Modifier.fillMaxWidth().height(rulerH).background(Mk.bg)) {
                            val secs = total / 1_000_000
                            for (s in 0..secs.toInt()) {
                                val x = s * 1_000_000L * pxPerUs
                                val major = s % 5 == 0
                                Box(Modifier.offset(x = x.dp).width(1.dp).fillMaxHeight().background(if (major) Mk.dim else Mk.line))
                                if (major) {
                                    Text(if (s >= 60) "%d:%02d".format(s/60,s%60) else "$s", color = Mk.dim, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, modifier = Modifier.offset(x=(x+3).dp, y=5.dp))
                                }
                            }
                        }
                        androidx.compose.foundation.layout.Divider(color = Mk.line, thickness = 0.5.dp)
                        for (track in 0 until trackCount) {
                            Row(Modifier.fillMaxWidth().height(trackH).background(if (track%2==0) Mk.bg2 else Mk.bg)) {
                                val clips = state.project.timeline.clips.filter { it.track == track }.sortedBy { it.startTimeUs }
                                var cursor = 0L
                                clips.forEach { c ->
                                    val gap = c.startTimeUs - cursor
                                    if (gap > 10_000) Spacer(Modifier.width((gap*pxPerUs).dp))
                                    ClipStrip(c, width = (c.durationUs*pxPerUs).coerceAtLeast(40f), selected = state.selectedClipId == c.id) { vm.selectClip(c.id) }
                                    cursor = c.endTimeUs
                                }
                            }
                            androidx.compose.foundation.layout.Divider(color = Mk.line, thickness = 0.5.dp)
                        }
                    }
                }
                val ratio = state.currentPlayheadUs / total.toFloat()
                Canvas(Modifier.offset(x = (ratio * widthPx).dp).width(2.dp).fillMaxHeight()) {
                    drawRect(Mk.red)
                    // triangle top
                    drawCircle(Mk.red, radius = 5f, center = Offset(size.width/2, 4f))
                }
            }
        }
    }
}

@Composable
private fun ClipStrip(clip: VideoClip, width: Float, selected: Boolean, onClick: () -> Unit) {
    val (bg, ic) = when(clip.type) {
        ClipType.VIDEO -> Brush.linearGradient(listOf(Mk.purple600, Mk.purple)) to Icons.Default.Videocam
        ClipType.IMAGE -> Brush.linearGradient(listOf(Mk.cyan, Mk.purple)) to Icons.Default.Image
        ClipType.TEXT -> Brush.linearGradient(listOf(Mk.pink, Mk.purple)) to Icons.Default.Title
        else -> SolidColor(Mk.bg3) to Icons.Default.AttachFile
    }
    val label = when(clip.type) { ClipType.TEXT -> clip.textContent.take(8); ClipType.VIDEO -> "VIDEO"; ClipType.IMAGE -> "IMG"; else -> "" }
    Box(Modifier.width(width.dp).fillMaxHeight().padding(horizontal=2.dp, vertical=4.dp)
        .clip(RoundedCornerShape(6.dp)).background(bg)
        .border(if(selected) 2.dp else 1.dp, if(selected) Color.White else Color.Black.copy(alpha=0.3f), RoundedCornerShape(6.dp))
        .clickable { onClick() },
        contentAlignment = Alignment.CenterStart) {
        Row(Modifier.padding(horizontal=6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(ic, null, tint = Color.White.copy(alpha=0.95f), modifier = Modifier.size(13.dp))
            if (width > 70) {
                Spacer(Modifier.width(3.dp))
                Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Clip)
            }
        }
        Canvas(Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter)) {
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha=0.3f), Color.Transparent)))
        }
    }
}

// ---------------- Inspectors ----------------
@Composable
private fun EffectsInspector(vm: EditorViewModel, state: EditorUiState, vip: Boolean, onVip: () -> Unit) {
    // categories
    var cat by remember { mutableStateOf("JJ") }
    val cats = listOf("JJ" to JedagJedugPresets.ALL_EFFECTS.filter { it.category == EffectCategory.JJEDAG_JEDUG },
        "Filter" to JedagJedugPresets.ALL_EFFECTS.filter { it.category == EffectCategory.FILTER },
        "Cine3D" to List(4) { JedagJedugPresets.ALL_EFFECTS[it % JedagJedugPresets.ALL_EFFECTS.size] },
        "Morph" to List(3) { JedagJedugPresets.ALL_EFFECTS[it] },
        "Graph" to List(3) { JedagJedugPresets.ALL_EFFECTS[it] }
    )
    val current = cats.first { it.first == cat }.second
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("JJ", "Filter", "Cine3D", "Morph", "Graph", "Blur").forEach { c ->
                MkChip(c, selected = c == cat) {
                    val proCat = c in listOf("Cine3D", "Morph", "Graph")
                    if (proCat && !vip) onVip() else cat = c
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(current) { fx ->
                EffectTile(fx.name, Color(fx.previewColor), Color(fx.previewColor).copy(alpha = 0.5f),
                    pro = cat in listOf("Cine3D", "Morph", "Graph") && !vip) {
                    if (cat in listOf("Cine3D","Morph","Graph") && !vip) onVip() else vm.applyJedagJedug(fx.id)
                }
            }
        }
    }
}

@Composable
private fun ColorInspector(vm: EditorViewModel, state: EditorUiState) {
    val c = state.selectedClipId?.let { state.project.timeline.getClipById(it) }
    if (c == null) { EmptyPanel("Pilih klip untuk atur warna"); return }
    Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        MkSlider(c.brightness * 100, { vm.adjustColor(brightness = it / 100f) }, -100f..100f, "Brightness")
        MkSlider((c.contrast - 1f) * 100, { vm.adjustColor(contrast = 1f + it/100f) }, -100f..100f, "Contrast")
        MkSlider((c.saturation - 1f) * 100, { vm.adjustColor(saturation = 1f + it/100f) }, -100f..200f, "Saturation")
    }
}

@Composable
private fun SpeedInspector(vm: EditorViewModel, state: EditorUiState) {
    val c = state.selectedClipId?.let { state.project.timeline.getClipById(it) }
    if (c == null) { EmptyPanel("Pilih klip video untuk atur kecepatan"); return }
    Column(Modifier.padding(horizontal = 14.dp)) {
        Row {
            Text("Kecepatan", color = Mk.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(String.format("%.2fx", c.speed), color = Mk.purple300, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(c.speed, { vm.setClipSpeed(it) }, valueRange = 0.1f..4f, steps = 38,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Mk.purple, inactiveTrackColor = Mk.bg3))
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 3f)) { s ->
                MkChip("${s}x", selected = Math.abs(c.speed - s) < 0.05f) { vm.setClipSpeed(s) }
            }
        }
    }
}

@Composable
private fun TextInspector(onAdd: (String) -> Unit) {
    Column(Modifier.padding(horizontal=14.dp)) {
        Text("Tambah layer teks di posisi playhead", color = Mk.dim, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        var t by remember { mutableStateOf("") }
        OutlinedTextField(value = t, onValueChange = { t = it }, label = { Text("Teks") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Mk.purple, cursorColor = Mk.purple))
        Spacer(Modifier.height(8.dp))
        MkButton("Tambah Teks", icon = Icons.Default.Add, modifier = Modifier.fillMaxWidth()) {
            if (t.isNotBlank()) { onAdd(t); t = "" }
        }
    }
}

@Composable
private fun ExportInspector(vm: EditorViewModel, state: EditorUiState) {
    val ex = state.project.exportSettings
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
        Text("${state.project.timeline.clips.size} klip • ${fmt(state.totalDurationUs)}", color = Mk.dim, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Text("Resolusi", color = Mk.dim, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(720 to 1280, 1080 to 1920, 1440 to 2560, 2160 to 3840)) { (w, h) ->
                MkChip("${h}p", selected = ex.width == w) { vm.updateExportSettings(width = w, height = h) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Frame Rate", color = Mk.dim, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(24, 30, 60)) { f ->
                MkChip("${f}fps", selected = ex.fps == f) { vm.updateExportSettings(fps = f) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bitrate", color = Mk.dim, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Text("${ex.bitrate / 1_000_000} Mbps", color = Mk.text, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Slider(ex.bitrate.toFloat(), { vm.updateExportSettings(bitrate = it.toLong()) }, valueRange = 4_000_000f..50_000_000f,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Mk.purple, inactiveTrackColor = Mk.bg3))
        Spacer(Modifier.height(8.dp))
        MkButton(if (state.isExporting) "Exporting..." else "Mulai Export", icon = Icons.Default.FileUpload, modifier = Modifier.fillMaxWidth(), enabled = !state.isExporting) {
            vm.exportVideo()
        }
    }
}

@Composable
private fun ComingPanel(msg: String, cta: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Mk.purple.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, null, tint = Mk.purple300, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(msg, color = Mk.dim, fontSize = 12.sp, textAlign = TextAlign.Center)
            if (cta != null) {
                Spacer(Modifier.height(10.dp))
                MkButton("Upgrade VIP", icon = Icons.Default.WorkspacePremium, onClick = cta)
            }
        }
    }
}

@Composable
private fun EmptyPanel(msg: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(msg, color = Mk.dim, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

// --------------- Dialogs / sheets ---------------
@Composable
private fun AddSheet(onDismiss: () -> Unit, onVideo: () -> Unit, onImage: () -> Unit, onText: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Mk.bg2, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Tambah ke timeline", color = Mk.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(14.dp))
                SheetRow(Icons.Default.Videocam, "Video", "Import dari galeri", Brush.linearGradient(listOf(Mk.purple, Mk.pink)), onVideo)
                SheetRow(Icons.Default.Image, "Gambar", "Foto atau frame", Brush.linearGradient(listOf(Mk.cyan, Mk.purple)), onImage)
                SheetRow(Icons.Default.Title, "Teks", "Overlay teks / lirik", Brush.linearGradient(listOf(Mk.pink, Mk.gold)), onText)
            }
        }
    }
}

@Composable
private fun SheetRow(icon: ImageVector, title: String, sub: String, brush: Brush, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(brush), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Mk.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = Mk.dim, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Mk.dim, Modifier.size(18.dp))
    }
}

@Composable
private fun TextDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var t by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Mk.bg2) {
            Column(Modifier.padding(18.dp)) {
                Text("Tambah teks", color = Mk.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = t, onValueChange = { t = it }, label = { Text("Isi teks") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Mk.purple, cursorColor = Mk.purple))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Batal", color = Mk.dim) }
                    Spacer(Modifier.width(6.dp))
                    MkButton("Tambah", modifier = Modifier.width(120.dp)) { if (t.isNotBlank()) onConfirm(t) }
                }
            }
        }
    }
}

@Composable
private fun ExportProg(p: Float) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.78f)), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(20.dp), color = Mk.bg2, modifier = Modifier.width(280.dp)) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(Mk.bg3, radius = size.minDimension/2f)
                        drawArc(Brush.linearGradient(listOf(Mk.purple, Mk.pink)), startAngle = -90f, sweepAngle = p * 360f,
                            useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
                    }
                    Text("${(p*100).toInt()}%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text("Sedang export...", color = Mk.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text("Jangan keluar app ya", color = Mk.dim, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun InfoDialog(title: String, body: String, onOk: () -> Unit) {
    Dialog(onDismissRequest = onOk) {
        Surface(shape = RoundedCornerShape(18.dp), color = Mk.bg2) {
            Column(Modifier.padding(20.dp)) {
                Text(title, color = Mk.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Text(body, color = Mk.dim, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    MkButton("OK", modifier = Modifier.width(100.dp), onClick = onOk)
                }
            }
        }
    }
}

private fun fmt(us: Long): String {
    val s = us / 1_000_000
    val m = s / 60
    val sec = s % 60
    val cs = (us % 1_000_000) / 10_000
    return "%02d:%02d.%02d".format(m, sec, cs)
}
