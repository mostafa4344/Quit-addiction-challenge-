@file:OptIn(ExperimentalMaterial3Api::class)

package com.mostafa.quitchallenge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mostafa.quitchallenge.AppViewModel
import com.mostafa.quitchallenge.data.AppData
import com.mostafa.quitchallenge.data.Trigger
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mostafa.quitchallenge.data.computeInsights
import com.mostafa.quitchallenge.data.riskHeatmap
import com.mostafa.quitchallenge.data.riskSummary
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.mostafa.quitchallenge.data.formatDuration

@Composable
fun InsightsScreen(data: AppData, vm: AppViewModel) {
    val now = rememberNow(60_000L)
    val insights = computeInsights(data, now)

    Scaffold(topBar = { TopAppBar(title = { Text("Pressure insights 📊") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Goals reached", "${insights.completions}", Modifier.weight(1f))
                StatBox("Slips", "${insights.failures}", Modifier.weight(1f))
                StatBox("Success rate", "${insights.successRate}%", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Urges beaten", "${insights.urgesHandled}", Modifier.weight(1f))
                StatBox("Keyword alerts", "${insights.keywordAlerts}", Modifier.weight(1f))
                StatBox("Total clean time", formatDuration(insights.totalCleanMs), Modifier.weight(1f))
            }

            PlansSection(data, insights.triggerCounts.take(3).map { it.first }, vm)

            if (!insights.hasData) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🔍", style = MaterialTheme.typography.displayMedium)
                    Text(
                        "Whenever you feel an urge or slip, log which pressure caused it. " +
                            "This screen will then show which pressures pull you the most and when, so you can plan ahead.",
                        textAlign = TextAlign.Center,
                    )
                }
                return@Column
            }

            if (insights.triggerCounts.isNotEmpty()) {
                SectionTitle("🎯 Pressures that pull you the most")
                val max = insights.triggerCounts.first().second
                insights.triggerCounts.forEach { (t, n) ->
                    BarRow("${t.emoji} ${t.label}", "$n×", n / max.toFloat(), MaterialTheme.colorScheme.secondary)
                }
            }

            SectionTitle("🕒 When you're most at risk")
            val maxPart = insights.dayPartCounts.maxOf { it.second }
            insights.dayPartCounts.forEach { (part, n) ->
                BarRow(part.label, "$n×", if (maxPart == 0) 0f else n / maxPart.toFloat(), MaterialTheme.colorScheme.primary)
            }
            RiskHeatmap(riskHeatmap(data))
            riskSummary(riskHeatmap(data), insights)?.let {
                Text("🧭 $it", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            insights.dayPartCounts.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.let { (part, _) ->
                Text(
                    "⚠️ Your riskiest time: ${part.label}. Plan something else to do at this time in advance.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

        }
    }
}

/**
 * The user's own if-then plans. Research on implementation intentions shows that
 * "if X happens, then I will Y" plans work best when they're the person's own and get rehearsed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlansSection(data: AppData, topTriggers: List<Trigger>, vm: AppViewModel) {
    var editing by remember { mutableStateOf<Trigger?>(null) }
    var picking by remember { mutableStateOf(false) }
    val shown = (topTriggers + data.plans.keys).distinct()

    SectionTitle("📝 My if-then plans")
    Text(
        "Decide in advance what you'll do when a pressure hits. Your plans show up in SOS mode and before your risky time of day.",
        style = MaterialTheme.typography.bodySmall,
    )
    shown.forEach { t ->
        val plan = data.plans[t]
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (plan != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${t.emoji} If I feel ${t.label.lowercase()}…", fontWeight = FontWeight.Bold)
                if (plan != null) {
                    Text("…then I will $plan")
                } else {
                    Text("Idea: ${t.tip}", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { editing = t }) { Text(if (plan != null) "Edit plan" else "Write my plan") }
            }
        }
    }
    OutlinedButton(onClick = { picking = true }) { Text("➕ Add a plan for another pressure") }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("Which pressure?") },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Trigger.entries.forEach { t ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                picking = false
                                editing = t
                            },
                            label = { Text("${t.emoji} ${t.label}") },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        )
    }

    editing?.let { t ->
        var text by remember(t) { mutableStateOf(data.plans[t].orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("${t.emoji} If I feel ${t.label.lowercase()}…") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(200) },
                        label = { Text("…then I will") },
                        placeholder = { Text(t.tip) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Make it specific: what, where, for how long.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setPlan(t, text)
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}

/** Weekday × part-of-day grid; darker cells mean more urges and slips. */
@Composable
private fun RiskHeatmap(grid: List<List<Int>>) {
    val max = grid.maxOf { row -> row.max() }
    val color = MaterialTheme.colorScheme.secondary
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val parts = listOf("🌅", "☀️", "🌇", "🌙")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("", Modifier.width(40.dp))
            parts.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center) }
        }
        grid.forEachIndexed { d, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(days[d], Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall)
                row.forEach { n ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (n == 0 || max == 0) MaterialTheme.colorScheme.surfaceVariant
                                else color.copy(alpha = 0.25f + 0.75f * n / max),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (n > 0) Text("$n", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
