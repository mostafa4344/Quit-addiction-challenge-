package com.mostafa.quitchallenge.guard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mostafa.quitchallenge.MainActivity
import com.mostafa.quitchallenge.data.dailyQuote
import com.mostafa.quitchallenge.ui.theme.QuitTheme

/**
 * Full-screen break / time-limit screen shown over whatever app is open.
 * Needs the "Display over other apps" permission, which lets the app open it from the background.
 */
class WarningActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val emoji = intent.getStringExtra(EXTRA_EMOJI) ?: "🛑"
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        val challengeId = intent.getStringExtra(EXTRA_CHALLENGE_ID)

        setContent {
            QuitTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(emoji, style = MaterialTheme.typography.displayLarge)
                        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        Text(
                            "💡 ${dailyQuote()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = ::goHome, modifier = Modifier.fillMaxWidth()) {
                            Text("💪 Close it and take a break")
                        }
                        OutlinedButton(onClick = { openSos(challengeId) }, modifier = Modifier.fillMaxWidth()) {
                            Text("🆘 Help me ride out this urge")
                        }
                        TextButton(onClick = ::finish, modifier = Modifier.fillMaxWidth()) {
                            Text("Continue anyway")
                        }
                    }
                }
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun openSos(challengeId: String?) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_OPEN_SOS, true)
                .putExtra(MainActivity.EXTRA_CHALLENGE_ID, challengeId),
        )
        finish()
    }

    companion object {
        private const val EXTRA_EMOJI = "emoji"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_CHALLENGE_ID = "challenge_id"

        fun canShow(context: Context): Boolean = Settings.canDrawOverlays(context)

        /** Shows the screen over other apps; does nothing without the overlay permission. */
        fun show(context: Context, emoji: String, title: String, message: String, challengeId: String?) {
            if (!canShow(context)) return
            context.startActivity(
                Intent(context, WarningActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(EXTRA_EMOJI, emoji)
                    .putExtra(EXTRA_TITLE, title)
                    .putExtra(EXTRA_MESSAGE, message)
                    .putExtra(EXTRA_CHALLENGE_ID, challengeId),
            )
        }
    }
}
