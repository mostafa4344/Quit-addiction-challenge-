package com.mostafa.quitchallenge.guard

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mostafa.quitchallenge.data.AppGraph
import com.mostafa.quitchallenge.data.UrgeSource
import com.mostafa.quitchallenge.data.dailyQuote
import com.mostafa.quitchallenge.ui.theme.QuitTheme

/** Full-screen "are you sure?" interruption shown over other apps by the guard service. */
class WarningActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        val keyword = intent.getStringExtra(EXTRA_KEYWORD)

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
                        Text("🛑", style = MaterialTheme.typography.displayLarge)
                        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        Text(
                            "💡 ${dailyQuote()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = { goBack(keyword) }, modifier = Modifier.fillMaxWidth()) {
                            Text("💪 Go back, stay strong")
                        }
                        OutlinedButton(onClick = { openSos(keyword) }, modifier = Modifier.fillMaxWidth()) {
                            Text("🆘 Help me ride out this urge")
                        }
                        androidx.compose.material3.TextButton(onClick = { proceed(keyword) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Continue anyway")
                        }
                    }
                }
            }
        }
    }

    private fun goBack(keyword: String?) {
        if (keyword != null) {
            AppGraph.repo(this).logUrge("", 3, emptyList(), "Searched: $keyword", UrgeSource.KEYWORD, resisted = true)
        }
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun openSos(keyword: String?) {
        if (keyword != null) {
            AppGraph.repo(this).logUrge("", 3, emptyList(), "Searched: $keyword", UrgeSource.KEYWORD, resisted = true)
        }
        startActivity(
            Intent(this, com.mostafa.quitchallenge.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(com.mostafa.quitchallenge.MainActivity.EXTRA_OPEN_SOS, true),
        )
        finish()
    }

    private fun proceed(keyword: String?) {
        if (keyword != null) {
            AppGraph.repo(this).logUrge("", 3, emptyList(), "Searched: $keyword", UrgeSource.KEYWORD, resisted = false)
        }
        finish()
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_KEYWORD = "keyword"

        fun show(context: Context, title: String, message: String, keyword: String?) {
            context.startActivity(
                Intent(context, WarningActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(EXTRA_TITLE, title)
                    .putExtra(EXTRA_MESSAGE, message)
                    .putExtra(EXTRA_KEYWORD, keyword),
            )
        }
    }
}
