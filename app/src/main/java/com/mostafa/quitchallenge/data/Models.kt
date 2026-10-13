package com.mostafa.quitchallenge.data

import kotlinx.serialization.Serializable

/** Pressures that push someone back toward the habit, each with a coping tip. */
@Serializable
enum class Trigger(val label: String, val emoji: String, val tip: String) {
    STRESS("Stress", "😣", "Take 5 slow breaths (in for 4s, hold 7s, out for 8s) or go for a short walk."),
    BOREDOM("Boredom", "😐", "Keep a ready 'when I'm bored' list: read, work out, learn a new skill."),
    LONELINESS("Loneliness", "😔", "Call a friend or family member, or go somewhere with people around."),
    TIREDNESS("Tiredness", "🥱", "A tired mind is a weak mind. Sleep on time and keep the phone away from your bed."),
    ANXIETY("Anxiety", "😰", "Write your worries down on paper, then finish one small task."),
    SOCIAL("Peer pressure", "👥", "Prepare a sentence for saying 'no' in advance and avoid risky places."),
    HABIT("Habit / routine time", "⏰", "At the time the urge usually hits, put a different routine in its place."),
    SADNESS("Feeling low", "😢", "Accept the feeling, do something you enjoy, or talk to someone close."),
    ANGER("Anger", "😠", "Change your location, drink cold water, slowly count to 10."),
    CELEBRATION("Celebration", "🎉", "Plan other ways to celebrate: a favourite meal, an outing."),
}

@Serializable
enum class Category(val label: String, val emoji: String) {
    SOCIAL_MEDIA("Social media", "📱"),
    GAMING("Mobile / games", "🎮"),
    SMOKING("Smoking", "🚬"),
    PORN("Pornography", "🚫"),
    JUNK_FOOD("Junk food", "🍔"),
    OTHER("Other", "⭐"),
}

@Serializable
enum class ChallengeType {
    /** Stay away completely; the user reports relapses themselves. */
    TIMER,

    /** Keep chosen apps under a daily time limit; tracked automatically via usage access. */
    APP_LIMIT,
}

@Serializable
enum class AttemptResult { COMPLETED, FAILED }

/** One finished run of a challenge: either the goal was reached or the user relapsed. */
@Serializable
data class Attempt(
    val startedAt: Long,
    val endedAt: Long,
    val result: AttemptResult,
    val triggers: List<Trigger> = emptyList(),
    val note: String = "",
) {
    val durationMs: Long get() = (endedAt - startedAt).coerceAtLeast(0)
}

@Serializable
data class TrackedApp(val packageName: String, val label: String)

@Serializable
data class Challenge(
    val id: String,
    val name: String,
    val category: Category,
    val goalHours: Int,
    val why: String = "",
    val createdAt: Long,
    /** Start of the running attempt, or null once the user has finished the challenge. */
    val currentStart: Long?,
    val attempts: List<Attempt> = emptyList(),
    val type: ChallengeType = ChallengeType.TIMER,
    val trackedApps: List<TrackedApp> = emptyList(),
    val dailyLimitMinutes: Int = 0,
) {
    val isActive: Boolean get() = currentStart != null
    val attemptNumber: Int get() = attempts.size + 1
    val failures: Int get() = attempts.count { it.result == AttemptResult.FAILED }
    val completions: Int get() = attempts.count { it.result == AttemptResult.COMPLETED }
}

@Serializable
enum class UrgeSource { MANUAL, KEYWORD }

/** An urge the user felt, logged by hand or raised by the keyword guard. */
@Serializable
data class UrgeLog(
    val id: String,
    val challengeId: String,
    val time: Long,
    val intensity: Int,
    val triggers: List<Trigger> = emptyList(),
    val note: String = "",
    val source: UrgeSource = UrgeSource.MANUAL,
    val resisted: Boolean = true,
)

val DEFAULT_KEYWORDS = listOf("porn", "xxx", "nsfw", "nude", "sex video", "xvideos", "pornhub", "onlyfans")

@Serializable
data class GuardSettings(
    val keywordGuard: Boolean = true,
    val keywords: List<String> = DEFAULT_KEYWORDS,
)

@Serializable
data class AppData(
    val challenges: List<Challenge> = emptyList(),
    val urges: List<UrgeLog> = emptyList(),
    val settings: GuardSettings = GuardSettings(),
)
