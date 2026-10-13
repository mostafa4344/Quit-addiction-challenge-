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
import androidx.compose.material3.InputChip
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
import com.mostafa.quitchallenge.guard.GuardService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun ProtectionScreen(data: AppData, vm: AppViewModel) {
    val context = LocalContext.current
    val guardOk = GuardService.isEnabled(context)
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
                "Turn these on so the app can track your app time, send break reminders and warn you before you search for something you're trying to quit.",
                style = MaterialTheme.typography.bodyMedium,
            )

            PermissionsList()

            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("⏸ Pause before opening limited apps", fontWeight = FontWeight.Bold)
                        Text(
                            "When you open an app from an app-limit challenge, take one breath first and choose: close it or open it. Needs step 3.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(checked = data.settings.pauseBeforeOpen, onCheckedChange = { vm.setPauseBeforeOpen(it) })
                }
            }

            AppLockCard()

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
                            "Turn on step 3 (Accessibility) above for the keyword guard to work.",
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

            BackupCard()
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
