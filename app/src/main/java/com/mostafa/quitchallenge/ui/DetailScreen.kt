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
import com.mostafa.quitchallenge.data.HOUR_MS
import com.mostafa.quitchallenge.data.Logic
import com.mostafa.quitchallenge.data.MILESTONES
import com.mostafa.quitchallenge.data.Trigger
import com.mostafa.quitchallenge.data.UrgeLog
import com.mostafa.quitchallenge.data.bn
import com.mostafa.quitchallenge.data.formatDate
import com.mostafa.quitchallenge.data.formatDuration
import com.mostafa.quitchallenge.data.formatGoal
import com.mostafa.quitchallenge.data.progressMessage
import com.mostafa.quitchallenge.data.restartMessage

private data class SupportMessage(val title: String, val body: String)

private fun tipsFor(triggers: List<Trigger>): String =
    triggers.joinToString("\n\n") { "${it.emoji} ${it.label}: ${it.tip}" }

@Composable
fun DetailScreen(challenge: Challenge, urges: List<UrgeLog>, vm: AppViewModel, onBack: () -> Unit) {
    val c = challenge
    val now = rememberNow()
    val elapsed = Logic.elapsedMs(c, now)
    val progress = Logic.progress(c, now)
    val goalReached = Logic.isGoalReached(c, now)

    var showUrge by remember { mutableStateOf(false) }
    var showRelapse by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var support by remember { mutableStateOf<SupportMessage?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(c.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "মুছে ফেলুন")
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
                        if (c.isActive) "${(progress * 100).toInt().bn()}%" else "✅",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (c.isActive) formatDuration(elapsed, showSeconds = true) else "চ্যালেঞ্জ সম্পূর্ণ",
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "লক্ষ্য: ${formatGoal(c.goalHours)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when {
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
                        "বাকি আছে: ${formatDuration(remaining)}",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { showUrge = true }, modifier = Modifier.weight(1f)) {
                            Text("⚡ ইচ্ছা হচ্ছে")
                        }
                        OutlinedButton(
                            onClick = { showRelapse = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text("❌ ফেইল করেছি")
                        }
                    }
                }
                else -> Button(onClick = { vm.restart(c.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("🔁 আবার নতুন করে শুরু করো")
                }
            }

            if (c.why.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("❤️ কেন শুরু করেছিলেন", fontWeight = FontWeight.Bold)
                        Text(c.why, Modifier.padding(top = 4.dp))
                    }
                }
            }

            MilestoneSection(c, elapsed)

            SectionTitle("📊 পরিসংখ্যান")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("বর্তমান চেষ্টা", "#${c.attemptNumber.bn()}", Modifier.weight(1f))
                StatBox("সম্পূর্ণ", c.completions.bn(), Modifier.weight(1f))
                StatBox("ফেইল", c.failures.bn(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("সেরা স্ট্রিক", formatDuration(Logic.bestStreakMs(c, now)), Modifier.weight(1f))
                StatBox("সামলানো ইচ্ছা", urges.size.bn(), Modifier.weight(1f))
            }

            if (c.attempts.isNotEmpty()) {
                SectionTitle("🗂 আগের চেষ্টাগুলো")
                c.attempts.withIndex().reversed().forEach { (i, a) ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val result = if (a.result == AttemptResult.COMPLETED) "✅ সম্পূর্ণ" else "❌ ফেইল"
                            Text("চেষ্টা #${(i + 1).bn()} — $result", fontWeight = FontWeight.Bold)
                            Text("টিকে ছিলেন: ${formatDuration(a.durationMs)}")
                            Text(formatDate(a.endedAt), style = MaterialTheme.typography.bodySmall)
                            if (a.triggers.isNotEmpty()) {
                                Text(
                                    "কারণ: " + a.triggers.joinToString(", ") { "${it.emoji} ${it.label}" },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (a.note.isNotBlank()) Text(a.note, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showUrge) {
        UrgeDialog(
            onDismiss = { showUrge = false },
            onSave = { intensity, triggers, note ->
                vm.logUrge(c.id, intensity, triggers, note)
                showUrge = false
                support = SupportMessage(
                    "আপনি পারবেন! 💪",
                    buildString {
                        append("ইচ্ছাটা সাধারণত ১৫–২০ মিনিটের মধ্যে কমে যায়। শুধু এই সময়টা পার করুন — পানি খান, জায়গা বদলান।")
                        if (c.why.isNotBlank()) append("\n\nমনে রাখুন কেন শুরু করেছিলেন:\n\"${c.why}\"")
                        if (triggers.isNotEmpty()) append("\n\n").append(tipsFor(triggers))
                    },
                )
            },
        )
    }

    if (showRelapse) {
        RelapseDialog(
            onDismiss = { showRelapse = false },
            onConfirm = { triggers, note ->
                vm.relapse(c.id, triggers, note)
                showRelapse = false
                support = SupportMessage(
                    "নতুন শুরু 🌅",
                    buildString {
                        append("আগের চেষ্টায় টিকে ছিলেন ${formatDuration(elapsed)}।\n\n")
                        append(restartMessage(c.attemptNumber))
                        if (triggers.isNotEmpty()) append("\n\nএবারের প্ল্যান:\n\n").append(tipsFor(triggers))
                    },
                )
            },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("চ্যালেঞ্জ মুছে ফেলবেন?") },
            text = { Text("এই চ্যালেঞ্জের সব চেষ্টা আর লগ মুছে যাবে। এটা ফেরত আনা যাবে না।") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    vm.delete(c.id)
                }) { Text("মুছে ফেলুন", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("না") } },
        )
    }

    support?.let { msg ->
        AlertDialog(
            onDismissRequest = { support = null },
            title = { Text(msg.title) },
            text = { Text(msg.body, Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { support = null }) { Text("ঠিক আছে") } },
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
            Text("অভিনন্দন! চ্যালেঞ্জ সম্পূর্ণ!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "আপনি ${formatGoal(c.goalHours)} আসক্তি থেকে দূরে থেকেছেন। এবার পরের লেভেল নিন — লক্ষ্য হবে ${formatGoal(Logic.nextLevelGoal(c.goalHours))}।",
                textAlign = TextAlign.Center,
            )
            Button(onClick = onLevelUp, modifier = Modifier.fillMaxWidth()) { Text("🔥 পরের লেভেল শুরু করো") }
            TextButton(onClick = onFinish) { Text("এখানেই শেষ করি") }
        }
    }
}

@Composable
private fun MilestoneSection(c: Challenge, elapsed: Long) {
    val relevant = MILESTONES.filter { it.hours <= maxOf(c.goalHours, 24) }
    if (relevant.isEmpty()) return
    SectionTitle("🎖 মাইলস্টোন")
    Logic.nextMilestone(elapsed)?.takeIf { c.isActive }?.let { next ->
        Text(
            "পরের ব্যাজ ${next.emoji} ${next.label} — আর ${formatDuration(next.hours * HOUR_MS - elapsed)}",
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
private fun UrgeDialog(onDismiss: () -> Unit, onSave: (Int, List<Trigger>, String) -> Unit) {
    var intensity by remember { mutableFloatStateOf(3f) }
    var triggers by remember { mutableStateOf(emptyList<Trigger>()) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚡ ইচ্ছা হচ্ছে?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ইচ্ছাটা কতটা তীব্র? ${intensity.toInt().bn()}/৫")
                Slider(value = intensity, onValueChange = { intensity = it }, valueRange = 1f..5f, steps = 3)
                Text("কোন প্রেশার থেকে আসছে?")
                TriggerPicker(triggers) { triggers = it }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = { Text("নোট (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(intensity.toInt(), triggers, note) }) { Text("লগ করো, আমি সামলাবো") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}

@Composable
private fun RelapseDialog(onDismiss: () -> Unit, onConfirm: (List<Trigger>, String) -> Unit) {
    var triggers by remember { mutableStateOf(emptyList<Trigger>()) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ফেইল করেছেন? সমস্যা নেই") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("সৎভাবে লিখুন কী হয়েছিল — এটা থেকেই পরের বারের প্ল্যান তৈরি হবে। চ্যালেঞ্জ আবার শূন্য থেকে শুরু হবে।")
                Text("কোন প্রেশারে হেরে গেলেন?")
                TriggerPicker(triggers) { triggers = it }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = { Text("কী হয়েছিল? (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(triggers, note) }) {
                Text("নতুন করে শুরু করো", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}
