package com.mostafa.quitchallenge.guard

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mostafa.quitchallenge.data.AppGraph
import com.mostafa.quitchallenge.data.Challenge
import com.mostafa.quitchallenge.data.ChallengeType
import com.mostafa.quitchallenge.data.Logic
import com.mostafa.quitchallenge.data.dayNumber
import com.mostafa.quitchallenge.data.formatDuration
import com.mostafa.quitchallenge.data.HOUR_MS
import com.mostafa.quitchallenge.data.dueCheckInSlots
import com.mostafa.quitchallenge.data.nextCheckInAt
import java.time.LocalDate

/**
 * Once a day asks "Did you stay on track today?" for every self-reported challenge.
 * The user answers right from the notification; no scanning or special permission needed.
 */
object CheckIn {
    const val ACTION_ON_TRACK = "com.mostafa.quitchallenge.CHECK_IN_ON_TRACK"
    const val ACTION_SLIPPED = "com.mostafa.quitchallenge.CHECK_IN_SLIPPED"
    const val EXTRA_ID = "challenge_id"
    private const val PREFS = "check_in"

    fun notificationId(challengeId: String): Int = challengeId.hashCode() + 3

    fun check(context: Context, now: Long = System.currentTimeMillis()) {
        val data = AppGraph.repo(context).data.value
        val settings = data.settings
        if (!settings.dailyCheckIn) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "sent_${LocalDate.now()}"
        val sentToday = prefs.getStringSet(key, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        val due = dueCheckInSlots(now, settings.checkInTimes, sentToday)
        if (due.isEmpty()) return
        // If several times were missed (phone off), send one notification and mark them all as done.
        prefs.edit()
            .clear()
            .putStringSet(key, (sentToday + due).map { it.toString() }.toSet())
            .apply()

        data.challenges
            .filter { it.type == ChallengeType.TIMER && Logic.hasStarted(it, now) && !Logic.isGoalReached(it, now) }
            // Skip a challenge the user confirmed less than an hour ago.
            .filter { c -> c.lastCheckIn == null || now - c.lastCheckIn > HOUR_MS }
            .forEach { ask(context, it, now) }
    }

    private fun ask(context: Context, c: Challenge, now: Long) {
        Notifier.show(
            context,
            notificationId(c.id),
            "📅 Daily check-in: ${c.name}",
            "Day ${dayNumber(c, now)} (${formatDuration(Logic.elapsedMs(c, now))}). Are you still on track? Be honest; a slip only restarts the streak.",
            actions = listOf(
                "✅ Yes, on track" to action(context, c.id, ACTION_ON_TRACK),
                "🌊 I slipped" to action(context, c.id, ACTION_SLIPPED),
            ),
        )
    }

    private fun action(context: Context, id: String, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            (id + action).hashCode(),
            Intent(context, CheckInReceiver::class.java).setAction(action).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}

/** Handles the two buttons on the daily check-in notification. */
class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(CheckIn.EXTRA_ID) ?: return
        val repo = AppGraph.repo(context)
        val now = System.currentTimeMillis()
        val c = repo.data.value.challenges.find { it.id == id } ?: return
        when (intent.action) {
            CheckIn.ACTION_ON_TRACK -> {
                repo.checkIn(id, now)
                Notifier.show(
                    context,
                    CheckIn.notificationId(id),
                    "✅ Day ${dayNumber(c, now)} done. Well done!",
                    "\"${c.name}\" continues. Every honest day counts 💪",
                )
            }
            CheckIn.ACTION_SLIPPED -> {
                val totalBefore = Logic.totalCleanMs(c, now)
                repo.relapse(id, emptyList(), "Reported in the daily check-in", now)
                repo.checkIn(id, now)
                SupportWorker.scheduleAfterSlip(context, id, now)
                Notifier.show(
                    context,
                    CheckIn.notificationId(id),
                    "🌅 Fresh start: ${c.name}",
                    "Your streak restarted, but your ${formatDuration(totalBefore)} of total clean time stays. " +
                        "Open the app to note what happened and plan for next time.",
                )
            }
        }
    }
}

/**
 * Wakes the app at each chosen check-in time, so notifications arrive on time instead of
 * waiting for the 15-minute background check. Inexact alarms need no special permission.
 */
object CheckInAlarm {
    fun schedule(context: Context) {
        val app = context.applicationContext
        val alarms = app.getSystemService(android.app.AlarmManager::class.java) ?: return
        val intent = PendingIntent.getBroadcast(
            app,
            0,
            Intent(app, CheckInAlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val settings = AppGraph.repo(app).data.value.settings
        val next = if (settings.dailyCheckIn) nextCheckInAt(System.currentTimeMillis(), settings.checkInTimes) else null
        if (next == null) {
            alarms.cancel(intent)
        } else {
            alarms.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, next, intent)
        }
    }
}

/** Fires at a check-in time (and after reboot): sends due check-ins and schedules the next alarm. */
class CheckInAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CheckIn.check(context)
        CheckInAlarm.schedule(context)
    }
}
