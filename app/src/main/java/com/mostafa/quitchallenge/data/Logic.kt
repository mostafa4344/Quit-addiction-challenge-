package com.mostafa.quitchallenge.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

const val HOUR_MS = 3_600_000L
const val MINUTE_MS = 60_000L

data class Milestone(val hours: Int, val label: String, val emoji: String)

val MILESTONES = listOf(
    Milestone(1, "1 hour", "🌱"),
    Milestone(6, "6 hours", "🌿"),
    Milestone(12, "12 hours", "🍀"),
    Milestone(24, "1 day", "⭐"),
    Milestone(72, "3 days", "🔥"),
    Milestone(168, "1 week", "💪"),
    Milestone(336, "2 weeks", "🏅"),
    Milestone(504, "21 days", "🧠"),
    Milestone(720, "1 month", "🏆"),
    Milestone(1440, "2 months", "💎"),
    Milestone(2160, "3 months", "👑"),
    Milestone(4320, "6 months", "🚀"),
    Milestone(8760, "1 year", "🌟"),
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

    /** All time spent clean across every attempt. Never goes back to zero after a slip. */
    fun totalCleanMs(c: Challenge, now: Long): Long =
        c.attempts.sumOf { it.durationMs } + elapsedMs(c, now)

    /**
     * Calendar days without a slip in the last 30 days (or since the challenge began),
     * returned as (clean days, days counted).
     */
    fun cleanDaysInLast30(c: Challenge, now: Long, zone: ZoneId = ZoneId.systemDefault()): Pair<Int, Int> {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val created = Instant.ofEpochMilli(c.createdAt).atZone(zone).toLocalDate()
        val first = maxOf(today.minusDays(29), created)
        if (first.isAfter(today)) return 0 to 0
        val slipDays = c.attempts
            .filter { it.result == AttemptResult.FAILED }
            .map { Instant.ofEpochMilli(it.endedAt).atZone(zone).toLocalDate() }
            .toSet()
        val days = generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.toList()
        return days.count { it !in slipDays } to days.size
    }

    fun bestStreakMs(c: Challenge, now: Long): Long =
        maxOf(c.attempts.maxOfOrNull { it.durationMs } ?: 0L, elapsedMs(c, now))

    private val LEVEL_LADDER_HOURS = listOf(24, 72, 168, 336, 504, 720, 1440, 2160, 4320, 8760)

    /** Each completed level moves to the next realistic step (1, 3, 7, 14, 21, 30, 60, 90, 180, 365 days). */
    fun nextLevelGoal(goalHours: Int): Int = LEVEL_LADDER_HOURS.firstOrNull { it > goalHours } ?: (goalHours * 2)

    /** Time and money not spent on the habit, based on the user's own per-day estimate. */
    fun savedMinutes(c: Challenge, now: Long): Long = totalCleanMs(c, now) * c.minutesPerDay / (24 * HOUR_MS)

    fun savedMoney(c: Challenge, now: Long): Double = totalCleanMs(c, now).toDouble() / (24 * HOUR_MS) * c.moneyPerDay

    /**
     * Today's usage counts only from the later of midnight and the attempt start,
     * so a fresh attempt after a failure isn't failed again by earlier usage.
     */
    fun usageWindowStart(c: Challenge, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        maxOf(startOfDay(now, zone), c.currentStart ?: now)

    /** One minute of grace so a split-second accidental open doesn't fail a challenge. */
    fun isOverLimit(c: Challenge, usedMs: Long): Boolean =
        usedMs >= c.dailyLimitMinutes * MINUTE_MS + MINUTE_MS
}

fun startOfDay(time: Long, zone: ZoneId): Long =
    Instant.ofEpochMilli(time).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

fun today(zone: ZoneId = ZoneId.systemDefault()): LocalDate = LocalDate.now(zone)

/** A raw foreground/background switch for one activity of an app. */
data class UsageEvent(val packageName: String, val className: String, val time: Long, val foreground: Boolean)

/**
 * Sums how long each package was on screen between [from] and [to].
 * An app counts as open while any of its activities is resumed; an app whose first event
 * is a pause was already open at [from].
 */
fun foregroundTimeByPackage(events: List<UsageEvent>, packages: Set<String>, from: Long, to: Long): Map<String, Long> {
    val totals = mutableMapOf<String, Long>()
    val openActivities = mutableMapOf<String, MutableSet<String>>()
    val openSince = mutableMapOf<String, Long>()
    val seen = mutableSetOf<String>()

    fun add(pkg: String, ms: Long) {
        if (ms > 0) totals[pkg] = (totals[pkg] ?: 0L) + ms
    }

    for (e in events.sortedBy { it.time }) {
        val pkg = e.packageName
        if (pkg !in packages || e.time < from || e.time > to) continue
        val open = openActivities.getOrPut(pkg) { mutableSetOf() }
        if (e.foreground) {
            if (open.isEmpty()) openSince[pkg] = e.time
            open += e.className
        } else if (e.className in open) {
            open -= e.className
            if (open.isEmpty()) add(pkg, e.time - (openSince.remove(pkg) ?: e.time))
        } else if (open.isEmpty() && pkg !in seen) {
            add(pkg, e.time - from)
        }
        seen += pkg
    }
    openSince.forEach { (pkg, start) -> add(pkg, to - start) }
    return totals
}

/** Finds the first keyword that appears in [text] as a whole word or phrase, ignoring case. */
fun findKeyword(text: String, keywords: List<String>): String? {
    val haystack = text.lowercase()
    for (raw in keywords) {
        val kw = raw.trim().lowercase()
        if (kw.isEmpty()) continue
        var i = haystack.indexOf(kw)
        while (i >= 0) {
            val before = haystack.getOrNull(i - 1)
            val after = haystack.getOrNull(i + kw.length)
            if ((before == null || !before.isLetterOrDigit()) && (after == null || !after.isLetterOrDigit())) return raw.trim()
            i = haystack.indexOf(kw, i + 1)
        }
    }
    return null
}

enum class DayPart(val label: String) {
    MORNING("🌅 Morning (5am–12pm)"),
    AFTERNOON("☀️ Afternoon (12pm–5pm)"),
    EVENING("🌇 Evening (5pm–9pm)"),
    NIGHT("🌙 Night (9pm–5am)"),
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
    val keywordAlerts: Int,
    val failures: Int,
    val completions: Int,
    val totalCleanMs: Long,
) {
    val hasData: Boolean get() = urgesHandled + keywordAlerts + failures + completions > 0
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
        urgesHandled = data.urges.count { it.resisted },
        keywordAlerts = data.urges.count { it.source == UrgeSource.KEYWORD },
        failures = failed.size,
        completions = data.challenges.sumOf { it.completions },
        totalCleanMs = data.challenges.sumOf { c -> c.attempts.sumOf { it.durationMs } + Logic.elapsedMs(c, now) },
    )
}

