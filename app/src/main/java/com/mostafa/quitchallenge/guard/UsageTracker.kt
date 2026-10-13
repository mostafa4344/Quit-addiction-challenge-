package com.mostafa.quitchallenge.guard

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.mostafa.quitchallenge.data.TrackedApp
import com.mostafa.quitchallenge.data.UsageEvent
import com.mostafa.quitchallenge.data.foregroundTimeByPackage

/** Reads per-app screen time from Android's usage stats (the same data Digital Wellbeing uses). */
object UsageTracker {
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Foreground time per package between [from] and [to]; empty when usage access is missing. */
    fun usageByPackage(context: Context, packages: Set<String>, from: Long, to: Long): Map<String, Long> {
        if (packages.isEmpty() || from >= to || !hasUsageAccess(context)) return emptyMap()
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val raw = usm.queryEvents(from, to) ?: return emptyMap()
        val events = mutableListOf<UsageEvent>()
        val e = UsageEvents.Event()
        while (raw.hasNextEvent()) {
            raw.getNextEvent(e)
            val pkg = e.packageName ?: continue
            if (pkg !in packages) continue
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> events += UsageEvent(pkg, e.className.orEmpty(), e.timeStamp, true)
                UsageEvents.Event.ACTIVITY_PAUSED -> events += UsageEvent(pkg, e.className.orEmpty(), e.timeStamp, false)
            }
        }
        return foregroundTimeByPackage(events, packages, from, to)
    }

    fun launchableApps(context: Context): List<TrackedApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        return pm.queryIntentActivities(intent, 0)
            .map { TrackedApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
