@file:OptIn(ExperimentalMaterial3Api::class)

package com.mostafa.quitchallenge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mostafa.quitchallenge.data.PRIVACY_EFFECTIVE_DATE
import com.mostafa.quitchallenge.data.PRIVACY_INTRO
import com.mostafa.quitchallenge.data.PRIVACY_POLICY_URL
import com.mostafa.quitchallenge.data.PRIVACY_SECTIONS

/** The full privacy policy, readable offline; the same text is published at [PRIVACY_POLICY_URL]. */
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Effective date: $PRIVACY_EFFECTIVE_DATE", style = MaterialTheme.typography.bodySmall)
            Text(PRIVACY_INTRO, style = MaterialTheme.typography.bodyMedium)
            PRIVACY_SECTIONS.forEach { (title, items) ->
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
                items.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
            Text(
                "Also available online: $PRIVACY_POLICY_URL",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
