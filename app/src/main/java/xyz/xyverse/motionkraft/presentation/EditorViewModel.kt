package xyz.xyverse.motionkraft.presentation

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import xyz.xyverse.motionkraft.data.model.*
import xyz.xyverse.motionkraft.editor.FFmpegCommandBuilder
import xyz.xyverse.motionkraft.effects.JedagJedugApplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class EditorUiState(
    val project: MographProject = MographProject(name = "Project ${System.currentTimeMillis() / 1000}"),
    val selectedClipId: String? = null,
    val currentPlayheadUs: Long = 0L,
    val isPlaying: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val zoomLevel: Float = 1f, // Timeline zoom
    val bottomPanel: BottomPanel = BottomPanel.EFFECTS,
    val showTextEditor: Boolean = false,
    val exportOutputUri: Uri? = null,
    val lastError: String? = null
)

enum class BottomPanel {
    NONE, EFFECTS, COLOR, TRANSFORM, TEXT, TRANSITION, AUDIO, SPEED, EXPORT
}

class EditorViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState

    fun importVideo(filePath: String, durationUs: Long) {
        val timeline = _uiState.value.project.timeline
        val lastEnd = timeline.clips.maxOfOrNull { it.endTimeUs } ?: 0L
        val clip = VideoClip(
            type = ClipType.VIDEO,
            filePath = filePath,
            startTimeUs = lastEnd,
            endTimeUs = lastEnd + durationUs,
            track = if (timeline.clips.isEmpty()) 0 else timeline.clips.maxOf { it.track } + 1
        )
        timeline.addClip(clip)
        _uiState.update { it.copy(selectedClipId = clip.id) }
    }

    fun importImage(filePath: String) {
        val timeline = _uiState.value.project.timeline
        val lastEnd = timeline.clips.maxOfOrNull { it.endTimeUs } ?: 0L
        val duration = 3_000_000L // 3 detik default gambar
        val clip = VideoClip(
            type = ClipType.IMAGE,
            filePath = filePath,
            startTimeUs = lastEnd,
            endTimeUs = lastEnd + duration,
            track = if (timeline.clips.isEmpty()) 0 else timeline.clips.maxOf { it.track } + 1
        )
        timeline.addClip(clip)
        _uiState.update { it.copy(selectedClipId = clip.id) }
    }

    fun addTextLayer(text: String) {
        val timeline = _uiState.value.project.timeline
        val playhead = _uiState.value.currentPlayheadUs
        val duration = 3_000_000L
        val clip = VideoClip(
            type = ClipType.TEXT,
            startTimeUs = playhead,
            endTimeUs = playhead + duration,
            track = (timeline.clips.maxOfOrNull { it.track } ?: -1) + 1,
            textContent = text,
            fontSize = 64f
        )
        timeline.addClip(clip)
        _uiState.update { it.copy(selectedClipId = clip.id, showTextEditor = false) }
    }

    fun updatePlayhead(timeUs: Long) {
        _uiState.update { it.copy(currentPlayheadUs = timeUs.coerceIn(0, it.project.timeline.getTotalDuration())) }
    }

    fun togglePlay() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun selectClip(clipId: String?) {
        _uiState.update { it.copy(selectedClipId = clipId) }
    }

    fun setBottomPanel(panel: BottomPanel) {
        _uiState.update {
            it.copy(bottomPanel = if (it.bottomPanel == panel) BottomPanel.NONE else panel)
        }
    }

    fun applyJedagJedug(presetId: String, bpm: Float = 120f, intensity: Float = 0.7f) {
        val state = _uiState.value
        val clip = state.selectedClipId?.let { state.project.timeline.getClipById(it) } ?: return
        JedagJedugApplier.applyPreset(clip, presetId, bpm, intensity)
        _uiState.update { it.copy(project = it.project.copy(modifiedAt = System.currentTimeMillis())) }
    }

    fun deleteSelectedClip() {
        val state = _uiState.value
        state.selectedClipId?.let { state.project.timeline.removeClip(it) }
        _uiState.update { it.copy(selectedClipId = null, project = it.project.copy(modifiedAt = System.currentTimeMillis())) }
    }

    fun setClipSpeed(speed: Float) {
        val state = _uiState.value
        val clip = state.selectedClipId?.let { state.project.timeline.getClipById(it) } ?: return
        val newDuration = (clip.durationUs / speed * clip.speed).toLong()
        val updated = clip.copy(speed = speed, endTimeUs = clip.startTimeUs + newDuration)
        state.project.timeline.clips.remove(clip)
        state.project.timeline.clips.add(updated)
        _uiState.update { it.copy(project = it.project.copy(modifiedAt = System.currentTimeMillis())) }
    }

    fun adjustColor(brightness: Float? = null, contrast: Float? = null, saturation: Float? = null) {
        val state = _uiState.value
        val clip = state.selectedClipId?.let { state.project.timeline.getClipById(it) } ?: return
        state.project.timeline.clips.remove(clip)
        state.project.timeline.clips.add(clip.copy(
            brightness = brightness ?: clip.brightness,
            contrast = contrast ?: clip.contrast,
            saturation = saturation ?: clip.saturation
        ))
        _uiState.update { it.copy(project = it.project.copy(modifiedAt = System.currentTimeMillis())) }
    }

    fun updateExportSettings(
        width: Int? = null, height: Int? = null, fps: Int? = null,
        bitrate: Long? = null, qualityPreset: String? = null
    ) {
        val proj = _uiState.value.project
        val ex = proj.exportSettings
        _uiState.update {
            it.copy(project = proj.copy(
                exportSettings = ex.copy(
                    width = width ?: ex.width,
                    height = height ?: ex.height,
                    fps = fps ?: ex.fps,
                    bitrate = bitrate ?: ex.bitrate,
                    qualityPreset = qualityPreset ?: ex.qualityPreset
                ),
                modifiedAt = System.currentTimeMillis()
            ))
        }
    }

    fun exportVideo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportProgress = 0f, lastError = null) }

            val project = _uiState.value.project
            val ctx = getApplication<Application>()

            // Buat file output di direktori Movies/MotionKraft
            val moviesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "MotionKraft"
            )
            if (!moviesDir.exists()) moviesDir.mkdirs()
            val outFile = File(moviesDir, "${project.name}_${System.currentTimeMillis()}.mp4")

            val cmd = FFmpegCommandBuilder.buildCommand(project, outFile.absolutePath)

            val result = withContext(Dispatchers.IO) {
                kotlinx.coroutines.suspendCancellableCoroutine<ReturnCode?> { cont ->
                    val session = FFmpegKit.executeAsync(cmd.joinToString(" "), { session ->
                        cont.resume(session.returnCode, {})
                    }, { log ->
                        // Bisa parse log untuk progress (time=...)
                    }, { statistics ->
                        val time = statistics.time
                        val total = project.timeline.getTotalDuration() / 1000f
                        if (total > 0) {
                            _uiState.update { it.copy(exportProgress = (time / total).coerceIn(0f, 1f)) }
                        }
                    })
                    cont.invokeOnCancellation { session?.cancel() }
                }
            }

            if (result != null && result.isValueSuccess) {
                // Tambahkan ke MediaStore supaya muncul di galeri
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, outFile.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.DATA, outFile.absolutePath)
                    put(MediaStore.Video.Media.DURATION, project.timeline.getTotalDuration() / 1000)
                }
                val uri = ctx.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                _uiState.update {
                    it.copy(isExporting = false, exportProgress = 1f, exportOutputUri = uri)
                }
            } else {
                _uiState.update {
                    it.copy(isExporting = false, lastError = "Export gagal, coba lagi dengan preset berbeda")
                }
            }
        }
    }

    fun undoError() {
        _uiState.update { it.copy(lastError = null) }
    }

    fun setZoom(value: Float) {
        _uiState.update { it.copy(zoomLevel = value) }
    }
}
