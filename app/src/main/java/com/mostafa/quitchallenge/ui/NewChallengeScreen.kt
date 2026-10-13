@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.mostafa.quitchallenge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mostafa.quitchallenge.data.Category
import com.mostafa.quitchallenge.data.bn
import com.mostafa.quitchallenge.data.formatGoal

private val PRESET_DAYS = listOf(1, 3, 7, 21, 30, 90)

@Composable
fun NewChallengeScreen(
    onBack: () -> Unit,
    onCreate: (name: String, category: Category, goalHours: Int, why: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Category.SOCIAL_MEDIA) }
    var days by rememberSaveable { mutableStateOf("7") }
    var hours by rememberSaveable { mutableStateOf("0") }
    var why by rememberSaveable { mutableStateOf("") }

    val goalHours = (days.toIntOrNull() ?: 0) * 24 + (hours.toIntOrNull() ?: 0)
    val defaultName = "চ্যালেঞ্জ: ${category.label} ছাড়া ${formatGoal(goalHours.coerceAtLeast(1))}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("নতুন চ্যালেঞ্জ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle("কোন আসক্তি থেকে মুক্তি চান?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text("${cat.emoji} ${cat.label}") },
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = { Text("চ্যালেঞ্জের নাম") },
                placeholder = { Text(defaultName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("কতদিন / কত ঘণ্টা দূরে থাকবেন?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESET_DAYS.forEach { d ->
                    FilterChip(
                        selected = days == d.toString() && (hours.toIntOrNull() ?: 0) == 0,
                        onClick = {
                            days = d.toString()
                            hours = "0"
                        },
                        label = { Text("${d.bn()} দিন") },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it.filter(Char::isDigit).take(4) },
                    label = { Text("দিন") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it.filter(Char::isDigit).take(3) },
                    label = { Text("ঘণ্টা") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                if (goalHours > 0) "মোট লক্ষ্য: ${formatGoal(goalHours)}" else "অন্তত ১ ঘণ্টার লক্ষ্য দিন",
                color = if (goalHours > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )

            SectionTitle("কেন ছাড়তে চান?")
            OutlinedTextField(
                value = why,
                onValueChange = { why = it.take(300) },
                label = { Text("আপনার কারণ (কঠিন সময়ে এটা দেখানো হবে)") },
                placeholder = { Text("যেমন: পড়াশোনায় মন দিতে চাই, পরিবারকে সময় দিতে চাই") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { onCreate(name.ifBlank { defaultName }, category, goalHours, why) },
                enabled = goalHours > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("🚀 চ্যালেঞ্জ শুরু করো")
            }
        }
    }
}
