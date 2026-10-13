package com.mostafa.quitchallenge.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.mostafa.quitchallenge.MainActivity
import com.mostafa.quitchallenge.R
import com.mostafa.quitchallenge.data.AppData
import com.mostafa.quitchallenge.data.AppGraph
import com.mostafa.quitchallenge.data.Challenge
import com.mostafa.quitchallenge.data.Logic
import com.mostafa.quitchallenge.data.formatDuration
import com.mostafa.quitchallenge.data.formatGoal
import com.mostafa.quitchallenge.security.AppLock

/** Home-screen widget showing the newest running challenge. */
class ChallengeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateAll(context)
    }

    companion object {
        /** The challenge shown on the widget: the newest one that is still running. */
        fun featured(data: AppData): Challenge? =
            data.challenges.filter { it.isActive }.maxByOrNull { it.createdAt }

        fun updateAll(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(app, ChallengeWidget::class.java))
            if (ids.isEmpty()) return
            val views = render(app, AppGraph.repo(app).data.value, System.currentTimeMillis())
            manager.updateAppWidget(ids, views)
        }

        private fun render(context: Context, data: AppData, now: Long): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_challenge)
            val open = PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)

            val c = featured(data)
            if (c == null) {
                views.setTextViewText(R.id.widget_title, "Quit Challenge")
                views.setTextViewText(R.id.widget_days, "🎯")
                views.setTextViewText(R.id.widget_elapsed, "No running challenge")
                views.setTextViewText(R.id.widget_goal, "Tap to start one")
                views.setViewVisibility(R.id.widget_progress, View.GONE)
                return views
            }

            if (!Logic.hasStarted(c, now)) {
                views.setTextViewText(R.id.widget_title, if (AppLock.isEnabled(context)) "🔥 My challenge" else "${c.category.emoji} ${c.name}")
                views.setTextViewText(R.id.widget_days, "Starts soon")
                views.setTextViewText(R.id.widget_elapsed, "in ${formatDuration((c.currentStart ?: now) - now)}")
                views.setTextViewText(R.id.widget_goal, "Goal: ${formatGoal(c.goalHours)}")
                views.setViewVisibility(R.id.widget_progress, View.GONE)
                return views
            }
            val elapsed = Logic.elapsedMs(c, now)
            val days = elapsed / 86_400_000L
            val progress = (Logic.progress(c, now) * 100).toInt()
            // With the app lock on, don't reveal what the challenge is about on the home screen.
            val title = if (AppLock.isEnabled(context)) "🔥 My challenge" else "${c.category.emoji} ${c.name}"
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_days, if (days == 1L) "Day 1 done" else "Day ${days + 1}")
            views.setTextViewText(R.id.widget_elapsed, formatDuration(elapsed))
            views.setTextViewText(
                R.id.widget_goal,
                if (Logic.isGoalReached(c, now)) "🏆 Goal reached! Tap to claim" else "$progress% of ${formatGoal(c.goalHours)}",
            )
            views.setViewVisibility(R.id.widget_progress, View.VISIBLE)
            views.setProgressBar(R.id.widget_progress, 100, progress, false)
            return views
        }
    }
}
