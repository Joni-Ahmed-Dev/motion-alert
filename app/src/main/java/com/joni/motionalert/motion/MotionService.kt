package com.joni.motionalert.motion

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.joni.motionalert.data.MotionApi
import com.joni.motionalert.motionApp
import com.joni.motionalert.notify.Notifier
import kotlinx.coroutines.launch

/**
 * Foreground service on the bike phone: listens to the accelerometer and
 * reports motion to the relay Worker. Sticky, so Android restarts it after
 * it is killed; the BootReceiver restarts it after a reboot.
 */
class MotionService : Service(), MotionDetector.Listener {
    private lateinit var detector: MotionDetector
    private var lastMotionElapsedMs = -MotionDetector.COOLDOWN_MS

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        detector = MotionDetector(this, this)
        detector.start()
        Notifier.createChannels(this)
        startForeground(Notifier.SERVICE_ID, Notifier.serviceNotification(this))
    }

    override fun onDestroy() {
        detector.stop()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TEST) sendMotion(test = true)
        return START_STICKY
    }

    override fun onMotion() {
        val now = SystemClock.elapsedRealtime()
        if (!MotionDetector.shouldAlert(lastMotionElapsedMs, now)) return
        lastMotionElapsedMs = now
        sendMotion(test = false)
    }

    private fun sendMotion(test: Boolean) {
        val app = motionApp
        val nowMs = System.currentTimeMillis()
        app.prefs.lastSentMs = nowMs
        app.appScope.launch {
            MotionApi.postMotion(app.prefs.deviceId, nowMs)
        }
        if (test) lastMotionElapsedMs = SystemClock.elapsedRealtime()
    }

    companion object {
        const val ACTION_TEST = "com.joni.motionalert.TEST_ALERT"

        /** True while the service process lives; the UI reads this for its status line. */
        @Volatile var isRunning = false
            private set

        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, MotionService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, MotionService::class.java))
        }

        fun sendTest(ctx: Context) {
            val i = Intent(ctx, MotionService::class.java).setAction(ACTION_TEST)
            runCatching { ContextCompat.startForegroundService(ctx, i) }
        }

        /** Used by receivers that must never crash on a refused background start. */
        fun tryStart(ctx: Context): Boolean = try {
            start(ctx); true
        } catch (e: Exception) {
            false
        }
    }
}
