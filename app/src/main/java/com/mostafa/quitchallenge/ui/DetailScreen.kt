@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.mostafa.quitchallenge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mostafa.quitchallenge.AppViewModel
import com.mostafa.quitchallenge.data.AttemptResult
import com.mostafa.quitchallenge.data.Challenge
import com.mostafa.quitchallenge.data.ChallengeType
import com.mostafa.quitchallenge.data.MINUTE_MS
import com.mostafa.quitchallenge.data.formatMinutes
import com.mostafa.quitchallenge.data.formatMoney
import com.mostafa.quitchallenge.data.checkedInToday
import com.mostafa.quitchallenge.guard.UsageTracker
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.platform.LocalContext
import com.mostafa.quitchallenge.data.HOUR_MS
import com.mostafa.quitchallenge.data.Logic
import com.mostafa.quitchallenge.data.MILESTONES
import com.mostafa.quitchallenge.data.Trigger
import com.mostafa.quitchallenge.data.UrgeLog
import com.mostafa.quitchallenge.data.formatDate
import com.mostafa.quitchallenge.data.formatDuration
import com.mostafa.quitchallenge.data.formatGoal
import com.mostafa.quitchallenge.data.progressMessage
import com.mostafa.quitchallenge.data.restartMessage

private data class SupportMessage(val title: String, val body: String)

private fun tipsFor(triggers: List<Trigger>): String =
    triggers.joinToString("\n\n") { "${it.emoji} ${it.label}: ${it.tip}" }

