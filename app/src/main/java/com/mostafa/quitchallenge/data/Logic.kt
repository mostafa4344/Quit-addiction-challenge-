package com.mostafa.quitchallenge.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

const val HOUR_MS = 3_600_000L

data class Milestone(val hours: Int, val label: String, val emoji: String)

val MILESTONES = listOf(
    Milestone(1, "১ ঘণ্টা", "🌱"),
    Milestone(6, "৬ ঘণ্টা", "🌿"),
    Milestone(12, "১২ ঘণ্টা", "🍀"),
    Milestone(24, "১ দিন", "⭐"),
    Milestone(72, "৩ দিন", "🔥"),
    Milestone(168, "১ সপ্তাহ", "💪"),
    Milestone(336, "২ সপ্তাহ", "🏅"),
    Milestone(504, "২১ দিন", "🧠"),
    Milestone(720, "১ মাস", "🏆"),
    Milestone(1440, "২ মাস", "💎"),
    Milestone(2160, "৩ মাস", "👑"),
    Milestone(4320, "৬ মাস", "🚀"),
    Milestone(8760, "১ বছর", "🌟"),
)

object Logic {
    fun goalMs(c: Challenge): Long = c.goalHours * HOUR_MS

    fun elapsedMs(c: Challenge, now: Long): Long =
        c.currentStart?.let { (now - it).coerceAtLeast(0) } ?: 0L

    fun progress(c: Challenge, now: Long): Float {
        val goal = goalMs(c)
        if (goal <= 0) return 0f
        return (elapsedMs(c, now).toDouble() / goal).toFloat().coerceIn(0f, 1f)
    }

    fun isGoalReached(c: Challenge, now: Long): Boolean =
        c.isActive && elapsedMs(c, now) >= goalMs(c)

    fun reachedMilestones(elapsedMs: Long): List<Milestone> =
        MILESTONES.filter { it.hours * HOUR_MS <= elapsedMs }

    fun nextMilestone(elapsedMs: Long): Milestone? =
        MILESTONES.firstOrNull { it.hours * HOUR_MS > elapsedMs }

    fun bestStreakMs(c: Challenge, now: Long): Long =
        maxOf(c.attempts.maxOfOrNull { it.durationMs } ?: 0L, elapsedMs(c, now))

    /** Each completed level doubles the goal, so the challenge keeps growing. */
    fun nextLevelGoal(goalHours: Int): Int = goalHours * 2
}

enum class DayPart(val label: String) {
    MORNING("🌅 সকাল (৫টা–১২টা)"),
    AFTERNOON("☀️ দুপুর (১২টা–৫টা)"),
    EVENING("🌇 সন্ধ্যা (৫টা–৯টা)"),
    NIGHT("🌙 রাত (৯টা–৫টা)"),
}

fun dayPartOf(time: Long, zone: ZoneId): DayPart =
    when (Instant.ofEpochMilli(time).atZone(zone).hour) {
        in 5..11 -> DayPart.MORNING
        in 12..16 -> DayPart.AFTERNOON
        in 17..20 -> DayPart.EVENING
        else -> DayPart.NIGHT
    }

data class Insights(
    val triggerCounts: List<Pair<Trigger, Int>>,
    val dayPartCounts: List<Pair<DayPart, Int>>,
    val urgesHandled: Int,
    val failures: Int,
    val completions: Int,
    val totalCleanMs: Long,
) {
    val hasData: Boolean get() = urgesHandled + failures + completions > 0
    val successRate: Int
        get() = if (failures + completions == 0) 0 else completions * 100 / (failures + completions)
}

/** Aggregates urges and relapses to show which pressures hit hardest and when. */
fun computeInsights(data: AppData, now: Long, zone: ZoneId = ZoneId.systemDefault()): Insights {
    val failed = data.challenges.flatMap { c -> c.attempts.filter { it.result == AttemptResult.FAILED } }
    val events: List<Pair<Long, List<Trigger>>> =
        data.urges.map { it.time to it.triggers } + failed.map { it.endedAt to it.triggers }

    val triggerCounts = events.flatMap { it.second }
        .groupingBy { it }.eachCount()
        .toList().sortedByDescending { it.second }
    val parts = events.groupingBy { dayPartOf(it.first, zone) }.eachCount()

    return Insights(
        triggerCounts = triggerCounts,
        dayPartCounts = DayPart.entries.map { it to (parts[it] ?: 0) },
        urgesHandled = data.urges.size,
        failures = failed.size,
        completions = data.challenges.sumOf { it.completions },
        totalCleanMs = data.challenges.sumOf { c -> c.attempts.sumOf { it.durationMs } + Logic.elapsedMs(c, now) },
    )
}

fun String.bnDigits(): String = buildString {
    for (ch in this@bnDigits) append(if (ch in '0'..'9') '০' + (ch - '0') else ch)
}

fun Int.bn(): String = toString().bnDigits()
fun Long.bn(): String = toString().bnDigits()

fun formatDuration(ms: Long, showSeconds: Boolean = false): String {
    val totalSec = ms / 1000
    val d = totalSec / 86_400
    val h = (totalSec % 86_400) / 3_600
    val m = (totalSec % 3_600) / 60
    val s = totalSec % 60
    val parts = buildList {
        if (d > 0) add("${d.bn()} দিন")
        if (d > 0 || h > 0) add("${h.bn()} ঘণ্টা")
        add("${m.bn()} মিনিট")
        if (showSeconds) add("${s.bn()} সেকেন্ড")
    }
    return parts.joinToString(" ")
}

fun formatGoal(hours: Int): String {
    val d = hours / 24
    val h = hours % 24
    return when {
        d > 0 && h > 0 -> "${d.bn()} দিন ${h.bn()} ঘণ্টা"
        d > 0 -> "${d.bn()} দিন"
        else -> "${h.bn()} ঘণ্টা"
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy, hh:mm a", Locale.ENGLISH)

fun formatDate(ms: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(ms).atZone(zone).format(dateFormatter).bnDigits()
