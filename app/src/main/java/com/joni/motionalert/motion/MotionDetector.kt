package com.joni.motionalert.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Accelerometer watcher for a parked bike being handled. Copied from
 * BikeGuard's SensorGuard MOTION mode, same thresholds: a low-pass gravity
 * estimate, a 1.5 m/s^2 deviation counts as a shake, and 4 shakes within
 * 3 seconds count as motion (one bump alone does not alert).
 */
class MotionDetector(ctx: Context, private val listener: Listener) : SensorEventListener {
    interface Listener {
        fun onMotion()
    }

    private val sm = ctx.getSystemService(SensorManager::class.java)
    private val accel: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var running = false
    private var gravity = 9.81f
    private var burst = 0
    private var burstStart = 0L

    fun start() {
        if (running) return
        running = true
        burst = 0
        val a = accel ?: return
        // SENSOR_DELAY_NORMAL (~5 Hz): cheap, motion while parked is slow.
        sm.registerListener(this, a, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        if (!running) return
        running = false
        sm.unregisterListener(this)
    }

    override fun onSensorChanged(e: SensorEvent) {
        val mag = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2])
        gravity = 0.95f * gravity + 0.05f * mag
        if (abs(mag - gravity) > 1.5f) {
            val now = SystemClock.elapsedRealtime()
            if (now - burstStart > 3_000) { burst = 0; burstStart = now }
            if (++burst >= 4) { burst = 0; listener.onMotion() }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        /** Minimum gap between two motion alerts, so one shake does not spam. */
        const val COOLDOWN_MS = 2 * 60_000L

        /** Pure cooldown check, so it can be unit tested without a sensor. */
        fun shouldAlert(lastAlertElapsedMs: Long, nowElapsedMs: Long): Boolean =
            nowElapsedMs - lastAlertElapsedMs >= COOLDOWN_MS
    }
}
