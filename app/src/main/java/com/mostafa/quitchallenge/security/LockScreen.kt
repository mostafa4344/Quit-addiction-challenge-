package com.mostafa.quitchallenge.security

import com.mostafa.quitchallenge.ui.CenteredScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

/** Number pad used both to unlock the app and to set a new PIN. */
@Composable
fun PinPad(
    length: Int?,
    onSubmit: (String) -> Unit,
    extraKey: (@Composable () -> Unit)? = null,
) {
    var pin by remember { mutableStateOf("") }
    fun type(d: Char) {
        if (pin.length >= 8) return
        pin += d
        if (length != null && pin.length == length) {
            onSubmit(pin)
            pin = ""
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(length ?: maxOf(pin.length, 4)) { i ->
                Surface(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape),
                    color = if (i < pin.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ) {}
            }
        }
        Spacer(Modifier.height(8.dp))
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { d -> Key(d.toString()) { type(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                if (extraKey != null) {
                    extraKey()
                } else if (length == null) {
                    TextButton(onClick = { if (pin.length >= 4) onSubmit(pin).also { pin = "" } }) { Text("OK") }
                }
            }
            Key("0") { type('0') }
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                TextButton(onClick = { pin = pin.dropLast(1) }) { Text("⌫", style = MaterialTheme.typography.titleLarge) }
            }
        }
    }
}

@Composable
private fun Key(label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.size(72.dp), shape = CircleShape) {
        Text(label, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var message by remember { mutableStateOf("Enter your PIN") }
    val biometric = remember { AppLock.biometricEnabled(context) }

    LaunchedEffect(Unit) {
        if (biometric && activity != null) AppLock.promptBiometric(activity, onUnlocked)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        CenteredScroll {
            Text("🔒", style = MaterialTheme.typography.displayMedium)
            Text("Quit Challenge is locked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(message, textAlign = TextAlign.Center)
            PinPad(
                length = AppLock.pinLength(context),
                onSubmit = { pin ->
                    when (val r = AppLock.verify(context, pin)) {
                        AppLock.Result.Ok -> onUnlocked()
                        is AppLock.Result.Wrong -> message = "Wrong PIN. ${r.triesLeft} tries left."
                        is AppLock.Result.CoolingDown -> message = "Too many wrong tries. Wait ${r.secondsLeft} seconds."
                    }
                },
                extraKey = if (biometric && activity != null) {
                    { TextButton(onClick = { AppLock.promptBiometric(activity, onUnlocked) }) { Text("👆", style = MaterialTheme.typography.headlineSmall) } }
                } else {
                    null
                },
            )
            Text(
                "Forgot your PIN? Clearing this app's storage in Android Settings removes the lock, but also erases all your data. Export a backup first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