fun formatDuration(ms: Long, showSeconds: Boolean = false): String {
    val totalSec = ms.coerceAtLeast(0) / 1000
    val d = totalSec / 86_400
    val h = (totalSec % 86_400) / 3_600
    val m = (totalSec % 3_600) / 60
    val s = totalSec % 60
    val parts = buildList {
        if (d > 0) add("${d}d")
        if (d > 0 || h > 0) add("${h}h")
        add("${m}m")
        if (showSeconds) add("${s}s")
    }
    return parts.joinToString(" ")
}

fun formatGoal(hours: Int): String {
    val d = hours / 24
    val h = hours % 24
    fun plural(n: Int, word: String) = if (n == 1) "1 $word" else "$n ${word}s"
    return when {
        d > 0 && h > 0 -> "${plural(d, "day")} ${plural(h, "hour")}"
        d > 0 -> plural(d, "day")
        else -> plural(h, "hour")
    }
}

fun formatMinutes(minutes: Int): String = when {
    minutes == 0 -> "0 min (don't open)"
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)

fun formatDate(ms: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(ms).atZone(zone).format(dateFormatter)

fun formatMoney(amount: Double, currency: String): String =
    if (amount >= 100) "$currency${amount.toLong()}" else "$currency${"%.2f".format(Locale.ENGLISH, amount)}"

/** Today's open attempts for a set of apps: (opened, skipped). */
fun openStats(attempts: List<OpenAttempt>, packages: Set<String>, from: Long): Pair<Int, Int> {
    val today = attempts.filter { it.time >= from && it.packageName in packages }
    return today.size to today.count { it.skipped }
}
