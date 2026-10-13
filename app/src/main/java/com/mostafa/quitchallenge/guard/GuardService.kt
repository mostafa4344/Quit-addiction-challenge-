package com.mostafa.quitchallenge.guard

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.mostafa.quitchallenge.data.AppGraph
import com.mostafa.quitchallenge.data.ChallengeType
import com.mostafa.quitchallenge.data.findKeyword
import com.mostafa.quitchallenge.data.formatDuration
import com.mostafa.quitchallenge.data.formatMinutes
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Watches which app is open and what is typed, entirely on the device.
 * Typed text is only matched against the keyword list and never stored.
 */
class GuardService : AccessibilityService() {
    private val worker = Executors.newSingleThreadScheduledExecutor()
    private var lastLimitCheck = 0L
    private var lastKeyword: String? = null
    private var lastKeywordAt = 0L
    private var lastAppPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                onAppOpened()
                maybePause(pkg)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> if (!event.isPassword) onTextTyped(event.text.joinToString(" "))
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Usage grows while the user stays inside one app, so keep checking every minute.
        worker.scheduleWithFixedDelay({ checkLimits() }, 1, 1, TimeUnit.MINUTES)
    }

    /** Shows the breathing pause when a time-limited app comes to the front from somewhere else. */
    private fun maybePause(pkg: String) {
        if (pkg in IGNORED_PACKAGES || pkg == currentKeyboard()) return
        val previous = lastAppPackage
        lastAppPackage = pkg
        if (pkg == previous) return
        val now = System.currentTimeMillis()
        if ((allowedUntil[pkg] ?: 0L) > now) return
        val data = AppGraph.repo(this).data.value
        if (!data.settings.pauseBeforeOpen) return
        val limited = data.challenges.any { c ->
            c.isActive && c.type == ChallengeType.APP_LIMIT && c.trackedApps.any { it.packageName == pkg }
        }
        if (limited) PauseActivity.show(this, pkg)
    }

    private fun currentKeyboard(): String? =
        Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)?.substringBefore('/')

    private fun onAppOpened() {
        val now = System.currentTimeMillis()
        if (now - lastLimitCheck < 15_000) return
        lastLimitCheck = now
        worker.execute { checkLimits() }
    }

    /** Usage only grows while a tracked app is on screen, so a fresh break means the user is in it right now. */
    private fun checkLimits() {
        val crossed = try {
            LimitMonitor.check(this).firstOrNull()
        } catch (e: Exception) {
            null
        } ?: return
        val c = crossed.challenge
        WarningActivity.show(
            this,
            title = "⛔ Time limit crossed",
            message = "You've used ${c.trackedApps.joinToString(", ") { it.label }} for ${formatDuration(crossed.usedMs)} today. " +
                "Your limit is ${formatMinutes(c.dailyLimitMinutes)}.\n\n\"${c.name}\" restarts now. Time for a break?",
            keyword = null,
        )
    }

    private fun onTextTyped(text: String) {
        val settings = AppGraph.repo(this).data.value.settings
        if (!settings.keywordGuard || text.isBlank()) return
        val keyword = findKeyword(text, settings.keywords) ?: return
        val now = System.currentTimeMillis()
        if (keyword == lastKeyword && now - lastKeywordAt < 30_000) return
        lastKeyword = keyword
        lastKeywordAt = now
        WarningActivity.show(
            this,
            title = "⚠️ Are you sure?",
            message = "You have tried to search \"$keyword\".\n\nRemember why you started. Do you really want to continue?",
            keyword = keyword,
        )
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    companion object {
        /** Our own screens and system overlays don't count as "leaving" an app. */
        private val IGNORED_PACKAGES = setOf("com.mostafa.quitchallenge", "com.android.systemui")

        /** After "Open anyway", don't pause the same app again for a few minutes. */
        private val allowedUntil = java.util.concurrent.ConcurrentHashMap<String, Long>()

        fun allowFor(pkg: String, minutes: Long = 3) {
            allowedUntil[pkg] = System.currentTimeMillis() + minutes * 60_000
        }

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
            val me = ComponentName(context, GuardService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }

        fun settingsIntent(): Intent =
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
