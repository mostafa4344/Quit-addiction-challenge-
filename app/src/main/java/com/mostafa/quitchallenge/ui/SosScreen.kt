@file:OptIn(ExperimentalMaterial3Api::class)

package com.mostafa.quitchallenge.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mostafa.quitchallenge.AppViewModel
import com.mostafa.quitchallenge.data.AppData
import com.mostafa.quitchallenge.data.Trigger
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private const val SURF_SECONDS = 5 * 60

private val GUIDANCE = listOf(
    "Notice where you feel the urge in your body: chest, stomach, hands?",
    "Don't fight it. Just watch it, like a wave in the sea.",
    "Breathe slowly: in for 4 seconds, hold for 7, out for 8.",
    "The urge is rising. It will peak, and then it will fall.",
    "You are not your urge. You can feel it without acting on it.",
    "Notice how it changes. Stronger, weaker, moving?",
    "Every urge you ride out makes the next one weaker.",
    "Remember why you started.",
)

private enum class SosStep { RATE, SURF, DONE }

/**
 * Urge surfing: rate the urge, ride it out with a guided timer, rate it again.
 * Research (Bowen & Marlatt 2009) found it changes how people respond to urges.
 */
@Composable
fun SosScreen(challengeId: String?, data: AppData, vm: AppViewModel, onClose: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(SosStep.RATE) }
    var before by rememberSaveable { mutableFloatStateOf(4f) }
    var after by rememberSaveable { mutableFloatStateOf(2f) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(SURF_SECONDS) }
    var triggers by remember { mutableStateOf(emptyList<Trigger>()) }
    val challenge = data.challenges.find { it.id == challengeId }
    val why = challenge?.why.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🆘 Ride out the urge") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") } },
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (step) {
                SosStep.RATE -> {
                    Text("You opened this instead of giving in. That already counts. 💪", textAlign = TextAlign.Center)
                    Text("How strong is the urge right now? ${before.toInt()}/5", fontWeight = FontWeight.Bold)
                    Slider(value = before, onValueChange = { before = it }, valueRange = 1f..5f, steps = 3)
                    Text(
                        "Next: a 5-minute guided \"urge surf\". You don't have to fight the urge, only watch it rise and fall.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = { step = SosStep.SURF }, modifier = Modifier.fillMaxWidth()) { Text("🌊 Start") }
                }

                SosStep.SURF -> {
                    LaunchedEffect(Unit) {
                        while (secondsLeft > 0) {
                            delay(1000)
                            secondsLeft--
                        }
                        step = SosStep.DONE
                    }
                    Wave(Modifier.fillMaxWidth().height(140.dp))
                    Text(
                        "%d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    val elapsed = SURF_SECONDS - secondsLeft
                    Text(
                        GUIDANCE[(elapsed / 20) % GUIDANCE.size],
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    if (why.isNotBlank()) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("❤️ Why you started", fontWeight = FontWeight.Bold)
                                Text(why)
                            }
                        }
                    }
                    if (data.plans.isNotEmpty()) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📝 Your plans", fontWeight = FontWeight.Bold)
                                data.plans.forEach { (t, plan) -> Text("${t.emoji} If ${t.label.lowercase()}: $plan") }
                            }
                        }
                    }
                    OutlinedButton(onClick = { step = SosStep.DONE }) { Text("The urge has passed") }
                }

                SosStep.DONE -> {
                    Text("🌅", style = MaterialTheme.typography.displayMedium)
                    Text("You rode it out!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("How strong is the urge now? ${after.toInt()}/5", fontWeight = FontWeight.Bold)
                    Slider(value = after, onValueChange = { after = it }, valueRange = 1f..5f, steps = 3)
                    Text("What pressure was it coming from?", fontWeight = FontWeight.Bold)
                    TriggerPicker(triggers) { triggers = it }
                    Button(
                        onClick = {
                            vm.logUrge(challengeId.orEmpty(), before.toInt(), triggers, "SOS urge surf", intensityAfter = after.toInt())
                            onClose()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (after < before) "Save: it dropped from ${before.toInt()} to ${after.toInt()} 🎉" else "Save and continue",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Wave(modifier: Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "phase",
    )
    // The wave slowly swells and settles, like an urge.
    val swell by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "swell",
    )
    Canvas(modifier) {
        val path = Path()
        val mid = size.height / 2
        val amp = size.height * 0.4f * swell
        path.moveTo(0f, mid)
        var x = 0f
        while (x <= size.width) {
            path.lineTo(x, mid + amp * sin(x / size.width * 4 * PI.toFloat() + phase))
            x += 4f
        }
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()
        drawPath(path, color.copy(alpha = 0.35f))
        drawCircle(color, radius = 6.dp.toPx(), center = Offset(size.width / 2, mid + amp * sin(2 * PI.toFloat() + phase)))
    }
}
