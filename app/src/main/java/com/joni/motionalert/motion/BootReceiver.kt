package com.joni.motionalert.motion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joni.motionalert.motionApp

/** After a reboot, restart motion monitoring on the bike phone. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.motionApp.prefs.isBike) {
            MotionService.tryStart(context)
        }
    }
}
