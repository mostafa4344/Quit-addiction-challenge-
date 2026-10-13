package com.mostafa.quitchallenge

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mostafa.quitchallenge.data.AppData
import com.mostafa.quitchallenge.data.Attempt
import com.mostafa.quitchallenge.data.AttemptResult
import com.mostafa.quitchallenge.data.Category
import com.mostafa.quitchallenge.data.Challenge
import com.mostafa.quitchallenge.data.Logic
import com.mostafa.quitchallenge.data.Repository
import com.mostafa.quitchallenge.data.Trigger
import com.mostafa.quitchallenge.data.UrgeLog
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(File(app.filesDir, "data.json"))
    val data: StateFlow<AppData> = repo.data

    fun createChallenge(name: String, category: Category, goalHours: Int, why: String): String {
        val now = System.currentTimeMillis()
        val challenge = Challenge(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            category = category,
            goalHours = goalHours,
            why = why.trim(),
            createdAt = now,
            currentStart = now,
        )
        repo.update { it.copy(challenges = it.challenges + challenge) }
        return challenge.id
    }

    fun logUrge(challengeId: String, intensity: Int, triggers: List<Trigger>, note: String) {
        val log = UrgeLog(
            id = UUID.randomUUID().toString(),
            challengeId = challengeId,
            time = System.currentTimeMillis(),
            intensity = intensity,
            triggers = triggers,
            note = note.trim(),
        )
        repo.update { it.copy(urges = it.urges + log) }
    }

    /** Records the failed attempt and immediately starts a fresh one. */
    fun relapse(challengeId: String, triggers: List<Trigger>, note: String) {
        val now = System.currentTimeMillis()
        updateChallenge(challengeId) { c ->
            val start = c.currentStart ?: return@updateChallenge c
            c.copy(
                attempts = c.attempts + Attempt(start, now, AttemptResult.FAILED, triggers, note.trim()),
                currentStart = now,
            )
        }
    }

    /** Claims a reached goal; either levels up (double goal) or ends the challenge. */
    fun completeGoal(challengeId: String, levelUp: Boolean) {
        val now = System.currentTimeMillis()
        updateChallenge(challengeId) { c ->
            val start = c.currentStart ?: return@updateChallenge c
            c.copy(
                attempts = c.attempts + Attempt(start, now, AttemptResult.COMPLETED),
                currentStart = if (levelUp) now else null,
                goalHours = if (levelUp) Logic.nextLevelGoal(c.goalHours) else c.goalHours,
            )
        }
    }

    fun restart(challengeId: String) {
        updateChallenge(challengeId) { it.copy(currentStart = System.currentTimeMillis()) }
    }

    fun delete(challengeId: String) {
        repo.update { d ->
            d.copy(
                challenges = d.challenges.filterNot { it.id == challengeId },
                urges = d.urges.filterNot { it.challengeId == challengeId },
            )
        }
    }

    private fun updateChallenge(id: String, transform: (Challenge) -> Challenge) {
        repo.update { d -> d.copy(challenges = d.challenges.map { if (it.id == id) transform(it) else it }) }
    }
}
