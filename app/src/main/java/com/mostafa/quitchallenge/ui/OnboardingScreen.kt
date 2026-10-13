package com.mostafa.quitchallenge.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

object Onboarding {
    private const val PREFS = "onboarding"
    private const val KEY_DONE = "done"

    fun isDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)

    fun markDone(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DONE, true).apply()
    }
}

/** First-launch walkthrough: what the app does, then permissions, then the app lock. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pages = 3

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LinearProgressIndicator(progress = { (page + 1) / pages.toFloat() }, modifier = Modifier.fillMaxWidth())
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (page) {
                    0 -> Welcome()
                    1 -> {
                        Text("Set up protection", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Turn these on one by one. Each button opens the right Android settings page; come back here when done. " +
                                "You can skip any of them and turn them on later in the Protection tab.",
                        )
                        PermissionsList()
                    }
                    else -> {
                        Text("Keep it private", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Optional: lock the app with a PIN or your fingerprint so nobody else can open it.")
                        AppLockCard()
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (page > 0) TextButton(onClick = { page-- }) { Text("Back") }
                Text("", Modifier.weight(1f))
                if (page < pages - 1) {
                    TextButton(onClick = onFinish) { Text("Skip all") }
                    Button(onClick = { page++ }) { Text("Next") }
                } else {
                    Button(onClick = onFinish) { Text("Start using the app") }
                }
            }
        }
    }
}

@Composable
private fun Welcome() {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("🔥", style = MaterialTheme.typography.displayLarge)
        Text("Welcome to Quit Challenge", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            "Turn quitting into a challenge you can win, one day at a time.",
            textAlign = TextAlign.Center,
        )
    }
    Feature("⏱", "Challenges", "Stay away for a number of days, or keep apps like Facebook and YouTube under a daily time limit.")
    Feature("⚡", "Help in hard moments", "Log an urge and get instant tips and your own reasons for quitting.")
    Feature("🌊", "Slips aren't the end", "A slip restarts the streak, but your total clean time and best streak always stay.")
    Feature("📊", "Know your pressures", "See which pressures and times of day pull you most, and plan for them.")
    Feature("🛡", "Protection", "Break reminders, time limits and a keyword guard that asks \"Are you sure?\".")
    Text(
        "Everything stays on your phone. No account, no internet.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Feature(emoji: String, title: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(emoji, style = MaterialTheme.typography.headlineSmall)
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
