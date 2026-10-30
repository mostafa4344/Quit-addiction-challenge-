package com.mostafa.quitchallenge.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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

/**
 * Opens a system settings page. Some phones (Android Go, some OEM builds) don't have every page,
 * so fall back to the app's info page, then to a short hint, instead of crashing.
 */
fun openSettings(context: Context, intent: Intent) {
    for (candidate in listOf(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), appInfoIntent(context))) {
        try {
            context.startActivity(candidate)
            return
        } catch (e: ActivityNotFoundException) {
            // try the next page
        } catch (e: SecurityException) {
            // try the next page
        }
    }
    Toast.makeText(context, "Open Settings > Apps > Quit Challenge to change this.", Toast.LENGTH_LONG).show()
}

data class PermissionStatus(
    val usage: Boolean,
    val notifications: Boolean,
    val overlay: Boolean,
    val battery: Boolean,
) {
    val done: Int get() = listOf(usage, notifications, overlay, battery).count { it }
    val total: Int get() = 4
    val missing: Int get() = total - done
}

fun permissionStatus(context: Context): PermissionStatus = PermissionStatus(
    usage = UsageTracker.hasUsageAccess(context),
    notifications = Notifier.canPost(context),
    overlay = WarningActivity.canShow(context),
    battery = isIgnoringBatteryOptimizations(context),
)

/** Live permission status, re-read every 2 seconds so it updates after returning from system settings. */
@Composable
fun rememberPermissionStatus(): PermissionStatus {
    val context = LocalContext.current
    val now = rememberNow(2_000L)
    return key(now) { permissionStatus(context) }
}

/** One permission the app needs, with its live status and how to turn it on. */
class PermissionStep(
    val number: Int,
    val title: String,
    /** Few words for the compact setup banner. */
    val why: String,
    val description: String,
    val granted: Boolean,
    val restricted: Boolean,
    val onGrant: () -> Unit,
)

/** Steps 1-4 in the order the user is asked to turn them on. */
@Composable
fun rememberPermissionSteps(): List<PermissionStep> {
    val context = LocalContext.current
    val status = rememberPermissionStatus()
    val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return listOf(
        PermissionStep(
            number = 1,
            title = "📲 Usage access",
            why = "to track app time limits",
            description = "Measures how long you use each app (the same data Digital Wellbeing uses). Needed for app time-limit challenges.",
            granted = status.usage,
            restricted = Build.VERSION.SDK_INT >= 35,
            onGrant = { openSettings(context, UsageTracker.usageAccessSettingsIntent()) },
        ),
        PermissionStep(
            number = 2,
            title = "🔔 Notifications",
            why = "for check-ins and break reminders",
            description = "Check-ins, break reminders near your limit, milestone celebrations and encouragement after a slip.",
            granted = status.notifications,
            restricted = false,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openSettings(context, appInfoIntent(context))
                }
            },
        ),
        PermissionStep(
            number = 3,
            title = "📢 Display over other apps",
            why = "to show full-screen break reminders",
            description = "Shows a full-screen \"Time for a break\" or \"Time limit crossed\" screen over the app you're using, " +
                "instead of only a notification. Find Quit Challenge in the list and turn it on.",
            granted = status.overlay,
            restricted = Build.VERSION.SDK_INT >= 35,
            onGrant = {
                openSettings(context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
            },
        ),
        PermissionStep(
            number = 4,
            title = "🔋 Battery: unrestricted",
            why = "so reminders arrive on time",
            description = "Some phones (Samsung, Xiaomi, Oppo, Vivo…) stop background protection to save battery. " +
                "Set Quit Challenge to \"Unrestricted\" / \"No restrictions\" so checks keep running.",
            granted = status.battery,
            restricted = false,
            onGrant = { openSettings(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
        ),
    )
}

/**
 * All protection permissions with live status, plain-language help and the steps for "restricted settings".
 * With [onlyMissing], only the ones still off are shown.
 */
@Composable
fun PermissionsList(onlyMissing: Boolean = false) {
    val steps = rememberPermissionSteps()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.filter { !onlyMissing || !it.granted }.forEach { step ->
            PermissionCard(step.number, step.title, step.description, step.granted, step.restricted, step.onGrant)
        }
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
                    OutlinedButton(onClick = { openSettings(context, appInfoIntent(context)) }) { Text("Open app info") }
                }
            }
        }
    }
}

/**
 * A hard-to-miss card that stays at the top of the main screens until every permission is on.
 * It stays compact (one line + one button for the next step) so the user's challenges remain
 * visible even on small phones with large text; full details live in the Protection tab.
 */
@Composable
fun SetupBanner() {
    val steps = rememberPermissionSteps()
    val missing = steps.filterNot { it.granted }
    val next = missing.firstOrNull() ?: return
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "⚠️ Protection isn't fully on (${steps.size - missing.size} of ${steps.size} done)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(
                progress = { (steps.size - missing.size) / steps.size.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Next: ${next.title}, ${next.why}.", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = next.onGrant) { Text("Turn on") }
                if (missing.size > 1) {
                    Text("${missing.size - 1} more after this", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
