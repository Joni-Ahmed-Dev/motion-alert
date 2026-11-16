package com.joni.motionalert.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.joni.motionalert.data.MotionApi
import com.joni.motionalert.motionApp
import com.joni.motionalert.notify.Notifier
import kotlinx.coroutines.launch

/**
 * Data-only FCM messages (the relay sends no "notification" block) so the
 * app localises the text itself and controls the tap action.
 *  kind=motion -> the bike moved: show the alert on the owner phone.
 */
class MotionPushService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val app = motionApp
        if (app.prefs.isOwner) {
            app.appScope.launch { MotionApi.registerToken(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val app = motionApp
        if (!app.prefs.isOwner) return
        if (message.data["kind"] != "motion") return
        app.prefs.lastReceivedMs = System.currentTimeMillis()
        Notifier.motionAlert(this)
    }
}
