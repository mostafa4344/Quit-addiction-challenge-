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

    fun updateChallenge(id: String, transform: (Challenge) -> Challenge) {
        update { d -> d.copy(challenges = d.challenges.map { if (it.id == id) transform(it) else it }) }
    }

    /** Records the failed attempt and immediately starts a fresh one. */
    fun relapse(id: String, triggers: List<Trigger>, note: String, now: Long = System.currentTimeMillis()) {
        updateChallenge(id) { c ->
            val start = c.currentStart ?: return@updateChallenge c
            c.copy(
                attempts = c.attempts + Attempt(start, now, AttemptResult.FAILED, triggers, note.trim()),
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
        )
        update { it.copy(urges = it.urges + log) }
    }
}

/** One shared repository per process, so the UI, the background worker and the guard service never overwrite each other. */
object AppGraph {
    @Volatile
    private var repository: Repository? = null

    fun repo(context: Context): Repository =
        repository ?: synchronized(this) {
            repository ?: run {
                val app = context.applicationContext
                Repository(File(app.filesDir, "data.json")) { ChallengeWidget.updateAll(app) }.also { repository = it }
            }
        }
}
