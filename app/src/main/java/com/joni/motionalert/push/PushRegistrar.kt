package com.joni.motionalert.push

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.joni.motionalert.MotionAlertApp
import com.joni.motionalert.data.MotionApi

/** Registers the owner phone's FCM token with the relay. Silently does
 *  nothing when Firebase isn't configured. */
object PushRegistrar {
    fun register(app: MotionAlertApp) {
        if (!app.prefs.isOwner) return
        if (FirebaseApp.getApps(app).isEmpty()) return
        runCatching {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    app.appScope.launch { MotionApi.registerToken(token) }
                }
        }
    }
}
