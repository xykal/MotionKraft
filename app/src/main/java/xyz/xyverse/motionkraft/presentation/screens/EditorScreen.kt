package xyz.xyverse.motionkraft.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.xyverse.motionkraft.R
import xyz.xyverse.motionkraft.presentation.EditorViewModel
import xyz.xyverse.motionkraft.presentation.components.Inspector
import xyz.xyverse.motionkraft.presentation.components.PreviewPanel
import xyz.xyverse.motionkraft.presentation.components.TimelineView
import xyz.xyverse.motionkraft.presentation.components.ToolButton
import xyz.xyverse.motionkraft.presentation.components.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: EditorViewModel, projectId: String, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val export by vm.export.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showText by remember { mutableStateOf(false) }
    var showInspector by remember { mutableStateOf(false) }

    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importMedia(uri, video = true)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importMedia(uri, video = false)
    }

    LaunchedEffect(projectId) { vm.open(projectId) }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }
    LaunchedEffect(export.resultName) {
        export.resultName?.let {
            snackbar.showSnackbar(context.getString(R.string.export_saved, it))
            vm.acknowledgeExport()
        }
    }
    LaunchedEffect(export.error) {
        export.error?.let {
            snackbar.showSnackbar(it)
            vm.acknowledgeExport()
        }
    }

    val project = state.project
    val canvasW = project.exportSettings.width
    val canvasH = project.exportSettings.height
    val selected = state.selectedId?.let { project.timeline.clip(it) }
    val active = project.timeline.clips
        .filter { state.playheadUs >= it.startTimeUs && state.playheadUs < it.endTimeUs }
        .sortedBy { it.track }
    val untitled = stringResource(R.string.untitled)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = project.name.ifBlank { untitled },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.flush()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { vm.export() }, enabled = !export.running) {
                        Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.export))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (export.running) {
                LinearProgressIndicator(progress = export.progress, modifier = Modifier.fillMaxWidth())
            }

            PreviewPanel(
                canvasWidth = canvasW,
                canvasHeight = canvasH,
                playheadUs = state.playheadUs,
                clips = active,
                modifier = Modifier.weight(1f)
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatTime(state.playheadUs)} / ${formatTime(project.timeline.durationUs)}",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.setZoom(state.pxPerSecond - 20f) }) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.zoom_out))
                }
                IconButton(onClick = { vm.setZoom(state.pxPerSecond + 20f) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.zoom_in))
                }
                FilledIconButton(onClick = { vm.togglePlay() }) {
                    Icon(
                        imageVector = if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(if (state.playing) R.string.pause else R.string.play)
                    )
                }
            }

            Box(Modifier.fillMaxWidth().height(150.dp).verticalScroll(rememberScrollState())) {
                TimelineView(
                    clips = project.timeline.clips,
                    durationUs = project.timeline.durationUs,
                    playheadUs = state.playheadUs,
                    selectedId = state.selectedId,
                    pxPerSecond = state.pxPerSecond,
                    onSeek = { vm.seek(it) },
                    onSelect = { vm.select(it) },
                    onMove = { id, delta -> vm.moveClip(id, delta) }
                )
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ToolButton(
                    icon = Icons.Default.VideoLibrary,
                    label = stringResource(R.string.tool_video),
                    onClick = {
                        pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }
                )
                ToolButton(
                    icon = Icons.Default.Image,
                    label = stringResource(R.string.tool_photo),
                    onClick = {
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
                ToolButton(
                    icon = Icons.Default.TextFields,
                    label = stringResource(R.string.tool_text),
                    onClick = { showText = true }
                )
                ToolButton(
                    icon = Icons.Default.ContentCut,
                    label = stringResource(R.string.tool_split),
                    onClick = { vm.split() },
                    enabled = selected != null
                )
                ToolButton(
                    icon = Icons.Default.ContentCopy,
                    label = stringResource(R.string.tool_duplicate),
                    onClick = { vm.duplicate() },
                    enabled = selected != null
                )
                ToolButton(
                    icon = Icons.Default.Tune,
                    label = stringResource(R.string.tool_adjust),
                    onClick = { showInspector = true },
                    enabled = selected != null
                )
                ToolButton(
                    icon = Icons.Default.Delete,
                    label = stringResource(R.string.tool_delete),
                    onClick = {
                        vm.deleteSelected()
                        showInspector = false
                    },
                    enabled = selected != null
                )
            }
        }
    }

    if (showInspector && selected != null) {
        ModalBottomSheet(onDismissRequest = { showInspector = false }) {
            Column(Modifier.fillMaxWidth().height(460.dp)) {
                Inspector(vm = vm, state = state, clip = selected, canvasWidth = canvasW, canvasHeight = canvasH)
            }
        }
    }

    if (showText) {
        var draft by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showText = false },
            title = { Text(stringResource(R.string.add_text)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(R.string.text_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addText(draft)
                    showText = false
                }) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { showText = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
