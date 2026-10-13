package com.mostafa.quitchallenge.guard

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mostafa.quitchallenge.widget.ChallengeWidget
import java.util.concurrent.TimeUnit

/** Background fallback that checks time limits every 15 minutes, even with the guard service off. */
class LimitWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        LimitMonitor.check(applicationContext)
        ProtectionWatch.check(applicationContext)
        MilestoneNotifier.check(applicationContext)
        RiskReminder.check(applicationContext)
        // Keeps the home-screen widget's time fresh between app opens.
        ChallengeWidget.updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "limit_check",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<LimitWorker>(15, TimeUnit.MINUTES).build(),
            )
        }
    }
}
