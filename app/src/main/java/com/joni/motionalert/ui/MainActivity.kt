package com.joni.motionalert.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.joni.motionalert.R
import com.joni.motionalert.motion.MotionService
import com.joni.motionalert.motionApp
import com.joni.motionalert.notify.Notifier
import com.joni.motionalert.push.PushRegistrar
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Leaf = Color(0xFF63A54E)
private val Bg = Color(0xFFF1F4F0)
private val TextDark = Color(0xFF111111)

@Composable
private fun MotionAlertTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Leaf,
            onPrimary = Color.White,
            background = Bg,
            surface = Color.White,
            onBackground = TextDark,
            onSurface = TextDark,
        ),
        content = content,
    )
}

/** Ticks once a second, so status lines and last-alert times stay fresh. */
@Composable
private fun rememberNow(): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) { delay(1000); value = System.currentTimeMillis() }
    }
    return now
}

private fun hasNotifications(ctx: Context): Boolean {
    if (Build.VERSION.SDK_INT < 33) return true
    return ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

private fun isIgnoringBattery(ctx: Context): Boolean {
    val pm = ctx.getSystemService(PowerManager::class.java) ?: return true
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}

/** Opens the system screen that asks the user to let the app run without limits. */
private fun openBatterySettings(ctx: Context) {
    val direct = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${ctx.packageName}"),
    )
    try {
        ctx.startActivity(direct)
    } catch (e: Exception) {
        runCatching { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
    }
}

private fun formatTime(ms: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MotionAlertTheme {
                MotionAlertScreen()
            }
        }
    }
}

@Composable
private fun MotionAlertScreen() {
    val ctx = LocalContext.current
    val app = ctx.motionApp
    var role by remember { mutableStateOf(app.prefs.role) }

    LaunchedEffect(role) {
        if (role == "owner") PushRegistrar.register(app)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        when (role) {
            "" -> RolePicker(
                onPick = { picked ->
                    app.prefs.role = picked
                    role = picked
                    if (picked == "bike") MotionService.tryStart(ctx)
                },
            )
            "bike" -> BikeHome(
                onChangeRole = {
                    MotionService.stop(ctx)
                    app.prefs.role = ""
                    role = ""
                },
            )
            else -> OwnerHome(
                onChangeRole = {
                    app.prefs.role = ""
                    role = ""
                },
            )
        }
    }
}

@Composable
private fun RolePicker(onPick: (String) -> Unit) {
    Text(
        text = stringResource(R.string.role_title),
        style = MaterialTheme.typography.titleLarge,
    )
    Button(
        onClick = { onPick("bike") },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(text = stringResource(R.string.role_bike), fontWeight = FontWeight.Bold)
            Text(text = stringResource(R.string.role_bike_desc))
        }
    }
    Button(
        onClick = { onPick("owner") },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(text = stringResource(R.string.role_owner), fontWeight = FontWeight.Bold)
            Text(text = stringResource(R.string.role_owner_desc))
        }
    }
}

@Composable
private fun SetupCard() {
    val ctx = LocalContext.current
    val now = rememberNow()
    var batteryDismissed by remember { mutableStateOf(false) }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val needsNotif = Build.VERSION.SDK_INT >= 33 && !hasNotifications(ctx)
    val needsBattery = !isIgnoringBattery(ctx) && !batteryDismissed
    if (!needsNotif && !needsBattery) return
    // The ticker above only exists to refresh these two checks after the
    // user comes back from the system screens.
    @Suppress("UNUSED_VARIABLE")
    val tick = now
    if (needsNotif) {
        Text(
            text = stringResource(R.string.notif_permission_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(text = stringResource(R.string.notif_permission_text))
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.notif_allow))
        }
    }
    if (needsBattery) {
        Text(
            text = stringResource(R.string.battery_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(text = stringResource(R.string.battery_text))
        Button(
            onClick = { openBatterySettings(ctx) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.battery_button))
        }
        TextButton(onClick = { batteryDismissed = true }) {
            Text(text = stringResource(R.string.battery_skip))
        }
    }
}

@Composable
private fun BikeHome(onChangeRole: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.motionApp
    val now = rememberNow()
    var testSent by remember { mutableStateOf(false) }
    SetupCard()
    val running = MotionService.isRunning
    Text(
        text = stringResource(if (running) R.string.bike_status_on else R.string.bike_status_off),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Text(text = stringResource(R.string.bike_note))
    if (running) {
        OutlinedButton(
            onClick = { MotionService.stop(ctx) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.bike_stop))
        }
    } else {
        Button(
            onClick = { MotionService.tryStart(ctx) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.bike_start))
        }
    }
    Button(
        onClick = {
            MotionService.sendTest(ctx)
            testSent = true
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = stringResource(R.string.bike_test))
    }
    if (testSent) Text(text = stringResource(R.string.bike_test_sent))
    val last = app.prefs.lastSentMs
    Text(
        text = if (last > 0L) stringResource(R.string.bike_last_alert, formatTime(last))
        else stringResource(R.string.bike_never),
    )
    @Suppress("UNUSED_VARIABLE")
    val tick = now
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = onChangeRole) {
        Text(text = stringResource(R.string.role_change))
    }
}

@Composable
private fun OwnerHome(onChangeRole: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.motionApp
    val now = rememberNow()
    SetupCard()
    Text(
        text = stringResource(R.string.owner_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Text(text = stringResource(R.string.owner_note))
    Button(
        onClick = { runCatching { ctx.startActivity(Notifier.findHubIntent(ctx)) } },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = stringResource(R.string.owner_open_findhub))
    }
    OutlinedButton(
        onClick = { Notifier.motionAlert(ctx) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = stringResource(R.string.owner_test_notif))
    }
    val last = app.prefs.lastReceivedMs
    Text(
        text = if (last > 0L) stringResource(R.string.bike_last_alert, formatTime(last))
        else stringResource(R.string.bike_never),
    )
    @Suppress("UNUSED_VARIABLE")
    val tick = now
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = onChangeRole) {
        Text(text = stringResource(R.string.role_change))
    }
}
