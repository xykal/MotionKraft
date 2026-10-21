package xyz.xyverse.motionkraft.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TransformTest {

    @Test
    fun linearInterpolatesBetweenKeys() {
        val keys = listOf(Keyframe(0L, 0f), Keyframe(1_000L, 10f))
        assertEquals(5f, Transform.sample(keys, 500L), 1e-4f)
    }

    @Test
    fun holdKeepsPreviousValueUntilNextKey() {
        val keys = listOf(Keyframe(0L, 3f, Interpolation.HOLD), Keyframe(1_000L, 9f))
        assertEquals(3f, Transform.sample(keys, 999L), 0f)
        assertEquals(9f, Transform.sample(keys, 1_000L), 0f)
    }

    @Test
    fun elasticEasingStartsAtZeroAndEndsAtOne() {
        assertEquals(0f, Transform.ease(Interpolation.ELASTIC, 0f), 1e-6f)
        assertEquals(1f, Transform.ease(Interpolation.ELASTIC, 1f), 1e-6f)
    }

    @Test
    fun bounceEasingEndsAtOne() {
        assertEquals(1f, Transform.ease(Interpolation.BOUNCE, 1f), 1e-4f)
    }

    @Test
    fun withConstantReplacesAnimation() {
        val animated = Transform(scaleX = listOf(Keyframe(0L, 1f), Keyframe(1_000L, 2f)))
        val fixed = animated.withConstant(TransformProperty.SCALE_X, 1.5f)
        assertEquals(1, fixed.scaleX.size)
        assertEquals(1.5f, fixed.valueAt(TransformProperty.SCALE_X, 500L), 0f)
    }
}
