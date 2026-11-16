package com.joni.motionalert.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.joni.motionalert.R
import com.joni.motionalert.ui.MainActivity

/**
 * All notifications in the app. Two channels: low importance monitoring
 * (the persistent "watching" notice on the bike phone) and high
 * importance motion alerts (the owner phone).
 */
object Notifier {
    const val SERVICE_ID = 1
    const val MOTION_ALERT_ID = 100

    const val CHANNEL_MONITORING = "monitoring"
    const val CHANNEL_ALERTS = "motion_alerts"

    /** Find Hub package: the name changed from Find My Device, the package did not. */
    const val FIND_HUB_PACKAGE = "com.google.android.apps.adm"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_MONITORING) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_MONITORING, ctx.getString(R.string.channel_monitoring), NotificationManager.IMPORTANCE_LOW)
            )
        }
        if (nm.getNotificationChannel(CHANNEL_ALERTS) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ALERTS, ctx.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    /** The persistent notice while the bike phone watches for motion. */
    fun serviceNotification(ctx: Context): android.app.Notification {
        createChannels(ctx)
        val open = PendingIntent.getActivity(
            ctx, SERVICE_ID, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(ctx, CHANNEL_MONITORING)
            .setSmallIcon(R.drawable.ic_stat_motion)
            .setContentTitle(ctx.getString(R.string.app_name))
            .setContentText(ctx.getString(R.string.service_notification))
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /** Motion alert on the owner phone. Tap opens Find Hub, so the owner
     *  sees the bike at once. When Find Hub is missing, its Play Store
     *  page opens instead. */
    fun motionAlert(ctx: Context) {
        createChannels(ctx)
        val open = PendingIntent.getActivity(
            ctx, MOTION_ALERT_ID, findHubIntent(ctx),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_motion)
            .setContentTitle(ctx.getString(R.string.motion_title))
            .setContentText(ctx.getString(R.string.motion_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(MOTION_ALERT_ID, n)
    }

    /** Launch Find Hub, or its Play Store page when it is not installed. */
    fun findHubIntent(ctx: Context): Intent {
        val launch = ctx.packageManager.getLaunchIntentForPackage(FIND_HUB_PACKAGE)
        if (launch != null) return launch
        return try {
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$FIND_HUB_PACKAGE"))
        } catch (e: Exception) {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$FIND_HUB_PACKAGE"),
            )
        }
    }
}
