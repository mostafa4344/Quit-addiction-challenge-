package com.mostafa.quitchallenge.data

import android.content.Context
import com.mostafa.quitchallenge.widget.ChallengeWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** Keeps all app data in one JSON file on the device; no account or internet needed. */
class Repository(private val file: File, private val onChange: (AppData) -> Unit = {}) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val state = MutableStateFlow(load())
    val data: StateFlow<AppData> = state.asStateFlow()

    private fun load(): AppData = try {
        if (file.exists()) json.decodeFromString(AppData.serializer(), file.readText()) else AppData()
    } catch (e: Exception) {
        AppData()
    }

    @Synchronized
    fun update(transform: (AppData) -> AppData) {
        val next = transform(state.value)
        state.value = next
        // Write to a temp file first so a crash mid-write can't corrupt the saved data.
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), next))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
        onChange(next)
    }

    fun checkIn(id: String, now: Long = System.currentTimeMillis()) {
        updateChallenge(id) { it.copy(lastCheckIn = now) }
    }

    fun setPlan(trigger: Trigger, plan: String) {
        update { d ->
            d.copy(plans = if (plan.isBlank()) d.plans - trigger else d.plans + (trigger to plan.trim()))
        }
    }

    fun exportJson(): String = json.encodeToString(AppData.serializer(), state.value)

    /** Replaces all data with a backup; returns false (and changes nothing) if the file isn't a valid backup. */
    fun importJson(text: String): Boolean {
        val imported = try {
            json.decodeFromString(AppData.serializer(), text)
        } catch (e: Exception) {
            return false
        }
        update { imported }
        return true
    }

    fun updateChallenge(id: String, transform: (Challenge) -> Challenge) {
        update { d -> d.copy(challenges = d.challenges.map { if (it.id == id) transform(it) else it }) }
    }

    /** Records the failed attempt and immediately starts a fresh one. */
    fun relapse(
        id: String,
        triggers: List<Trigger>,
        note: String,
        now: Long = System.currentTimeMillis(),
        warningSigns: String = "",
        plan: String = "",
    ) {
        updateChallenge(id) { c ->
            val start = c.currentStart ?: return@updateChallenge c
            c.copy(
                attempts = c.attempts + Attempt(start, now, AttemptResult.FAILED, triggers, note.trim(), warningSigns.trim(), plan.trim()),
                currentStart = now,
            )
        }
    }

    /** Claims a reached goal; either levels up (double goal) or ends the challenge. */
    fun completeGoal(id: String, levelUp: Boolean, now: Long = System.currentTimeMillis()) {
        updateChallenge(id) { c ->
            val start = c.currentStart ?: return@updateChallenge c
            c.copy(
                attempts = c.attempts + Attempt(start, now, AttemptResult.COMPLETED),
                currentStart = if (levelUp) now else null,
                goalHours = if (levelUp) Logic.nextLevelGoal(c.goalHours) else c.goalHours,
            )
        }
    }

    fun logUrge(
        challengeId: String,
        intensity: Int,
        triggers: List<Trigger>,
        note: String,
        source: UrgeSource = UrgeSource.MANUAL,
        resisted: Boolean = true,
        intensityAfter: Int? = null,
    ) {
        val log = UrgeLog(
            id = UUID.randomUUID().toString(),
            challengeId = challengeId,
            time = System.currentTimeMillis(),
            intensity = intensity,
            triggers = triggers,
            note = note.trim(),
            source = source,
            resisted = resisted,
            intensityAfter = intensityAfter,
        )
        update { it.copy(urges = it.urges + log) }
    }
}

/** One shared repository per process, so the UI, the background worker and the guard service never overwrite each other. */
object AppGraph {
    @Volatile
    private var repository: Repository? = null

    /** Drops the cached repository so the next [repo] call reloads from disk (used by on-device tests). */
    internal fun reset() {
        repository = null
    }

    fun repo(context: Context): Repository =
        repository ?: synchronized(this) {
            repository ?: run {
                val app = context.applicationContext
                Repository(File(app.filesDir, "data.json")) { ChallengeWidget.updateAll(app) }.also { repository = it }
            }
        }
}