@Composable
fun DetailScreen(challenge: Challenge, urges: List<UrgeLog>, vm: AppViewModel, onBack: () -> Unit, onSos: () -> Unit) {
    val c = challenge
    val now = rememberNow()
    val elapsed = Logic.elapsedMs(c, now)
    val progress = Logic.progress(c, now)
    val goalReached = Logic.isGoalReached(c, now)

    var showRelapse by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var support by remember { mutableStateOf<SupportMessage?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(c.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
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
            // Big progress ring with the live timer.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (c.isActive) progress else 1f },
                    modifier = Modifier.size(240.dp),
                    strokeWidth = 14.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text(
                        if (c.isActive) "${(progress * 100).toInt()}%" else "✅",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (c.isActive) formatDuration(elapsed, showSeconds = true) else "Challenge completed",
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Goal: ${formatGoal(c.goalHours)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (c.type == ChallengeType.APP_LIMIT && c.isActive) AppUsageCard(c)

            when {
                c.isActive && !Logic.hasStarted(c, now) -> {
                    Text(
                        "⏳ Your challenge starts in ${formatDuration((c.currentStart ?: now) - now, showSeconds = true)}",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Use the time to prepare: remove triggers, write your if-then plans in Insights, and tell someone you trust.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(onClick = { vm.restart(c.id) }, modifier = Modifier.fillMaxWidth()) { Text("Start right now instead") }
                }
                goalReached -> GoalReachedCard(c, onLevelUp = { vm.completeGoal(c.id, levelUp = true) }, onFinish = {
                    vm.completeGoal(c.id, levelUp = false)
                })
                c.isActive -> {
                    Text(
                        progressMessage(progress),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val remaining = Logic.goalMs(c) - elapsed
                    Text(
                        "Remaining: ${formatDuration(remaining)}",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onSos, modifier = Modifier.weight(1f)) {
                            Text("🆘 I feel an urge")
                        }
                        OutlinedButton(
                            onClick = { showRelapse = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text("🌊 I slipped")
                        }
                    }
                    if (c.type == ChallengeType.TIMER) {
                        if (checkedInToday(c, now)) {
                            Text(
                                "✅ Checked in today. See you tomorrow!",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            TextButton(onClick = { vm.checkIn(c.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text("📅 Check in: I stayed on track today")
                            }
                        }
                    }
                }
                else -> Button(onClick = { vm.restart(c.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("🔁 Start again")
                }
            }

            if (c.why.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("❤️ Why you started", fontWeight = FontWeight.Bold)
                        Text(c.why, Modifier.padding(top = 4.dp))
                    }
                }
            }

            MilestoneSection(c, elapsed)

            SectionTitle("📊 Stats")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Total clean time", formatDuration(Logic.totalCleanMs(c, now)), Modifier.weight(1f))
                StatBox("Best streak", formatDuration(Logic.bestStreakMs(c, now)), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val (clean, counted) = Logic.cleanDaysInLast30(c, now)
                StatBox("Slip-free days (last 30)", "$clean / $counted", Modifier.weight(1f))
                StatBox("Urges beaten", "${urges.count { it.resisted }}", Modifier.weight(1f))
            }
            if (c.minutesPerDay > 0 || c.moneyPerDay > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (c.minutesPerDay > 0) {
                        StatBox("Time won back", formatDuration(Logic.savedMinutes(c, now) * MINUTE_MS), Modifier.weight(1f))
                    }
                    if (c.moneyPerDay > 0) {
                        StatBox("Money saved", formatMoney(Logic.savedMoney(c, now), c.currency), Modifier.weight(1f))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Current attempt", "#${c.attemptNumber}", Modifier.weight(1f))
                StatBox("Goals reached", "${c.completions}", Modifier.weight(1f))
                StatBox("Slips", "${c.failures}", Modifier.weight(1f))
            }
            Text(
                "A slip only restarts the current streak. Your total clean time and best streak always stay.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (c.attempts.isNotEmpty()) {
                SectionTitle("🗂 Previous attempts")
                c.attempts.withIndex().reversed().forEach { (i, a) ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val result = if (a.result == AttemptResult.COMPLETED) "✅ Goal reached" else "🔁 Slipped, then restarted"
                            Text("Attempt #${i + 1}: $result", fontWeight = FontWeight.Bold)
                            Text("Lasted: ${formatDuration(a.durationMs)}")
                            Text(formatDate(a.endedAt), style = MaterialTheme.typography.bodySmall)
                            if (a.triggers.isNotEmpty()) {
                                Text(
                                    "Pressure: " + a.triggers.joinToString(", ") { "${it.emoji} ${it.label}" },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (a.note.isNotBlank()) Text(a.note, style = MaterialTheme.typography.bodySmall)
                            if (a.warningSigns.isNotBlank()) {
                                Text("Warning signs: ${a.warningSigns}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (a.plan.isNotBlank()) {
                                Text("Plan: ${a.plan}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRelapse) {
        RelapseDialog(
            onDismiss = { showRelapse = false },
            onConfirm = { triggers, note, warningSigns, plan ->
                val totalBefore = Logic.totalCleanMs(c, System.currentTimeMillis())
                vm.relapse(c.id, triggers, note, warningSigns, plan)
                showRelapse = false
                support = SupportMessage(
                    "A fresh start 🌅",
                    buildString {
                        append("This streak lasted ${formatDuration(elapsed)}. ")
                        append("Your total clean time of ${formatDuration(totalBefore)} is still yours. One slip doesn't erase it.\n\n")
                        append(restartMessage(c.attemptNumber))
                        if (plan.isNotBlank()) append("\n\nYour plan:\n\"$plan\"")
                        if (triggers.isNotEmpty()) append("\n\nIdeas that help:\n\n").append(tipsFor(triggers))
                    },
                )
            },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this challenge?") },
            text = { Text("All attempts and logs of this challenge will be deleted. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    vm.delete(c.id)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }

    support?.let { msg ->
        AlertDialog(
            onDismissRequest = { support = null },
            title = { Text(msg.title) },
            text = { Text(msg.body, Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { support = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun GoalReachedCard(c: Challenge, onLevelUp: () -> Unit, onFinish: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("🏆", style = MaterialTheme.typography.displayMedium)
            Text("Congratulations! Challenge complete!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "You stayed strong for ${formatGoal(c.goalHours)}. Take the next level: the goal becomes ${formatGoal(Logic.nextLevelGoal(c.goalHours))}.",
                textAlign = TextAlign.Center,
            )
            Button(onClick = onLevelUp, modifier = Modifier.fillMaxWidth()) { Text("🔥 Start next level") }
            TextButton(onClick = onFinish) { Text("Finish here") }
        }
    }
}

@Composable
private fun MilestoneSection(c: Challenge, elapsed: Long) {
    val relevant = MILESTONES.filter { it.hours <= maxOf(c.goalHours, 24) }
    if (relevant.isEmpty()) return
    SectionTitle("🎖 Milestones")
    Logic.nextMilestone(elapsed)?.takeIf { c.isActive }?.let { next ->
        Text(
            "Next badge ${next.emoji} ${next.label} in ${formatDuration(next.hours * HOUR_MS - elapsed)}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        relevant.forEach { m ->
            val reached = !c.isActive || m.hours * HOUR_MS <= elapsed
            AssistChip(
                onClick = {},
                label = { Text("${m.emoji} ${m.label}") },
                modifier = Modifier.alpha(if (reached) 1f else 0.35f),
            )
        }
    }
}

@Composable
private fun RelapseDialog(onDismiss: () -> Unit, onConfirm: (List<Trigger>, String, String, String) -> Unit) {
    var triggers by remember { mutableStateOf(emptyList<Trigger>()) }
    var note by remember { mutableStateOf("") }
    var warningSigns by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Slipped? That's okay") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NoAutoKeyboard()
                Text(
                    "A slip is a lesson, not the end. Only the current streak restarts; your total clean time and best streak stay. " +
                        "Three quick questions turn this into a better plan:",
                )
                Text("1. Which pressure got you?", fontWeight = FontWeight.Bold)
                TriggerPicker(triggers) { triggers = it }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = { Text("What happened? (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("2. What warning signs came before it?", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = warningSigns,
                    onValueChange = { warningSigns = it.take(200) },
                    placeholder = { Text("e.g. I was in bed with my phone after midnight") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("3. Next time this happens, I will…", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = plan,
                    onValueChange = { plan = it.take(200) },
                    placeholder = { Text("e.g. put the phone in another room and read") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(triggers, note, warningSigns, plan) }) {
                Text("Restart my streak")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AppUsageCard(c: Challenge) {
    val context = LocalContext.current
    val usage by rememberTodayUsage(c)
    val used = usage.values.sum()
    val limitMs = c.dailyLimitMinutes * MINUTE_MS
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📲 Today's app time", fontWeight = FontWeight.Bold)
            if (!UsageTracker.hasUsageAccess(context)) {
                Text("Allow \"Usage access\" so the app can measure your screen time, like Digital Wellbeing does.")
                Button(onClick = { openSettings(context, UsageTracker.usageAccessSettingsIntent()) }) {
                    Text("Grant usage access")
                }
                return@Column
            }
            Text("${formatDuration(used)} used of ${formatMinutes(c.dailyLimitMinutes)} limit")
            LinearProgressIndicator(
                progress = { if (limitMs == 0L) (if (used > 0) 1f else 0f) else (used.toFloat() / limitMs).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (limitMs > 0 && used >= limitMs * 8 / 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            c.trackedApps.forEach { app ->
                Text("• ${app.label}: ${formatDuration(usage[app.packageName] ?: 0L)}", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "Cross the limit and this challenge restarts automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
