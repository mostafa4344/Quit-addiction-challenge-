@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.mostafa.quitchallenge.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mostafa.quitchallenge.data.Category
import com.mostafa.quitchallenge.data.ChallengeType
import com.mostafa.quitchallenge.data.TrackedApp
import com.mostafa.quitchallenge.data.formatGoal
import com.mostafa.quitchallenge.data.formatMinutes
import com.mostafa.quitchallenge.guard.UsageTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PRESET_DAYS = listOf(1, 3, 7, 21, 30, 90)
private val PRESET_LIMITS = listOf(0, 15, 30, 60, 120)

@Composable
fun NewChallengeScreen(
    onBack: () -> Unit,
    onCreate: (
        name: String,
        category: Category,
        goalHours: Int,
        why: String,
        type: ChallengeType,
        apps: List<TrackedApp>,
        dailyLimitMinutes: Int,
        minutesPerDay: Int,
        moneyPerDay: Double,
    ) -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(ChallengeType.TIMER) }
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Category.SOCIAL_MEDIA) }
    var days by rememberSaveable { mutableStateOf("7") }
    var hours by rememberSaveable { mutableStateOf("0") }
    var why by rememberSaveable { mutableStateOf("") }
    var limit by rememberSaveable { mutableStateOf("30") }
    var apps by remember { mutableStateOf(emptyList<TrackedApp>()) }
    var costMinutes by rememberSaveable { mutableStateOf("") }
    var costMoney by rememberSaveable { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    val isAppLimit = type == ChallengeType.APP_LIMIT
    val goalHours = (days.toIntOrNull() ?: 0) * 24 + (hours.toIntOrNull() ?: 0)
    val limitMinutes = limit.toIntOrNull() ?: 0
    val defaultName = if (isAppLimit && apps.isNotEmpty()) {
        "Challenge: ${apps.first().label} ${formatMinutes(limitMinutes)}/day for ${formatGoal(goalHours.coerceAtLeast(1))}"
    } else {
        "Challenge: no ${category.label.lowercase()} for ${formatGoal(goalHours.coerceAtLeast(1))}"
    }
    val valid = goalHours > 0 && (!isAppLimit || apps.isNotEmpty())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New challenge") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle("Challenge type")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isAppLimit,
                    onClick = { type = ChallengeType.TIMER },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("⏱ Stay away") }
                SegmentedButton(
                    selected = isAppLimit,
                    onClick = { type = ChallengeType.APP_LIMIT },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("📲 App time limit") }
            }
            Text(
                if (isAppLimit) {
                    "The app tracks how long you use the chosen apps each day. Cross the daily limit and the challenge fails and restarts automatically."
                } else {
                    "A timer runs while you stay away. If you slip, tap \"I failed\" and the challenge restarts from zero."
                },
                style = MaterialTheme.typography.bodySmall,
            )

            SectionTitle("What do you want to quit?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text("${cat.emoji} ${cat.label}") },
                    )
                }
            }

            if (isAppLimit) {
                SectionTitle("Which apps?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    apps.forEach { app ->
                        InputChip(
                            selected = true,
                            onClick = { apps = apps - app },
                            label = { Text("${app.label} ✕") },
                        )
                    }
                }
                OutlinedButton(onClick = { showPicker = true }) { Text("➕ Choose apps") }

                SectionTitle("Daily time limit (minutes)")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESET_LIMITS.forEach { m ->
                        FilterChip(
                            selected = limitMinutes == m,
                            onClick = { limit = m.toString() },
                            label = { Text(formatMinutes(m)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it.filter(Char::isDigit).take(4) },
                    label = { Text("Minutes per day") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(80) },
                label = { Text("Challenge name") },
                placeholder = { Text(defaultName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(if (isAppLimit) "For how long?" else "How many days / hours will you stay away?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESET_DAYS.forEach { d ->
                    FilterChip(
                        selected = days == d.toString() && (hours.toIntOrNull() ?: 0) == 0,
                        onClick = {
                            days = d.toString()
                            hours = "0"
                        },
                        label = { Text(formatGoal(d * 24)) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it.filter(Char::isDigit).take(4) },
                    label = { Text("Days") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it.filter(Char::isDigit).take(3) },
                    label = { Text("Hours") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                if (goalHours > 0) "Total goal: ${formatGoal(goalHours)}" else "Set a goal of at least 1 hour",
                color = if (goalHours > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )

            SectionTitle("Why do you want to quit?")
            OutlinedTextField(
                value = why,
                onValueChange = { why = it.take(300) },
                label = { Text("Your reason (shown to you in hard moments)") },
                placeholder = { Text("e.g. I want to focus on my studies and spend time with my family") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("What did it cost you per day? (optional)")
            Text(
                "Used to show how much time and money you win back.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = costMinutes,
                    onValueChange = { costMinutes = it.filter(Char::isDigit).take(4) },
                    label = { Text("Minutes / day") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = costMoney,
                    onValueChange = { v -> costMoney = v.filter { it.isDigit() || it == '.' }.take(8) },
                    label = { Text("Money / day (৳)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }

            if (isAppLimit && apps.isEmpty()) {
                Text("Choose at least one app to track.", color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    onCreate(
                        name.ifBlank { defaultName },
                        category,
                        goalHours,
                        why,
                        type,
                        apps,
                        limitMinutes,
                        costMinutes.toIntOrNull() ?: 0,
                        costMoney.toDoubleOrNull() ?: 0.0,
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("🚀 Start challenge")
            }
        }
    }

    if (showPicker) {
        AppPickerDialog(
            initial = apps,
            onDismiss = { showPicker = false },
            onDone = {
                apps = it
                showPicker = false
            },
        )
    }
}

@Composable
private fun AppPickerDialog(initial: List<TrackedApp>, onDismiss: () -> Unit, onDone: (List<TrackedApp>) -> Unit) {
    val context = LocalContext.current.applicationContext
    val installed by produceState<List<TrackedApp>?>(null) {
        value = withContext(Dispatchers.IO) { UsageTracker.launchableApps(context) }
    }
    var selected by remember { mutableStateOf(initial.map { it.packageName }.toSet()) }
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose apps") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val list = installed
                if (list == null) {
                    Text("Loading apps…")
                } else {
                    val filtered = list.filter { it.label.contains(query.trim(), ignoreCase = true) }
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            val isOn = app.packageName in selected
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { selected = if (isOn) selected - app.packageName else selected + app.packageName },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = isOn, onCheckedChange = null)
                                Text(app.label, Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val all = (installed.orEmpty() + initial).distinctBy { it.packageName }
                onDone(all.filter { it.packageName in selected })
            }) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
