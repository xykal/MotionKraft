package xyz.xyverse.motionkraft.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.xyverse.motionkraft.R
import xyz.xyverse.motionkraft.data.model.ClipType
import xyz.xyverse.motionkraft.data.model.EffectIds
import xyz.xyverse.motionkraft.data.model.Effects
import xyz.xyverse.motionkraft.data.model.JjPresets
import xyz.xyverse.motionkraft.data.model.TransformProperty
import xyz.xyverse.motionkraft.data.model.VideoClip
import xyz.xyverse.motionkraft.presentation.EditorState
import xyz.xyverse.motionkraft.presentation.EditorViewModel

enum class InspectorTab(val label: Int) {
    TRANSFORM(R.string.tab_transform),
    COLOR(R.string.tab_color),
    EFFECTS(R.string.tab_effects),
    MOTION(R.string.tab_motion),
    TEXT(R.string.tab_text)
}

/** Edits the selected clip. Every control writes through the ViewModel, which saves the project. */
@Composable
fun Inspector(
    vm: EditorViewModel,
    state: EditorState,
    clip: VideoClip,
    canvasWidth: Int,
    canvasHeight: Int
) {
    val tabs = if (clip.type == ClipType.TEXT) {
        listOf(InspectorTab.TRANSFORM, InspectorTab.TEXT)
    } else {
        listOf(InspectorTab.TRANSFORM, InspectorTab.COLOR, InspectorTab.EFFECTS, InspectorTab.MOTION)
    }
    val index = state.tab.coerceIn(0, tabs.lastIndex)
    Column(Modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = index) {
            tabs.forEachIndexed { i, tab ->
                Tab(
                    selected = i == index,
                    onClick = { vm.setTab(i) },
                    text = { Text(stringResource(tab.label)) }
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (tabs[index]) {
                InspectorTab.TRANSFORM -> TransformTab(vm, clip, canvasWidth, canvasHeight)
                InspectorTab.COLOR -> ColorTab(vm, clip)
                InspectorTab.EFFECTS -> EffectsTab(vm, clip)
                InspectorTab.MOTION -> MotionTab(vm, state)
                InspectorTab.TEXT -> TextTab(vm, clip)
            }
        }
    }
}

@Composable
private fun TransformTab(vm: EditorViewModel, clip: VideoClip, canvasWidth: Int, canvasHeight: Int) {
    val t = clip.transform
    if (clip.type != ClipType.TEXT) {
        SectionLabel(stringResource(R.string.speed))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0.5f, 1f, 1.5f, 2f).forEach { speed ->
                FilterChip(
                    selected = clip.speed == speed,
                    onClick = { vm.setSpeed(speed) },
                    label = { Text("${speed}x") }
                )
            }
        }
    }
    val scale = t.valueAt(TransformProperty.SCALE_X, 0L)
    LabeledSlider(stringResource(R.string.scale), scale, 0.2f..3f, "%.2fx".format(scale)) {
        vm.setUniformScale(it)
    }
    val rotation = t.valueAt(TransformProperty.ROTATION, 0L)
    LabeledSlider(stringResource(R.string.rotation), rotation, -180f..180f, "%.0f°".format(rotation)) {
        vm.setTransform(TransformProperty.ROTATION, it)
    }
    val opacity = t.valueAt(TransformProperty.OPACITY, 0L)
    LabeledSlider(stringResource(R.string.opacity), opacity, 0f..1f, "%d%%".format((opacity * 100).toInt())) {
        vm.setTransform(TransformProperty.OPACITY, it)
    }
    val px = t.valueAt(TransformProperty.POSITION_X, 0L)
    LabeledSlider(stringResource(R.string.position_x), px, -(canvasWidth / 2f)..(canvasWidth / 2f), "%.0f px".format(px)) {
        vm.setTransform(TransformProperty.POSITION_X, it)
    }
    val py = t.valueAt(TransformProperty.POSITION_Y, 0L)
    LabeledSlider(stringResource(R.string.position_y), py, -(canvasHeight / 2f)..(canvasHeight / 2f), "%.0f px".format(py)) {
        vm.setTransform(TransformProperty.POSITION_Y, it)
    }
}

