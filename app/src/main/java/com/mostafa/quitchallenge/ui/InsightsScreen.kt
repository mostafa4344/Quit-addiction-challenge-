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
import com.mostafa.quitchallenge.data.bn
import com.mostafa.quitchallenge.data.computeInsights
import com.mostafa.quitchallenge.data.formatDuration

@Composable
fun InsightsScreen(data: AppData) {
    val now = rememberNow(60_000L)
    val insights = computeInsights(data, now)

    Scaffold(topBar = { TopAppBar(title = { Text("প্রেশার বিশ্লেষণ 📊") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("সম্পূর্ণ চ্যালেঞ্জ", insights.completions.bn(), Modifier.weight(1f))
                StatBox("ফেইল", insights.failures.bn(), Modifier.weight(1f))
                StatBox("সফলতার হার", "${insights.successRate.bn()}%", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("সামলানো ইচ্ছা", insights.urgesHandled.bn(), Modifier.weight(1f))
                StatBox("মোট আসক্তিমুক্ত সময়", formatDuration(insights.totalCleanMs), Modifier.weight(2f))
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
                        "যখনই ইচ্ছা হবে বা ফেইল করবেন, কোন প্রেশার থেকে হলো সেটা লগ করুন। " +
                            "তাহলে এখানে দেখা যাবে কোন প্রেশার আপনাকে সবচেয়ে বেশি টানে আর কখন — সেই অনুযায়ী প্ল্যান বানানো যাবে।",
                        textAlign = TextAlign.Center,
                    )
                }
                return@Column
            }

            if (insights.triggerCounts.isNotEmpty()) {
                SectionTitle("🎯 কোন প্রেশার সবচেয়ে বেশি টানে")
                val max = insights.triggerCounts.first().second
                insights.triggerCounts.forEach { (t, n) ->
                    BarRow("${t.emoji} ${t.label}", n, max, MaterialTheme.colorScheme.secondary)
                }
            }

            SectionTitle("🕒 কোন সময়ে ঝুঁকি বেশি")
            val maxPart = insights.dayPartCounts.maxOf { it.second }
            insights.dayPartCounts.forEach { (part, n) ->
                BarRow(part.label, n, maxPart, MaterialTheme.colorScheme.primary)
            }
            insights.dayPartCounts.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.let { (part, _) ->
                Text(
                    "⚠️ আপনার সবচেয়ে ঝুঁকির সময়: ${part.label}। এই সময়ে আগে থেকেই অন্য কাজ ঠিক করে রাখুন।",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            val top = insights.triggerCounts.take(3)
            if (top.isNotEmpty()) {
                SectionTitle("📝 আপনার প্ল্যান")
                top.forEach { (t, _) ->
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("${t.emoji} যখন ${t.label} হবে", fontWeight = FontWeight.Bold)
                            Text(t.tip, Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
