package com.mostafa.quitchallenge

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import com.mostafa.quitchallenge.security.AppLock
import com.mostafa.quitchallenge.security.LockScreen
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mostafa.quitchallenge.guard.LimitMonitor
import com.mostafa.quitchallenge.guard.LimitWorker
import com.mostafa.quitchallenge.guard.Notifier
import com.mostafa.quitchallenge.ui.DetailScreen
import com.mostafa.quitchallenge.ui.HomeScreen
import com.mostafa.quitchallenge.ui.InsightsScreen
import com.mostafa.quitchallenge.ui.NewChallengeScreen
import com.mostafa.quitchallenge.ui.Onboarding
import com.mostafa.quitchallenge.ui.OnboardingScreen
import com.mostafa.quitchallenge.ui.ProtectionScreen
import com.mostafa.quitchallenge.ui.theme.QuitTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    private var locked by mutableStateOf(false)
    private var backgroundedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifier.createChannel(this)
        LimitWorker.schedule(this)
        locked = AppLock.isEnabled(this) && savedInstanceState?.getBoolean(KEY_UNLOCKED) != true
        setContent {
            QuitTheme {
                if (locked) LockScreen(onUnlocked = { locked = false }) else App()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val lockOn = AppLock.isEnabled(this)
        // Keep app content out of the recent-apps preview while the lock is on.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(!lockOn)
        if (lockOn && backgroundedAt > 0 && SystemClock.elapsedRealtime() - backgroundedAt > AppLock.RELOCK_AFTER_MS) {
            locked = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Survive rotation without asking for the PIN again.
        outState.putBoolean(KEY_UNLOCKED, !locked)
    }

    override fun onStop() {
        super.onStop()
        backgroundedAt = SystemClock.elapsedRealtime()
    }
}

private const val KEY_UNLOCKED = "unlocked"
private const val HOME = "home"
private const val INSIGHTS = "insights"
private const val PROTECT = "protect"
private const val NEW = "new"
private const val DETAIL = "detail:"

@Composable
fun App(vm: AppViewModel = viewModel()) {
    val data by vm.data.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf(HOME) }
    val isTab = route == HOME || route == INSIGHTS || route == PROTECT
    val context = LocalContext.current

    // Keep app-limit challenges up to date while the app is open.
    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) { LimitMonitor.check(context.applicationContext) }
            delay(60_000)
        }
    }

    var onboarded by rememberSaveable { mutableStateOf(Onboarding.isDone(context)) }
    if (!onboarded) {
        OnboardingScreen(onFinish = {
            Onboarding.markDone(context)
            onboarded = true
        })
        return
    }

    BackHandler(enabled = route != HOME) { route = HOME }

    Scaffold(
        bottomBar = {
            if (isTab) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == HOME,
                        onClick = { route = HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Challenges") },
                    )
                    NavigationBarItem(
                        selected = route == INSIGHTS,
                        onClick = { route = INSIGHTS },
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text("Insights") },
                    )
                    NavigationBarItem(
                        selected = route == PROTECT,
                        onClick = { route = PROTECT },
                        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        label = { Text("Protection") },
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
                route == PROTECT -> ProtectionScreen(data, vm)
                route == NEW -> NewChallengeScreen(
                    onBack = { route = HOME },
                    onCreate = { name, category, goalHours, why, type, apps, limit ->
                        route = DETAIL + vm.createChallenge(name, category, goalHours, why, type, apps, limit)
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