@Composable
private fun ColorTab(vm: EditorViewModel, clip: VideoClip) {
    LabeledSlider(stringResource(R.string.brightness), clip.brightness, -1f..1f, "%.2f".format(clip.brightness)) {
        vm.setColor(brightness = it)
    }
    LabeledSlider(stringResource(R.string.contrast), clip.contrast, 0f..2f, "%.2f".format(clip.contrast)) {
        vm.setColor(contrast = it)
    }
    LabeledSlider(stringResource(R.string.saturation), clip.saturation, 0f..3f, "%.2f".format(clip.saturation)) {
        vm.setColor(saturation = it)
    }
    if (clip.type == ClipType.VIDEO && clip.hasAudio) {
        LabeledSlider(stringResource(R.string.volume), clip.volume, 0f..2f, "%d%%".format((clip.volume * 100).toInt())) {
            vm.setVolume(it)
        }
    }
}

@Composable
private fun EffectsTab(vm: EditorViewModel, clip: VideoClip) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Effects.ALL.forEach { def ->
            val active = clip.effects.any { it.effectId == def.id }
            FilterChip(
                selected = active,
                onClick = { vm.toggleEffect(def.id) },
                label = { Text(stringResource(effectLabel(def.id))) }
            )
        }
    }
    clip.effects.forEach { fx ->
        Effects.definition(fx.effectId)?.parameters.orEmpty().forEach { p ->
            val value = Effects.value(fx, p.id)
            LabeledSlider(stringResource(paramLabel(p.id)), value, p.min..p.max, "%.1f".format(value)) {
                vm.setEffectParam(fx.effectId, p.id, it)
            }
        }
    }
}

@Composable
private fun MotionTab(vm: EditorViewModel, state: EditorState) {
    SectionLabel(stringResource(R.string.tab_motion))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        JjPresets.ALL.forEach { id ->
            AssistChip(onClick = { vm.applyPreset(id) }, label = { Text(stringResource(presetLabel(id))) })
        }
    }
    LabeledSlider(stringResource(R.string.bpm), state.motionBpm, 60f..180f, "%.0f".format(state.motionBpm)) {
        vm.setMotionBpm(it)
    }
    LabeledSlider(
        stringResource(R.string.param_intensity),
        state.motionIntensity,
        0f..1f,
        "%d%%".format((state.motionIntensity * 100).toInt())
    ) { vm.setMotionIntensity(it) }
    OutlinedButton(onClick = { vm.resetMotion() }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.reset_motion))
    }
}

@Composable
private fun TextTab(vm: EditorViewModel, clip: VideoClip) {
    OutlinedTextField(
        value = clip.text,
        onValueChange = { vm.setText(it) },
        label = { Text(stringResource(R.string.text_content)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 1
    )
    LabeledSlider(stringResource(R.string.font_size), clip.fontSize, 24f..160f, "%.0f".format(clip.fontSize)) {
        vm.setFontSize(it)
    }
    LabeledSlider(stringResource(R.string.outline), clip.strokeWidth, 0f..12f, "%.0f".format(clip.strokeWidth)) {
        vm.setStrokeWidth(it)
    }
    SectionLabel(stringResource(R.string.text_color))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(0xFFFFFFFF, 0xFFFFE066, 0xFF000000, 0xFFFF5C8A).forEach { argb ->
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { vm.setTextColor(argb.toInt()) }
            )
        }
    }
}

private fun effectLabel(id: String): Int = when (id) {
    EffectIds.VINTAGE -> R.string.effect_vintage
    EffectIds.BLACK_WHITE -> R.string.effect_bw
    EffectIds.CINEMATIC -> R.string.effect_cinematic
    EffectIds.BLUR -> R.string.effect_blur
    else -> R.string.effect_glitch
}

private fun paramLabel(id: String): Int = when (id) {
    "strength" -> R.string.param_strength
    "radius" -> R.string.param_radius
    else -> R.string.param_intensity
}

private fun presetLabel(id: String): Int = when (id) {
    JjPresets.ZOOM_PUNCH -> R.string.preset_zoom_punch
    JjPresets.HEAVY_SHAKE -> R.string.preset_heavy_shake
    JjPresets.PULSE -> R.string.preset_pulse
    JjPresets.QUAKE -> R.string.preset_quake
    JjPresets.WHIP_PAN -> R.string.preset_whip_pan
    JjPresets.SPIN -> R.string.preset_spin
    else -> R.string.preset_glitch_rgb
}
