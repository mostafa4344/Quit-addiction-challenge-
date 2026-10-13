package com.mostafa.quitchallenge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mostafa.quitchallenge.ui.DetailScreen
import com.mostafa.quitchallenge.ui.HomeScreen
import com.mostafa.quitchallenge.ui.InsightsScreen
import com.mostafa.quitchallenge.ui.NewChallengeScreen
import com.mostafa.quitchallenge.ui.theme.QuitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuitTheme { App() }
        }
    }
}

private const val HOME = "home"
private const val INSIGHTS = "insights"
private const val NEW = "new"
private const val DETAIL = "detail:"

@Composable
fun App(vm: AppViewModel = viewModel()) {
    val data by vm.data.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf(HOME) }
    val isTab = route == HOME || route == INSIGHTS

    BackHandler(enabled = route != HOME) { route = HOME }

    Scaffold(
        bottomBar = {
            if (isTab) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == HOME,
                        onClick = { route = HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("চ্যালেঞ্জ") },
                    )
                    NavigationBarItem(
                        selected = route == INSIGHTS,
                        onClick = { route = INSIGHTS },
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text("প্রেশার বিশ্লেষণ") },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                route == HOME -> HomeScreen(
                    data = data,
                    onOpen = { route = DETAIL + it },
                    onNew = { route = NEW },
                )
                route == INSIGHTS -> InsightsScreen(data)
                route == NEW -> NewChallengeScreen(
                    onBack = { route = HOME },
                    onCreate = { name, category, goalHours, why ->
                        route = DETAIL + vm.createChallenge(name, category, goalHours, why)
                    },
                )
                route.startsWith(DETAIL) -> {
                    val id = route.removePrefix(DETAIL)
                    val challenge = data.challenges.find { it.id == id }
                    if (challenge == null) {
                        LaunchedEffect(id) { route = HOME }
                    } else {
                        DetailScreen(
                            challenge = challenge,
                            urges = data.urges.filter { it.challengeId == id },
                            vm = vm,
                            onBack = { route = HOME },
                        )
                    }
                }
            }
        }
    }
}
