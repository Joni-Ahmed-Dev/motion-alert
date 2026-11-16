package com.joni.motionalert

import android.app.Application
import android.content.Context
import com.joni.motionalert.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MotionAlertApp : Application() {
    lateinit var prefs: Prefs

    /** Outlives screens and the motion service: used for relay uploads. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
    }
}

val Context.motionApp: MotionAlertApp get() = applicationContext as MotionAlertApp
