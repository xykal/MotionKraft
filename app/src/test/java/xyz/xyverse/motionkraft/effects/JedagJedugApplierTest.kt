package xyz.xyverse.motionkraft.effects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xyverse.motionkraft.data.model.EffectIds
import xyz.xyverse.motionkraft.data.model.JjPresets

class JedagJedugApplierTest {

    @Test
    fun zeroBpmDoesNotLoopForever() {
        val result = JedagJedugApplier.build(JjPresets.HEAVY_SHAKE, 1_000_000L, 0f, 1f)
        assertTrue(result.transform.positionX.isNotEmpty())
    }

    @Test
    fun glitchPresetAddsGlitchEffectOnly() {
        val result = JedagJedugApplier.build(JjPresets.GLITCH_RGB, 1_000_000L, 120f, 0.5f)
        assertEquals(1, result.effects.size)
        assertEquals(EffectIds.GLITCH, result.effects.first().effectId)
    }

    @Test
    fun zoomPunchKeepsScaleWithinExpectedRange() {
        val result = JedagJedugApplier.build(JjPresets.ZOOM_PUNCH, 3_000_000L, 120f, 1f)
        assertTrue(result.transform.scaleX.all { it.value in 0.9f..2.0f })
    }
}
