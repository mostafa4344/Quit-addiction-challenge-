package com.mostafa.quitchallenge.guard

import android.content.Context
import com.mostafa.quitchallenge.data.AppGraph
import com.mostafa.quitchallenge.data.DayPart
import com.mostafa.quitchallenge.data.HOUR_MS
import com.mostafa.quitchallenge.data.Logic
import com.mostafa.quitchallenge.data.computeInsights
import com.mostafa.quitchallenge.data.dayPartOf
import com.mostafa.quitchallenge.data.formatGoal
import java.time.LocalDate
import java.time.ZoneId

/** Celebrates new badges and reached goals, even when the app isn't open. */
object MilestoneNotifier {
    private const val PREFS = "milestone_notified"

    fun check(context: Context, now: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        AppGraph.repo(context).data.value.challenges.filter { it.isActive }.forEach { c ->
            val elapsed = Logic.elapsedMs(c, now)
            val attemptKey = "${c.id}_${c.currentStart}"
            val lastHours = prefs.getInt(attemptKey, 0)
            if (Logic.isGoalReached(c, now)) {
                if (lastHours != Int.MAX_VALUE) {
                    prefs.edit().putInt(attemptKey, Int.MAX_VALUE).apply()
                    Notifier.show(
                        context,
                        c.id.hashCode() + 2,
                        "🏆 Goal reached: ${c.name}",
                        "You did it: ${formatGoal(c.goalHours)}! Open the app to claim your reward and start the next level.",
                    )
                }
                return@forEach
            }
            val newest = Logic.reachedMilestones(elapsed).lastOrNull() ?: return@forEach
            if (newest.hours > lastHours) {
                prefs.edit().putInt(attemptKey, newest.hours).apply()
                // Don't announce a badge reached long ago, e.g. right after installing an update.
                if (elapsed - newest.hours * HOUR_MS < 6 * HOUR_MS) {
                    Notifier.show(
                        context,
                        c.id.hashCode() + 2,
                        "${newest.emoji} New badge: ${newest.label}!",
                        "${newest.label} strong in \"${c.name}\". Keep the streak alive!",
                    )
                }
            }
        }
    }
}

/** Reminds the user of their own plan shortly before the time of day they slip or get urges most. */
object RiskReminder {
    private const val PREFS = "risk_reminder"
    private const val MIN_EVENTS = 3

    private fun startHour(part: DayPart): Int = when (part) {
        DayPart.MORNING -> 5
        DayPart.AFTERNOON -> 12
        DayPart.EVENING -> 17
        DayPart.NIGHT -> 21
    }

    fun check(context: Context, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()) {
        val data = AppGraph.repo(context).data.value
        if (data.challenges.none { it.isActive }) return
        val insights = computeInsights(data, now, zone)
        val (risky, count) = insights.dayPartCounts.maxByOrNull { it.second } ?: return
        if (count < MIN_EVENTS) return

        // Fire within the hour before the risky part of the day begins.
        val nowTime = java.time.Instant.ofEpochMilli(now).atZone(zone)
        val hoursUntil = (startHour(risky) - nowTime.hour + 24) % 24
        if (hoursUntil != 1 || dayPartOf(now, zone) == risky) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now(zone).toString()
        if (prefs.getString("last", null) == today) return
        prefs.edit().putString("last", today).apply()

        val topTrigger = insights.triggerCounts.firstOrNull()?.first
        val plan = topTrigger?.let { data.plans[it] }
        val body = when {
            plan != null -> "Your risky time (${risky.label}) starts soon. Your plan for ${topTrigger.label.lowercase()}: $plan"
            topTrigger != null -> "Your risky time (${risky.label}) starts soon. ${topTrigger.emoji} ${topTrigger.tip}"
            else -> "Your risky time (${risky.label}) starts soon. Plan something else to do now."
        }
        Notifier.show(context, "risk".hashCode(), "⚠️ Heads up: risky time ahead", body)
    }
}
