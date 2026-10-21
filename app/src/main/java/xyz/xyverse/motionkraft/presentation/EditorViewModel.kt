package xyz.xyverse.motionkraft.presentation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.xyverse.motionkraft.R
import xyz.xyverse.motionkraft.data.ProjectStore
import xyz.xyverse.motionkraft.data.model.*
import xyz.xyverse.motionkraft.editor.ExportService
import xyz.xyverse.motionkraft.editor.ExportState
import xyz.xyverse.motionkraft.editor.FFmpegCommandBuilder
import xyz.xyverse.motionkraft.editor.MediaImporter
import xyz.xyverse.motionkraft.effects.JedagJedugApplier
import java.io.File
import java.util.UUID

data class EditorState(
    val project: MographProject = MographProject(),
    val selectedId: String? = null,
    val playheadUs: Long = 0L,
    val playing: Boolean = false,
    val pxPerSecond: Float = 80f,
    val tab: Int = 0,
    val motionBpm: Float = 120f,
    val motionIntensity: Float = 0.7f,
    val message: String? = null
)

/** Owns the open project. All edits go through [mutate] so every change is saved. */
class EditorViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ProjectStore(app)
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()
    val export: StateFlow<ExportState> = ExportService.state

    private var saveJob: Job? = null
    private var playJob: Job? = null

    // ---- project lifecycle ----

    fun createProject(width: Int, height: Int): String {
        val project = MographProject(exportSettings = ExportSettings(width = width, height = height))
        _state.value = EditorState(project = project)
        scheduleSave()
        return project.id
    }

    fun open(id: String) {
        if (_state.value.project.id == id) return
        flush()
        io.launch {
            val loaded = store.load(id) ?: return@launch
            _state.value = EditorState(project = loaded)
        }
    }

    /** Writes the current project now, not after the debounce delay. */
    fun flush() {
        saveJob?.cancel()
        saveJob = null
        val snapshot = _state.value.project
        io.launch { store.save(snapshot) }
    }

    override fun onCleared() {
        playJob?.cancel()
        flush()
        super.onCleared()
    }

    private fun scheduleSave() {
        val snapshot = _state.value.project
        saveJob?.cancel()
        saveJob = io.launch {
            delay(400)
            store.save(snapshot)
        }
    }

    private fun mutate(block: (Timeline) -> Timeline) {
        _state.update { s ->
            s.copy(project = s.project.copy(timeline = block(s.project.timeline), modifiedAt = System.currentTimeMillis()))
        }
        scheduleSave()
    }

    private fun mutateClip(id: String, block: (VideoClip) -> VideoClip) {
        mutate { t -> t.clip(id)?.let { t.replace(block(it)) } ?: t }
    }

    private fun selectedClip(): VideoClip? {
        val s = _state.value
        return s.selectedId?.let { s.project.timeline.clip(it) }
    }

    private fun updateSelected(block: (VideoClip) -> VideoClip) {
        val id = _state.value.selectedId ?: return
        mutateClip(id, block)
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun say(text: String) = _state.update { it.copy(message = text) }

    // ---- media and text ----

    fun importMedia(uri: Uri, video: Boolean) {
        io.launch {
            val media = MediaImporter.import(getApplication(), uri, video)
            if (media == null) {
                say(getApplication<Application>().getString(R.string.import_failed))
                return@launch
            }
            val clip = VideoClip(
                type = if (media.isVideo) ClipType.VIDEO else ClipType.IMAGE,
                filePath = media.path,
                hasAudio = media.hasAudio,
                startTimeUs = _state.value.project.timeline.endOfTrack(0),
                lengthUs = media.durationUs,
                track = 0
            )
            mutate { it.add(clip) }
            _state.update { it.copy(selectedId = clip.id) }
        }
    }

    fun addText(text: String) {
        val s = _state.value
        val track = (s.project.timeline.clips.maxOfOrNull { it.track } ?: 0) + 1
        val clip = VideoClip(
            type = ClipType.TEXT,
            text = text.ifBlank { "Text" },
            startTimeUs = s.playheadUs,
            lengthUs = 3_000_000L,
            track = track
        )
        mutate { it.add(clip) }
        _state.update { it.copy(selectedId = clip.id) }
    }

    // ---- transport ----

    fun seek(timeUs: Long) {
        _state.update { it.copy(playheadUs = timeUs.coerceIn(0L, it.project.timeline.durationUs)) }
    }

    fun togglePlay() {
        if (_state.value.playing) stopPlayback() else startPlayback()
    }

    private fun startPlayback() {
        playJob?.cancel()
        _state.update { it.copy(playing = true) }
        playJob = viewModelScope.launch {
            var last = System.nanoTime()
            while (isActive) {
                delay(33)
                val now = System.nanoTime()
                val stepUs = (now - last) / 1_000L
                last = now
                val s = _state.value
                val next = s.playheadUs + stepUs
                if (next >= s.project.timeline.durationUs) {
                    _state.update { it.copy(playheadUs = 0L, playing = false) }
                    break
                }
                _state.update { it.copy(playheadUs = next) }
            }
        }
    }

    private fun stopPlayback() {
        playJob?.cancel()
        playJob = null
        _state.update { it.copy(playing = false) }
    }

    // ---- selection and view ----

    fun select(id: String?) = _state.update { it.copy(selectedId = id) }

    fun setTab(index: Int) = _state.update { it.copy(tab = index) }

    fun setZoom(pxPerSecond: Float) = _state.update { it.copy(pxPerSecond = pxPerSecond.coerceIn(30f, 240f)) }

    fun setMotionBpm(bpm: Float) = _state.update { it.copy(motionBpm = bpm) }

    fun setMotionIntensity(v: Float) = _state.update { it.copy(motionIntensity = v) }

    // ---- clip editing ----

    fun moveClip(id: String, deltaUs: Long) {
        mutateClip(id) { it.copy(startTimeUs = (it.startTimeUs + deltaUs).coerceAtLeast(0L)) }
    }

    fun setSpeed(speed: Float) {
        updateSelected { c ->
            val sourceUs = c.lengthUs * c.speed
            c.copy(speed = speed, lengthUs = (sourceUs / speed).toLong().coerceAtLeast(100_000L))
        }
    }

    fun split() {
        val c = selectedClip() ?: return
        val cut = _state.value.playheadUs
        if (cut <= c.startTimeUs + 100_000L || cut >= c.endTimeUs - 100_000L) {
            say(getApplication<Application>().getString(R.string.move_playhead))
            return
        }
        val leftUs = cut - c.startTimeUs
        val right = c.copy(
            id = UUID.randomUUID().toString(),
            startTimeUs = cut,
            lengthUs = c.endTimeUs - cut,
            sourceStartUs = c.sourceStartUs + (leftUs * c.speed).toLong(),
            transform = shiftTransform(c.transform, leftUs)
        )
        mutate { t -> t.replace(c.copy(lengthUs = leftUs)).add(right) }
        _state.update { it.copy(selectedId = right.id) }
    }

    fun duplicate() {
        val c = selectedClip() ?: return
        val copy = c.copy(id = UUID.randomUUID().toString(), startTimeUs = c.endTimeUs)
        mutate { it.add(copy) }
        _state.update { it.copy(selectedId = copy.id) }
    }

    fun deleteSelected() {
        val id = _state.value.selectedId ?: return
        mutate { it.remove(id) }
        _state.update { it.copy(selectedId = null) }
    }

    fun setTransform(property: TransformProperty, value: Float) {
        updateSelected { it.copy(transform = it.transform.withConstant(property, value)) }
    }

    fun setUniformScale(value: Float) {
        updateSelected {
            it.copy(transform = it.transform
                .withConstant(TransformProperty.SCALE_X, value)
                .withConstant(TransformProperty.SCALE_Y, value))
        }
    }

    fun setColor(brightness: Float? = null, contrast: Float? = null, saturation: Float? = null) {
        updateSelected {
            it.copy(
                brightness = brightness ?: it.brightness,
                contrast = contrast ?: it.contrast,
                saturation = saturation ?: it.saturation
            )
        }
    }

    fun setVolume(volume: Float) = updateSelected { it.copy(volume = volume) }

    fun toggleEffect(effectId: String) {
        updateSelected { c ->
            val present = c.effects.any { it.effectId == effectId }
            c.copy(effects = if (present) {
                c.effects.filterNot { it.effectId == effectId }
            } else {
                c.effects + EffectInstance(effectId)
            })
        }
    }

    fun setEffectParam(effectId: String, param: String, value: Float) {
        updateSelected { c ->
            c.copy(effects = c.effects.map {
                if (it.effectId == effectId) it.copy(params = it.params + (param to value)) else it
            })
        }
    }

    fun applyPreset(presetId: String) {
        val s = _state.value
        val c = selectedClip() ?: return
        val result = JedagJedugApplier.build(presetId, c.lengthUs, s.motionBpm, s.motionIntensity)
        mutateClip(c.id) { clip ->
            clip.copy(
                transform = result.transform,
                effects = clip.effects.filterNot { it.effectId == EffectIds.GLITCH } + result.effects
            )
        }
    }

    fun resetMotion() {
        updateSelected { clip ->
            clip.copy(
                transform = Transform(),
                effects = clip.effects.filterNot { it.effectId == EffectIds.GLITCH }
            )
        }
    }

    fun setText(text: String) = updateSelected { it.copy(text = text) }

    fun setFontSize(size: Float) = updateSelected { it.copy(fontSize = size) }

    fun setStrokeWidth(width: Float) = updateSelected { it.copy(strokeWidth = width) }

    fun setTextColor(argb: Int) = updateSelected { it.copy(textColor = argb) }

    // ---- export ----

    fun export() {
        if (ExportService.state.value.running) return
        val project = _state.value.project
        if (project.timeline.clips.isEmpty()) {
            say(getApplication<Application>().getString(R.string.need_clip))
            return
        }
        flush()
        io.launch {
            val app = getApplication<Application>()
            val stamp = System.currentTimeMillis()
            val output = File(app.cacheDir, "export_$stamp.mp4").absolutePath
            val args = FFmpegCommandBuilder.build(
                project = project,
                outputPath = output,
                textFileFor = { clip -> writeTextFile(clip) },
                fontPath = resolveFont()
            )
            ExportService.start(app, args, output, "MotionKraft_$stamp.mp4", project.timeline.durationUs)
        }
    }

    fun acknowledgeExport() = ExportService.acknowledge()

    private fun writeTextFile(clip: VideoClip): String {
        val file = File(getApplication<Application>().cacheDir, "text_${clip.id}.txt")
        file.writeText(clip.text)
        return file.absolutePath
    }

    private fun resolveFont(): String =
        listOf("/system/fonts/Roboto-Regular.ttf", "/system/fonts/DroidSans.ttf")
            .firstOrNull { File(it).exists() } ?: FFmpegCommandBuilder.DEFAULT_FONT

    private fun shiftTransform(t: Transform, by: Long): Transform = t.copy(
        positionX = shiftKeys(t.positionX, by),
        positionY = shiftKeys(t.positionY, by),
        scaleX = shiftKeys(t.scaleX, by),
        scaleY = shiftKeys(t.scaleY, by),
        rotation = shiftKeys(t.rotation, by),
        opacity = shiftKeys(t.opacity, by)
    )

    /** Moves keyframes so they stay in place relative to the clip start after a split. */
    private fun shiftKeys(keys: List<Keyframe>, by: Long): List<Keyframe> {
        val sampled = Transform.sample(keys, by)
        val kept = keys.filter { it.timeUs > by }
            .map { Keyframe(it.timeUs - by, it.value, it.interpolation) }
        return if (kept.isEmpty()) listOf(Keyframe(0L, sampled)) else listOf(Keyframe(0L, sampled)) + kept
    }
}
