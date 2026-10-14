@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.mostafa.quitchallenge.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import com.mostafa.quitchallenge.data.MAX_CHECK_INS_PER_DAY
import com.mostafa.quitchallenge.data.formatTimeOfDay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mostafa.quitchallenge.AppViewModel
import com.mostafa.quitchallenge.data.AppData
import com.mostafa.quitchallenge.data.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun ProtectionScreen(data: AppData, vm: AppViewModel, onOpenPrivacy: () -> Unit) {
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("Protection 🛡") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Turn these on so the app can track your app time, show break reminders and send your daily check-in.",
                style = MaterialTheme.typography.bodyMedium,
            )

            PermissionsList()

            CheckInCard(data, vm)

            AppLockCard()


            BackupCard()

            TextButton(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth()) {
                Text("🔒 Privacy policy: your data never leaves this phone")
            }
        }
    }
}

/** Export / import all data as a JSON file, so nothing is lost when changing phones. */
@Composable
private fun BackupCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            message = withContext(Dispatchers.IO) {
                try {
                    val text = AppGraph.repo(context).exportJson()
                    context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                        ?: error("Can't open file")
                    "✅ Backup saved."
                } catch (e: Exception) {
                    "❌ Couldn't save the backup: ${e.message}"
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                } catch (e: Exception) {
                    null
                }
            }
            if (text == null) message = "❌ Couldn't read that file." else pendingImport = text
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("💾 Backup", fontWeight = FontWeight.Bold)
            Text(
                "Your data lives only on this phone. Save a backup file (for example to Google Drive) to keep it when you change phones or reinstall.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exportLauncher.launch("quit-challenge-backup-${LocalDate.now()}.json") }) { Text("Save backup") }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }) {
                    Text("Restore")
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }

    pendingImport?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Restore this backup?") },
            text = { Text("All current challenges, logs and settings on this phone will be replaced by the backup.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    val ok = AppGraph.repo(context).importJson(text)
                    message = if (ok) "✅ Backup restored." else "❌ That file isn't a Quit Challenge backup."
                }) { Text("Restore", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } },
        )
    }
}

private data class CheckInPreset(val label: String, val times: List<Int>)

private val CHECK_IN_PRESETS = listOf(
    CheckInPreset("Once a day", listOf(21 * 60)),
    CheckInPreset("Twice", listOf(9 * 60, 21 * 60)),
    CheckInPreset("3 times", listOf(9 * 60, 15 * 60, 21 * 60)),
    CheckInPreset("4 times", listOf(8 * 60, 12 * 60, 17 * 60, 22 * 60)),
)

/** Lets each person pick how many check-in notifications they get per day, and at what times. */
@Composable
private fun CheckInCard(data: AppData, vm: AppViewModel) {
    val context = LocalContext.current
    val times = data.settings.checkInTimes.sorted()

    fun pickTime(initial: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            initial / 60,
            initial % 60,
            false,
        ).show()
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("📅 Check-in notifications", fontWeight = FontWeight.Bold)
                    Text(
                        "Each \"Stay away\" challenge asks: \"Are you still on track?\" " +
                            "Answer right from the notification: ✅ keep going, or 🌊 slipped and restart.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = data.settings.dailyCheckIn, onCheckedChange = { vm.setDailyCheckIn(it) })
            }
            if (data.settings.dailyCheckIn) {
                Text(
                    if (times.isEmpty()) "No times set yet. Add at least one." else "${times.size} time${if (times.size == 1) "" else "s"} a day:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                // Tap a time to change it, ✕ to remove it.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    times.forEach { t ->
                        InputChip(
                            selected = true,
                            onClick = { pickTime(t) { new -> vm.setCheckInTimes(times - t + new) } },
                            label = { Text("🕘 ${formatTimeOfDay(t)}") },
                            trailingIcon = {
                                Text(
                                    "✕",
                                    modifier = Modifier.clickable { vm.setCheckInTimes(times - t) },
                                )
                            },
                        )
                    }
                }
                if (times.size < MAX_CHECK_INS_PER_DAY) {
                    OutlinedButton(onClick = { pickTime(times.lastOrNull()?.plus(60)?.rem(24 * 60) ?: (21 * 60)) { vm.setCheckInTimes(times + it) } }) {
                        Text("➕ Add a time")
                    }
                }
                Text("Quick choices:", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CHECK_IN_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = times == preset.times,
                            onClick = { vm.setCheckInTimes(preset.times) },
                            label = { Text(preset.label) },
                        )
                    }
                }
                Text(
                    "Up to $MAX_CHECK_INS_PER_DAY times a day. Tap a time to change it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
