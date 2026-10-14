package com.mostafa.quitchallenge.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mostafa.quitchallenge.guard.WarningActivity
import com.mostafa.quitchallenge.guard.Notifier
import com.mostafa.quitchallenge.guard.UsageTracker

fun isIgnoringBatteryOptimizations(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

private fun appInfoIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** All protection permissions with live status, plain-language help and the steps for "restricted settings". */
@Composable
fun PermissionsList() {
    val context = LocalContext.current
    // Re-read permission states regularly so they update after returning from system settings.
    val now = rememberNow(2_000L)
    val usageOk = key(now) { UsageTracker.hasUsageAccess(context) }
    val overlayOk = key(now) { WarningActivity.canShow(context) }
    val notifyOk = key(now) { Notifier.canPost(context) }
    val batteryOk = key(now) { isIgnoringBatteryOptimizations(context) }
    val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PermissionCard(
            step = 1,
            title = "📲 Usage access",
            description = "Measures how long you use each app (the same data Digital Wellbeing uses). Needed for app time-limit challenges.",
            granted = usageOk,
            restricted = Build.VERSION.SDK_INT >= 35,
            onGrant = { context.startActivity(UsageTracker.usageAccessSettingsIntent()) },
        )
        PermissionCard(
            step = 2,
            title = "🔔 Notifications",
            description = "Break reminders near your limit, milestone celebrations and encouragement after a slip.",
            granted = notifyOk,
            restricted = false,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.startActivity(appInfoIntent(context))
                }
            },
        )
        PermissionCard(
            step = 3,
            title = "🪟 Display over other apps",
            description = "Shows a full-screen \"Time for a break\" or \"Time limit crossed\" screen over the app you're using, " +
                "instead of only a notification. Find Quit Challenge in the list and turn it on.",
            granted = overlayOk,
            restricted = Build.VERSION.SDK_INT >= 35,
            onGrant = {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        )
        PermissionCard(
            step = 4,
            title = "🔋 Battery: unrestricted",
            description = "Some phones (Samsung, Xiaomi, Oppo, Vivo…) stop background protection to save battery. " +
                "Set Quit Challenge to \"Unrestricted\" / \"No restrictions\" so checks keep running.",
            granted = batteryOk,
            restricted = false,
            onGrant = {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
        )
    }

}

@Composable
private fun PermissionCard(
    step: Int,
    title: String,
    description: String,
    granted: Boolean,
    restricted: Boolean,
    onGrant: () -> Unit,
) {
    val context = LocalContext.current
    var showHelp by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$step. $title", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(if (granted) "✅ On" else "❌ Off", fontWeight = FontWeight.Bold)
            }
            Text(description, style = MaterialTheme.typography.bodySmall)
            if (!granted) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onGrant) { Text("Turn on") }
                    if (restricted) TextButton(onClick = { showHelp = !showHelp }) { Text("Can't turn it on?") }
                }
                if (showHelp) {
                    Text(
                        "Android blocks this setting for apps installed from an APK file (\"Restricted setting\"). To allow it:\n" +
                            "1. Tap \"Open app info\" below.\n" +
                            "2. Tap the ⋮ menu in the top-right corner.\n" +
                            "3. Tap \"Allow restricted settings\" and confirm with your screen lock.\n" +
                            "4. Come back here and tap \"Turn on\" again.\n" +
                            "If you don't see ⋮, first try \"Turn on\" once; the menu appears after Android blocks it the first time.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = { context.startActivity(appInfoIntent(context)) }) { Text("Open app info") }
                }
            }
        }
    }
}
