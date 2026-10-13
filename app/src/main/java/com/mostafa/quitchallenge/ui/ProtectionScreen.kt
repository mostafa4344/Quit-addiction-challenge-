@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.mostafa.quitchallenge.ui

import android.Manifest
import android.os.Build
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.mostafa.quitchallenge.AppViewModel
import com.mostafa.quitchallenge.data.AppData
import com.mostafa.quitchallenge.guard.GuardService
import com.mostafa.quitchallenge.guard.Notifier
import com.mostafa.quitchallenge.guard.UsageTracker

@Composable
fun ProtectionScreen(data: AppData, vm: AppViewModel) {
    val context = LocalContext.current
    // Re-read permission states regularly so they update after returning from system settings.
    val now = rememberNow(2_000L)
    val usageOk = key(now) { UsageTracker.hasUsageAccess(context) }
    val guardOk = key(now) { GuardService.isEnabled(context) }
    val notifyOk = key(now) { Notifier.canPost(context) }
    val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var newKeyword by remember { mutableStateOf("") }

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
                "Give these permissions so the app can track your app time, send break reminders and warn you before you search for something you're trying to quit.",
                style = MaterialTheme.typography.bodyMedium,
            )

            PermissionCard(
                title = "📲 Usage access",
                description = "Measures how long you use each app (the same data Digital Wellbeing uses). Needed for app time-limit challenges.",
                granted = usageOk,
                onGrant = { context.startActivity(UsageTracker.usageAccessSettingsIntent()) },
            )
            PermissionCard(
                title = "🔔 Notifications",
                description = "Break reminders when you're close to your limit, and alerts when a challenge restarts.",
                granted = notifyOk,
                onGrant = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
            PermissionCard(
                title = "🛡 Accessibility (Guard)",
                description = "Shows an \"Are you sure?\" screen when you type a blocked keyword, and stops you the moment an app crosses its time limit. " +
                    "In settings, open \"Installed apps\" or \"Downloaded apps\", choose \"Quit Challenge Guard\" and turn it on. " +
                    "Everything is checked on your phone only; typed text is never saved or sent anywhere.",
                granted = guardOk,
                onGrant = { context.startActivity(GuardService.settingsIntent()) },
            )

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("🔎 Keyword guard", fontWeight = FontWeight.Bold)
                            Text(
                                "When you type any of these words, you'll see: \"You have tried to search … Are you sure you want to continue?\"",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(checked = data.settings.keywordGuard, onCheckedChange = { vm.setKeywordGuard(it) })
                    }
                    if (!guardOk) {
                        Text(
                            "Turn on the Accessibility permission above for the keyword guard to work.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.settings.keywords.forEach { kw ->
                            InputChip(selected = false, onClick = { vm.removeKeyword(kw) }, label = { Text("$kw ✕") })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newKeyword,
                            onValueChange = { newKeyword = it.take(40) },
                            label = { Text("Add a keyword") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                vm.addKeyword(newKeyword)
                                newKeyword = ""
                            },
                            enabled = newKeyword.isNotBlank(),
                        ) { Text("Add") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(title: String, description: String, granted: Boolean, onGrant: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(if (granted) "✅ On" else "❌ Off", fontWeight = FontWeight.Bold)
            }
            Text(description, style = MaterialTheme.typography.bodySmall)
            if (!granted) {
                OutlinedButton(onClick = onGrant) { Text("Turn on") }
            }
        }
    }
}
