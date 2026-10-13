package com.mostafa.quitchallenge.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Asks for a new PIN twice. */
@Composable
fun PinSetupDialog(onDismiss: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    var first by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("Choose a 4–8 digit PIN, then tap OK") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (first == null) "Set a PIN" else "Confirm your PIN") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                PinPad(
                    length = first?.length,
                    onSubmit = { pin ->
                        val firstPin = first
                        when {
                            firstPin == null && PinHasher.isValidPin(pin) -> {
                                first = pin
                                message = "Enter the same PIN again"
                            }
                            firstPin == null -> message = "The PIN must be 4–8 digits"
                            pin == firstPin -> {
                                AppLock.setPin(context, pin)
                                onDone()
                            }
                            else -> {
                                first = null
                                message = "PINs didn't match. Start again."
                            }
                        }
                    },
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Asks for the current PIN before turning the lock off. */
@Composable
fun PinCheckDialog(title: String, onDismiss: () -> Unit, onVerified: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("Enter your current PIN") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                PinPad(
                    length = AppLock.pinLength(context),
                    onSubmit = { pin ->
                        when (val r = AppLock.verify(context, pin)) {
                            AppLock.Result.Ok -> onVerified()
                            is AppLock.Result.Wrong -> message = "Wrong PIN. ${r.triesLeft} tries left."
                            is AppLock.Result.CoolingDown -> message = "Too many wrong tries. Wait ${r.secondsLeft} seconds."
                        }
                    },
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
