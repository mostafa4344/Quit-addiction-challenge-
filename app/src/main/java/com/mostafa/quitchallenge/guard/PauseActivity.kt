package com.mostafa.quitchallenge.guard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mostafa.quitchallenge.data.AppGraph
import com.mostafa.quitchallenge.data.ChallengeType
import com.mostafa.quitchallenge.data.Logic
import com.mostafa.quitchallenge.data.formatDuration
import com.mostafa.quitchallenge.data.formatMinutes
import com.mostafa.quitchallenge.data.openStats
import com.mostafa.quitchallenge.ui.theme.QuitTheme
import kotlinx.coroutines.delay

/**
 * A few seconds of breathing before a time-limited app opens, then a real choice.
 * Research on the "one sec" app found the option to back out is what cuts app openings.
 */
class PauseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val now = System.currentTimeMillis()
        val data = AppGraph.repo(this).data.value
        val challenge = data.challenges.firstOrNull { c ->
            c.isActive && c.type == ChallengeType.APP_LIMIT && c.trackedApps.any { it.packageName == pkg }
        }
        if (challenge == null) {
            finish()
            return
        }
        val label = challenge.trackedApps.firstOrNull { it.packageName == pkg }?.label ?: "this app"
        val windowStart = Logic.usageWindowStart(challenge, now)
        val usedToday = UsageTracker.usageByPackage(this, challenge.trackedApps.map { it.packageName }.toSet(), windowStart, now)
            .values.sum()
        val (opened, skipped) = openStats(data.openAttempts, setOf(pkg), windowStart)

        setContent {
            QuitTheme {
                BackHandler { skip(pkg) }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var secondsLeft by remember { mutableIntStateOf(PAUSE_SECONDS) }
                    LaunchedEffect(Unit) {
                        while (secondsLeft > 0) {
                            delay(1000)
                            secondsLeft--
                        }
                    }
                    val breath = rememberInfiniteTransition(label = "breath")
                    val scale by breath.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
                        label = "scale",
                    )
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    ) {
                        Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .size(180.dp)
                                    .scale(scale)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                            )
                            Text(if (scale < 0.8f) "Breathe in" else "Breathe out", fontWeight = FontWeight.Bold)
                        }
                        Text("Do you really want to open $label?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                        Text(
                            "Today: ${formatDuration(usedToday)} of your ${formatMinutes(challenge.dailyLimitMinutes)} limit · " +
                                "opened $opened times, skipped $skipped",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        if (challenge.why.isNotBlank()) {
                            Text("❤️ \"${challenge.why}\"", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        }
                        Button(onClick = { skip(pkg) }, modifier = Modifier.fillMaxWidth()) {
                            Text("💪 I don't need it, close $label")
                        }
                        TextButton(onClick = { open(pkg) }, enabled = secondsLeft == 0) {
                            Text(if (secondsLeft > 0) "Open $label in $secondsLeft…" else "Open $label anyway")
                        }
                    }
                }
            }
        }
    }

    private fun skip(pkg: String) {
        AppGraph.repo(this).logOpenAttempt(pkg, skipped = true)
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun open(pkg: String) {
        AppGraph.repo(this).logOpenAttempt(pkg, skipped = false)
        GuardService.allowFor(pkg)
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val PAUSE_SECONDS = 5

        fun show(context: Context, packageName: String) {
            context.startActivity(
                Intent(context, PauseActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(EXTRA_PACKAGE, packageName),
            )
        }
    }
}
