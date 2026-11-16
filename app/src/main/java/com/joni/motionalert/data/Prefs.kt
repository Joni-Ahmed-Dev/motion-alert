package com.joni.motionalert.data

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

/** Tiny prefs: the phone role, a stable device id, and the last alert times for the UI. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("motion_alert", Context.MODE_PRIVATE)

    /** "" = not picked yet, "bike" = this phone detects motion, "owner" = this phone gets alerts. */
    var role: String
        get() = sp.getString("role", "") ?: ""
        set(v) = sp.edit { putString("role", v) }

    val isBike: Boolean get() = role == "bike"
    val isOwner: Boolean get() = role == "owner"

    val deviceId: String
        get() {
            val saved = sp.getString("device_id", null)
            if (!saved.isNullOrBlank()) return saved
            val fresh = UUID.randomUUID().toString()
            sp.edit { putString("device_id", fresh) }
            return fresh
        }

    /** Wall clock millis of the last motion event this bike phone sent, 0 = none yet. */
    var lastSentMs: Long
        get() = sp.getLong("last_sent_ms", 0L)
        set(v) = sp.edit { putLong("last_sent_ms", v) }

    /** Wall clock millis of the last motion alert this owner phone showed, 0 = none yet. */
    var lastReceivedMs: Long
        get() = sp.getLong("last_received_ms", 0L)
        set(v) = sp.edit { putLong("last_received_ms", v) }
}
