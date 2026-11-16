package com.joni.motionalert

import com.joni.motionalert.motion.MotionDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cooldown rule: one shake alerts once, then silence for 2 minutes. */
class MotionCooldownTest {
    @Test
    fun neverAlerted_alerts() {
        assertTrue(MotionDetector.shouldAlert(-MotionDetector.COOLDOWN_MS, 0L))
    }

    @Test
    fun justAlerted_doesNotAlert() {
        assertFalse(MotionDetector.shouldAlert(1_000L, 2_000L))
    }

    @Test
    fun oneSecondBeforeCooldown_doesNotAlert() {
        assertFalse(MotionDetector.shouldAlert(0L, MotionDetector.COOLDOWN_MS - 1))
    }

    @Test
    fun exactlyAtCooldown_alerts() {
        assertTrue(MotionDetector.shouldAlert(0L, MotionDetector.COOLDOWN_MS))
    }
}
