package com.mostafa.quitchallenge

import com.mostafa.quitchallenge.data.localCurrencySymbol
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mostafa.quitchallenge.data.AppData
import com.mostafa.quitchallenge.data.AppGraph
import com.mostafa.quitchallenge.data.Category
import com.mostafa.quitchallenge.data.Challenge
import com.mostafa.quitchallenge.data.ChallengeType
import com.mostafa.quitchallenge.data.TrackedApp
import com.mostafa.quitchallenge.data.Trigger
import com.mostafa.quitchallenge.data.MAX_CHECK_INS_PER_DAY
import com.mostafa.quitchallenge.guard.CheckInAlarm
import com.mostafa.quitchallenge.guard.SupportWorker
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppGraph.repo(app)
    val data: StateFlow<AppData> = repo.data

    fun createChallenge(
        name: String,
        category: Category,
        goalHours: Int,
        why: String,
        type: ChallengeType,
        apps: List<TrackedApp>,
        dailyLimitMinutes: Int,
        minutesPerDay: Int = 0,
        moneyPerDay: Double = 0.0,
        startAt: Long = System.currentTimeMillis(),
    ): String {
        val now = System.currentTimeMillis()
        val challenge = Challenge(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            category = category,
            goalHours = goalHours,
            why = why.trim(),
            createdAt = now,
            currentStart = maxOf(startAt, now),
            type = type,
            trackedApps = if (type == ChallengeType.APP_LIMIT) apps else emptyList(),
            dailyLimitMinutes = dailyLimitMinutes,
            minutesPerDay = minutesPerDay,
            moneyPerDay = moneyPerDay,
            currency = localCurrencySymbol(),
        )
        repo.update { it.copy(challenges = it.challenges + challenge) }
        return challenge.id
    }

    fun logUrge(challengeId: String, intensity: Int, triggers: List<Trigger>, note: String, intensityAfter: Int? = null) =
        repo.logUrge(challengeId, intensity, triggers, note, intensityAfter = intensityAfter)

    fun setPlan(trigger: Trigger, plan: String) = repo.setPlan(trigger, plan)

    fun relapse(challengeId: String, triggers: List<Trigger>, note: String, warningSigns: String, plan: String) {
        val now = System.currentTimeMillis()
        repo.relapse(challengeId, triggers, note, now, warningSigns, plan)
        // A plan written right after a slip becomes the standing plan for those pressures, unless one exists.
        if (plan.isNotBlank()) {
            val existing = repo.data.value.plans
            triggers.filter { it !in existing }.forEach { repo.setPlan(it, plan) }
        }
        SupportWorker.scheduleAfterSlip(getApplication(), challengeId, now)
    }

    fun completeGoal(challengeId: String, levelUp: Boolean) = repo.completeGoal(challengeId, levelUp)

    fun restart(challengeId: String) {
        repo.updateChallenge(challengeId) { it.copy(currentStart = System.currentTimeMillis()) }
    }

    fun delete(challengeId: String) {
        repo.update { d ->
            d.copy(
                challenges = d.challenges.filterNot { it.id == challengeId },
                urges = d.urges.filterNot { it.challengeId == challengeId },
            )
        }
    }

    fun checkIn(challengeId: String) = repo.checkIn(challengeId)

    fun setDailyCheckIn(enabled: Boolean) {
        repo.update { it.copy(settings = it.settings.copy(dailyCheckIn = enabled)) }
        CheckInAlarm.schedule(getApplication())
    }

    fun setCheckInTimes(times: List<Int>) {
        val clean = times.map { it.coerceIn(0, 24 * 60 - 1) }.distinct().sorted().take(MAX_CHECK_INS_PER_DAY)
        repo.update { it.copy(settings = it.settings.copy(checkInTimes = clean)) }
        CheckInAlarm.schedule(getApplication())
    }
}
