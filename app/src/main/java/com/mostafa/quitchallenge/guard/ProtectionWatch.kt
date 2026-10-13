package com.mostafa.quitchallenge.guard

import android.content.Context
import com.mostafa.quitchallenge.data.AppGraph
import com.mostafa.quitchallenge.data.ChallengeType

/** Warns once when a protection the user had turned on gets switched off (by the user, an update or the system). */
object ProtectionWatch {
    private const val PREFS = "protection_watch"

    fun check(context: Context) {
        val data = AppGraph.repo(context).data.value
        val needsUsage = data.challenges.any { it.isActive && it.type == ChallengeType.APP_LIMIT }
        val needsGuard = needsUsage || data.settings.keywordGuard
        watch(context, "usage", needsUsage, UsageTracker.hasUsageAccess(context), "Usage access")
        watch(context, "guard", needsGuard, GuardService.isEnabled(context), "Quit Challenge Guard (Accessibility)")
    }

    private fun watch(context: Context, key: String, needed: Boolean, on: Boolean, name: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wasOn = prefs.getBoolean(key, false)
        if (on != wasOn) prefs.edit().putBoolean(key, on).apply()
        if (wasOn && !on && needed) {
            Notifier.show(
                context,
                key.hashCode(),
                "⚠️ Protection turned off",
                "$name was switched off, so your challenges aren't fully protected. Open Quit Challenge → Protection to turn it back on.",
            )
        }
    }
}
