package com.mostafa.quitchallenge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mostafa.quitchallenge.security.AppLock
import com.mostafa.quitchallenge.security.PinCheckDialog
import com.mostafa.quitchallenge.security.PinSetupDialog
import com.mostafa.quitchallenge.widget.ChallengeWidget

private enum class LockDialog { NONE, SET, CHANGE_CHECK, DISABLE_CHECK }

@Composable
fun AppLockCard() {
    val context = LocalContext.current
    // Bumped after every change so the card re-reads the lock state.
    var version by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf(LockDialog.NONE) }
    val enabled = remember(version) { AppLock.isEnabled(context) }
    val biometricAvailable = remember { AppLock.biometricAvailable(context) }
    val biometricOn = remember(version) { AppLock.biometricEnabled(context) }

    fun changed() {
        version++
        dialog = LockDialog.NONE
        ChallengeWidget.updateAll(context)
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🔒 App lock", fontWeight = FontWeight.Bold)
                    Text(
                        "Ask for a PIN every time the app is opened (and after 1 minute in the background), so nobody else can see your progress.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { dialog = if (it) LockDialog.SET else LockDialog.DISABLE_CHECK },
                )
            }
            if (enabled) {
                if (biometricAvailable) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👆 Unlock with fingerprint / face", Modifier.weight(1f))
                        Switch(checked = biometricOn, onCheckedChange = {
                            AppLock.setBiometric(context, it)
                            version++
                        })
                    }
                }
                TextButton(onClick = { dialog = LockDialog.CHANGE_CHECK }) { Text("Change PIN") }
            }
        }
    }

    when (dialog) {
        LockDialog.NONE -> Unit
        LockDialog.SET -> PinSetupDialog(onDismiss = { dialog = LockDialog.NONE }, onDone = { changed() })
        LockDialog.CHANGE_CHECK -> PinCheckDialog(
            title = "Change PIN",
            onDismiss = { dialog = LockDialog.NONE },
            onVerified = { dialog = LockDialog.SET },
        )
        LockDialog.DISABLE_CHECK -> PinCheckDialog(
            title = "Turn off app lock",
            onDismiss = { dialog = LockDialog.NONE },
            onVerified = {
                AppLock.disable(context)
                changed()
            },
        )
    }
}
