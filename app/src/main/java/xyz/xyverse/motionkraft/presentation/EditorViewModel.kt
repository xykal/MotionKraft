package xyz.xyverse.motionkraft.presentation

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import xyz.xyverse.motionkraft.MotionKraftApp
import xyz.xyverse.motionkraft.data.model.*
import xyz.xyverse.motionkraft.editor.FFmpegCommandBuilder
import xyz.xyverse.motionkraft.effects.JedagJedugApplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
    val zoomLevel: Float = 1f,
    val bottomPanel: BottomPanel = BottomPanel.NONE,
    val exportOutputPath: String? = null,
    val lastError: String? = null,
    val totalDurationUs: Long = 0L,
    val statusMessage: String? = null
)

enum class BottomPanel {
    NONE, EFFECTS, COLOR, TRANSFORM, TEXT, TRANSITION, AUDIO, SPEED, EXPORT
}

class EditorViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState

    private val _isVip = MutableStateFlow(false)
    val isVip: StateFlow<Boolean> = _isVip

    val username: String = "MotionKraft User"

    fun setVip(v: Boolean) { _isVip.value = v }

    private var playbackJob: Job? = null

    init {
        pollPendingImport()
    }

    private fun pollPendingImport() {
        viewModelScope.launch {
            while (isActive) {
                val pending = getApplication<MotionKraftApp>().pendingImport
                if (pending != null) {
                    getApplication<MotionKraftApp>().pendingImport = null
                    if (pending.isVideo) importVideo(pending.path, pending.durationUs)
                    else importImage(pending.path)
                }
                delay(300)
            }
        }
    }

    fun importVideo(filePath: String, durationUs: Long) {
        val timeline = _uiState.value.project.timeline
        val lastEnd = timeline.clips.filter { it.type == ClipType.VIDEO || it.type == ClipType.IMAGE }
            .maxOfOrNull { it.endTimeUs } ?: 0L
        val track = if (timeline.clips.isEmpty()) 0 else (timeline.clips.maxOf { it.track } + 1)
        val clip = VideoClip(
            type = ClipType.VIDEO,
            filePath = filePath,
            startTimeUs = lastEnd,
            endTimeUs = lastEnd + durationUs,
            track = track
        )
        timeline.addClip(clip)
        recalcDuration()
        _uiState.update {
            it.copy(selectedClipId = clip.id, statusMessage = "Video added (${durationUs / 1_000_000}s)")
        }
        clearStatusAfterDelay()
    }

    fun importImage(filePath: String) {
        val timeline = _uiState.value.project.timeline
        val lastEnd = timeline.clips.maxOfOrNull { it.endTimeUs } ?: 0L
        val duration = 3_000_000L
        val track = if (timeline.clips.isEmpty()) 0 else (timeline.clips.maxOf { it.track } + 1)
        val clip = VideoClip(
            type = ClipType.IMAGE,
            filePath = filePath,
            startTimeUs = lastEnd,
            endTimeUs = lastEnd + duration,
            track = track
        )
        timeline.addClip(clip)
        recalcDuration()
        _uiState.update { it.copy(selectedClipId = clip.id, statusMessage = "Image added") }
        clearStatusAfterDelay()
    }

    fun addTextLayer(text: String) {
        val timeline = _uiState.value.project.timeline
        val playhead = _uiState.value.currentPlayheadUs
        val duration = 3_000_000L
        val track = (timeline.clips.maxOfOrNull { it.track } ?: -1) + 1
        val clip = VideoClip(
            type = ClipType.TEXT,
            startTimeUs = playhead,
            endTimeUs = playhead + duration,
            track = track,
            textContent = text,
            fontSize = 64f
        )
        timeline.addClip(clip)
        recalcDuration()
        _uiState.update { it.copy(selectedClipId = clip.id, bottomPanel = BottomPanel.NONE, statusMessage = "Text added") }
        clearStatusAfterDelay()
    }

    fun updatePlayhead(timeUs: Long) {
        val total = _uiState.value.totalDurationUs.coerceAtLeast(1000L)
        _uiState.update { it.copy(currentPlayheadUs = timeUs.coerceIn(0, total)) }
    }

    fun togglePlay() {
        val playing = !_uiState.value.isPlaying
        _uiState.update { it.copy(isPlaying = playing) }
        if (playing) startPlayback() else stopPlayback()
    }

    private fun startPlayback() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val start = _uiState.value.currentPlayheadUs
            val startWall = System.currentTimeMillis()
            while (isActive && _uiState.value.isPlaying) {
                val elapsed = (System.currentTimeMillis() - startWall) * 1000L
                var pos = (start + elapsed)
                val total = _uiState.value.totalDurationUs
                if (pos >= total) {
                    pos = 0
                    _uiState.update { it.copy(currentPlayheadUs = 0) }
                    break
                }
                _uiState.update { it.copy(currentPlayheadUs = pos) }
                delay(33) // ~30fps UI update
            }
        }
    }

    private fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
    }

    fun selectClip(clipId: String?) = _uiState.update { it.copy(selectedClipId = clipId) }

    fun setBottomPanel(panel: BottomPanel) {
        _uiState.update {
            it.copy(bottomPanel = if (it.bottomPanel == panel) BottomPanel.NONE else panel)
        }
    }

    fun applyJedagJedug(presetId: String, bpm: Float = 120f, intensity: Float = 0.7f) {
        val state = _uiState.value
        val clip = state.selectedClipId?.let { state.project.timeline.getClipById(it) } ?: return
        JedagJedugApplier.applyPreset(clip, presetId, bpm, intensity)
        _uiState.update { it.copy(project = it.project.copy(modifiedAt = System.currentTimeMillis()),
            statusMessage = "Effect applied") }
        clearStatusAfterDelay()
    }

    fun deleteSelectedClip() {
        val id = _uiState.value.selectedClipId ?: return
        _uiState.value.project.timeline.removeClip(id)
        recalcDuration()
        _uiState.update { it.copy(selectedClipId = null, statusMessage = "Clip deleted") }
        clearStatusAfterDelay()
    }

    fun setClipSpeed(speed: Float) {
        val state = _uiState.value
        val clip = state.selectedClipId?.let { state.project.timeline.getClipById(it) } ?: return
        val newDuration = (clip.durationUs * clip.speed / speed).toLong()
        state.project.timeline.clips.remove(clip)
        state.project.timeline.clips.add(clip.copy(speed = speed, endTimeUs = clip.startTimeUs + newDuration))
        recalcDuration()
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

    fun applyZoom(factor: Float) {
        val current = _uiState.value.zoomLevel
        val newZoom = (current * factor).coerceIn(0.25f, 8f)
        _uiState.update { it.copy(zoomLevel = newZoom) }
    }

    fun moveClipStart(clipId: String, deltaUs: Long) {
        val clip = _uiState.value.project.timeline.getClipById(clipId) ?: return
        val ns = (clip.startTimeUs + deltaUs).coerceAtLeast(0)
        val dur = clip.durationUs
        _uiState.value.project.timeline.clips.remove(clip)
        _uiState.value.project.timeline.clips.add(clip.copy(startTimeUs = ns, endTimeUs = ns + dur))
        recalcDuration()
        _uiState.update { it.copy(project = it.project.copy(modifiedAt = System.currentTimeMillis())) }
    }

    fun splitAtPlayhead() {
        val state = _uiState.value
        val ph = state.currentPlayheadUs
        val clip = state.project.timeline.clips
            .filter { ph in it.startTimeUs until it.endTimeUs }
            .maxByOrNull { it.track } ?: return
        val cutPoint = ph
        val oldEnd = clip.endTimeUs
        state.project.timeline.clips.remove(clip)
        state.project.timeline.clips.add(clip.copy(endTimeUs = cutPoint))
        state.project.timeline.clips.add(clip.copy(
            startTimeUs = cutPoint,
            endTimeUs = oldEnd,
            trimStartUs = clip.trimStartUs + (cutPoint - clip.startTimeUs)
        ))
        recalcDuration()
        _uiState.update { it.copy(statusMessage = "Split at playhead") }
        clearStatusAfterDelay()
    }

    fun exportVideo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportProgress = 0f, lastError = null) }
            val project = _uiState.value.project
            val ctx = getApplication<Application>()

            val moviesDir = File(ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "MotionKraft")
            if (!moviesDir.exists()) moviesDir.mkdirs()
            val outFile = File(moviesDir, "${project.name.replace(" ","_")}_${System.currentTimeMillis()}.mp4")

            val cmd = FFmpegCommandBuilder.buildCommand(project, outFile.absolutePath)
            val cmdStr = cmd.joinToString(" ")

            val result = withContext(Dispatchers.IO) {
                kotlinx.coroutines.suspendCancellableCoroutine<ReturnCode?> { cont ->
                    val session = FFmpegKit.executeAsync(cmdStr, { session ->
                        cont.resume(session.returnCode, {})
                    }, { }, { statistics ->
                        val time = statistics.time.toFloat()
                        val total = project.timeline.getTotalDuration() / 1000f
                        if (total > 0f) {
                            _uiState.update { it.copy(exportProgress = (time / total).coerceIn(0f, 1f)) }
                        }
                    })
                    cont.invokeOnCancellation { session?.cancel() }
                }
            }

            if (result != null && result.isValueSuccess) {
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, outFile.name)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.DATA, outFile.absolutePath)
                        put(MediaStore.Video.Media.DURATION, project.timeline.getTotalDuration() / 1000)
                    }
                    ctx.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                } catch (_: Exception) { }
                _uiState.update {
                    it.copy(isExporting = false, exportProgress = 1f, exportOutputPath = outFile.absolutePath)
                }
            } else {
                _uiState.update {
                    it.copy(isExporting = false, lastError = "Export failed (code ${result?.toString()}).")
                }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(lastError = null) }
    fun dismissExportResult() = _uiState.update { it.copy(exportOutputPath = null) }

    private fun recalcDuration() {
        val total = _uiState.value.project.timeline.getTotalDuration().coerceAtLeast(2_000_000L)
        _uiState.update { it.copy(totalDurationUs = total,
            project = it.project.copy(timeline = it.project.timeline.also { t -> t.durationUs = total })
        )}
    }

    private fun clearStatusAfterDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            _uiState.update { it.copy(statusMessage = null) }
        }, 1800)
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
}
