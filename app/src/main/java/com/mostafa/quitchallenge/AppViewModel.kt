package com.mostafa.quitchallenge

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mostafa.quitchallenge.data.AppData
import com.mostafa.quitchallenge.data.AppGraph
import com.mostafa.quitchallenge.data.Category
import com.mostafa.quitchallenge.data.Challenge
import com.mostafa.quitchallenge.data.ChallengeType
import com.mostafa.quitchallenge.data.TrackedApp
import com.mostafa.quitchallenge.data.Trigger
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
    ): String {
        val now = System.currentTimeMillis()
        val challenge = Challenge(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            category = category,
            goalHours = goalHours,
            why = why.trim(),
            createdAt = now,
            currentStart = now,
            type = type,
            trackedApps = if (type == ChallengeType.APP_LIMIT) apps else emptyList(),
            dailyLimitMinutes = dailyLimitMinutes,
        )
        repo.update { it.copy(challenges = it.challenges + challenge) }
        return challenge.id
    }

    fun logUrge(challengeId: String, intensity: Int, triggers: List<Trigger>, note: String) =
        repo.logUrge(challengeId, intensity, triggers, note)

    fun relapse(challengeId: String, triggers: List<Trigger>, note: String) = repo.relapse(challengeId, triggers, note)

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

    fun setKeywordGuard(enabled: Boolean) {
        repo.update { it.copy(settings = it.settings.copy(keywordGuard = enabled)) }
    }

    fun addKeyword(keyword: String) {
        val kw = keyword.trim()
        if (kw.isEmpty()) return
        repo.update { d ->
            if (d.settings.keywords.any { it.equals(kw, ignoreCase = true) }) d
            else d.copy(settings = d.settings.copy(keywords = d.settings.keywords + kw))
        }
    }

    fun removeKeyword(keyword: String) {
        repo.update { it.copy(settings = it.settings.copy(keywords = it.settings.keywords - keyword)) }
    }
}
