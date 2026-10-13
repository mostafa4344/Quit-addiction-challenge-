@file:OptIn(ExperimentalMaterial3Api::class)

package com.mostafa.quitchallenge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.mostafa.quitchallenge.data.Challenge
import com.mostafa.quitchallenge.data.ChallengeType
import com.mostafa.quitchallenge.data.Logic
import com.mostafa.quitchallenge.data.dailyQuote
import com.mostafa.quitchallenge.data.formatDuration
import com.mostafa.quitchallenge.data.formatGoal
import com.mostafa.quitchallenge.data.formatMinutes

@Composable
fun HomeScreen(data: AppData, onOpen: (String) -> Unit, onNew: () -> Unit) {
    val now = rememberNow()
    val sorted = data.challenges.sortedWith(
        compareByDescending<Challenge> { it.isActive }.thenByDescending { it.createdAt },
    )
    Scaffold(
        topBar = { TopAppBar(title = { Text("Quit Challenge 🔥") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New challenge") },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("💡 Today's motivation", fontWeight = FontWeight.Bold)
                        Text(dailyQuote(), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (sorted.isEmpty()) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🎯", style = MaterialTheme.typography.displayMedium)
                        Text(
                            "No challenges yet",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            "Tap the button below to start your first challenge: choose how many days or hours " +
                                "you'll stay away, or set a daily time limit for apps like Facebook or YouTube.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            items(sorted, key = { it.id }) { c ->
                ChallengeCard(c, now, onClick = { onOpen(c.id) })
            }
        }
    }
}

@Composable
private fun ChallengeCard(c: Challenge, now: Long, onClick: () -> Unit) {
    val progress = Logic.progress(c, now)
    val reached = Logic.isGoalReached(c, now)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${c.category.emoji} ${c.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text("Attempt #${c.attemptNumber}", style = MaterialTheme.typography.labelMedium)
            }
            if (c.type == ChallengeType.APP_LIMIT) {
                Text(
                    "📲 ${formatMinutes(c.dailyLimitMinutes)} / day on ${c.trackedApps.joinToString(", ") { it.label }}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            when {
                !c.isActive -> Text("✅ Completed. Goal was ${formatGoal(c.goalHours)}")
                reached -> Text(
                    "🏆 Goal reached! Tap to claim your reward",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
                else -> {
                    Text(formatDuration(Logic.elapsedMs(c, now), showSeconds = true))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "Goal: ${formatGoal(c.goalHours)} • ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
