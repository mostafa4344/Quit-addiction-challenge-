package com.mostafa.quitchallenge.guard

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mostafa.quitchallenge.data.AppGraph
import java.util.concurrent.TimeUnit

/**
 * The hours right after a slip carry the highest risk of giving up entirely,
 * so send one encouraging note a few hours later if the new streak is still going.
 */
class SupportWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.success()
        val start = inputData.getLong(KEY_START, -1L)
        val c = AppGraph.repo(applicationContext).data.value.challenges.find { it.id == id } ?: return Result.success()
        if (c.currentStart != start) return Result.success()
        Notifier.show(
            applicationContext,
            id.hashCode() + 1,
            "💪 $HOURS hours strong again",
            "You've stayed on track for $HOURS hours since your slip in \"${c.name}\". That's exactly how comebacks are built. Keep going!",
        )
        return Result.success()
    }

    companion object {
        private const val KEY_ID = "challenge_id"
        private const val KEY_START = "start"
        private const val HOURS = 6L

        fun scheduleAfterSlip(context: Context, challengeId: String, newStart: Long) {
            val request = OneTimeWorkRequestBuilder<SupportWorker>()
                .setInitialDelay(HOURS, TimeUnit.HOURS)
                .setInputData(workDataOf(KEY_ID to challengeId, KEY_START to newStart))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("support_$challengeId", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
