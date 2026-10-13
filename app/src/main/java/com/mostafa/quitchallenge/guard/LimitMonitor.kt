package com.mostafa.quitchallenge.guard

import android.content.Context
import com.mostafa.quitchallenge.data.AppGraph
import com.mostafa.quitchallenge.data.Challenge
import com.mostafa.quitchallenge.data.ChallengeType
import com.mostafa.quitchallenge.data.Logic
import com.mostafa.quitchallenge.data.MINUTE_MS
import com.mostafa.quitchallenge.data.formatDuration
import com.mostafa.quitchallenge.data.formatMinutes

/** A daily limit that was just crossed; the challenge has already been restarted. */
data class LimitBreak(val challenge: Challenge, val usedMs: Long)

/**
 * Checks every running app-limit challenge against today's usage: warns near the limit,
 * and fails + restarts the challenge once the limit is crossed.
 */
object LimitMonitor {
    private const val PREFS = "limit_alerts"

    @Synchronized
    fun check(context: Context, now: Long = System.currentTimeMillis()): List<LimitBreak> {
        if (!UsageTracker.hasUsageAccess(context)) return emptyList()
        val repo = AppGraph.repo(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val breaks = mutableListOf<LimitBreak>()

        repo.data.value.challenges
            .filter { it.type == ChallengeType.APP_LIMIT && it.isActive && !Logic.isGoalReached(it, now) }
            .forEach { c ->
                val from = Logic.usageWindowStart(c, now)
                val used = UsageTracker.usageByPackage(context, c.trackedApps.map { it.packageName }.toSet(), from, now)
                    .values.sum()
                val limitMs = c.dailyLimitMinutes * MINUTE_MS
                val apps = c.trackedApps.joinToString(", ") { it.label }

                if (Logic.isOverLimit(c, used)) {
                    repo.relapse(c.id, emptyList(), "Daily limit crossed: used ${formatDuration(used)} of ${formatMinutes(c.dailyLimitMinutes)} on $apps", now)
                    Notifier.show(
                        context,
                        c.id.hashCode(),
                        "⛔ Time limit crossed: ${c.name}",
                        "You used $apps for ${formatDuration(used)} today (limit ${formatMinutes(c.dailyLimitMinutes)}). " +
                            "The challenge restarts now. Close the app and take a break, you can do this! 💪",
                    )
                    breaks += LimitBreak(c, used)
                } else if (limitMs > 0 && used >= limitMs * 8 / 10) {
                    val key = "warn_${c.id}_$from"
                    if (!prefs.getBoolean(key, false)) {
                        prefs.edit().putBoolean(key, true).apply()
                        Notifier.show(
                            context,
                            c.id.hashCode(),
                            "⏳ Take a break: ${c.name}",
                            "Only ${formatDuration(limitMs - used)} left today on $apps. Put the phone down for a while.",
                        )
                    }
                }
            }
        return breaks
    }
}
