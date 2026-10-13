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
import com.mostafa.quitchallenge.data.AppData
import com.mostafa.quitchallenge.data.computeInsights
import com.mostafa.quitchallenge.data.formatDuration

@Composable
fun InsightsScreen(data: AppData) {
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
                StatBox("Completed", "${insights.completions}", Modifier.weight(1f))
                StatBox("Failed", "${insights.failures}", Modifier.weight(1f))
                StatBox("Success rate", "${insights.successRate}%", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Urges beaten", "${insights.urgesHandled}", Modifier.weight(1f))
                StatBox("Keyword alerts", "${insights.keywordAlerts}", Modifier.weight(1f))
                StatBox("Total clean time", formatDuration(insights.totalCleanMs), Modifier.weight(1f))
            }

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
            insights.dayPartCounts.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.let { (part, _) ->
                Text(
                    "⚠️ Your riskiest time: ${part.label}. Plan something else to do at this time in advance.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            val top = insights.triggerCounts.take(3)
            if (top.isNotEmpty()) {
                SectionTitle("📝 Your plan")
                top.forEach { (t, _) ->
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("${t.emoji} When you feel ${t.label.lowercase()}", fontWeight = FontWeight.Bold)
                            Text(t.tip, Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
