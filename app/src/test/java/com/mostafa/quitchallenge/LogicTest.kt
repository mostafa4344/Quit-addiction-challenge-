package com.mostafa.quitchallenge

import com.mostafa.quitchallenge.data.AppData
import com.mostafa.quitchallenge.data.Attempt
import com.mostafa.quitchallenge.data.AttemptResult
import com.mostafa.quitchallenge.data.Category
import com.mostafa.quitchallenge.data.Challenge
import com.mostafa.quitchallenge.data.ChallengeType
import com.mostafa.quitchallenge.data.DayPart
import com.mostafa.quitchallenge.data.HOUR_MS
import com.mostafa.quitchallenge.data.Logic
import com.mostafa.quitchallenge.data.MINUTE_MS
import com.mostafa.quitchallenge.data.StartOption
import com.mostafa.quitchallenge.data.riskHeatmap
import com.mostafa.quitchallenge.data.startTimeFor
import com.mostafa.quitchallenge.data.Trigger
import com.mostafa.quitchallenge.data.UrgeLog
import com.mostafa.quitchallenge.data.UrgeSource
import com.mostafa.quitchallenge.data.UsageEvent
import com.mostafa.quitchallenge.data.computeInsights
import com.mostafa.quitchallenge.data.foregroundTimeByPackage
import com.mostafa.quitchallenge.data.formatDuration
import com.mostafa.quitchallenge.data.dueCheckInSlots
import com.mostafa.quitchallenge.data.nextCheckInAt
import com.mostafa.quitchallenge.data.formatTimeOfDay
import com.mostafa.quitchallenge.data.checkedInToday
import com.mostafa.quitchallenge.data.dayNumber
import com.mostafa.quitchallenge.data.formatGoal
import com.mostafa.quitchallenge.data.formatMinutes
import com.mostafa.quitchallenge.data.formatMoney
import com.mostafa.quitchallenge.data.localCurrencySymbol
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class LogicTest {
    private fun challenge(
        start: Long?,
        goalHours: Int = 24,
        attempts: List<Attempt> = emptyList(),
        limitMinutes: Int = 0,
    ) = Challenge(
        id = "c1",
        name = "Test",
        category = Category.OTHER,
        goalHours = goalHours,
        createdAt = 0,
        currentStart = start,
        attempts = attempts,
        type = ChallengeType.APP_LIMIT,
        dailyLimitMinutes = limitMinutes,
    )

    @Test
    fun progressIsClampedBetweenZeroAndOne() {
        val c = challenge(start = 0)
        assertEquals(0.5f, Logic.progress(c, 12 * HOUR_MS), 0.0001f)
        assertEquals(1f, Logic.progress(c, 48 * HOUR_MS), 0.0001f)
        assertEquals(0f, Logic.progress(c, -5), 0.0001f)
    }

    @Test
    fun goalReachedOnlyWhileActive() {
        assertTrue(Logic.isGoalReached(challenge(start = 0), 24 * HOUR_MS))
        assertFalse(Logic.isGoalReached(challenge(start = 0), 23 * HOUR_MS))
        assertFalse(Logic.isGoalReached(challenge(start = null), 100 * HOUR_MS))
    }

    @Test
    fun milestonesAndBestStreak() {
        assertEquals(1, Logic.nextMilestone(0)?.hours)
        assertEquals(72, Logic.nextMilestone(25 * HOUR_MS)?.hours)
        assertEquals(4, Logic.reachedMilestones(24 * HOUR_MS).size)
        val c = challenge(
            start = 100 * HOUR_MS,
            attempts = listOf(Attempt(0, 50 * HOUR_MS, AttemptResult.FAILED)),
        )
        assertEquals(50 * HOUR_MS, Logic.bestStreakMs(c, 110 * HOUR_MS))
        assertEquals(60 * HOUR_MS, Logic.bestStreakMs(c, 160 * HOUR_MS))
    }

    @Test
    fun totalCleanTimeNeverResetsAfterASlip() {
        val c = challenge(
            start = 50 * HOUR_MS,
            attempts = listOf(Attempt(0, 40 * HOUR_MS, AttemptResult.FAILED)),
        )
        assertEquals(50 * HOUR_MS, Logic.totalCleanMs(c, 60 * HOUR_MS))
    }

    @Test
    fun cleanDaysCountsCalendarDaysWithoutSlips() {
        val day = 24 * HOUR_MS
        // Created on day 0, slipped on day 2 (twice) and day 4; today is day 5.
        val c = challenge(
            start = 4 * day + HOUR_MS,
            attempts = listOf(
                Attempt(0, 2 * day + HOUR_MS, AttemptResult.FAILED),
                Attempt(2 * day + HOUR_MS, 2 * day + 5 * HOUR_MS, AttemptResult.FAILED),
                Attempt(2 * day + 5 * HOUR_MS, 4 * day + HOUR_MS, AttemptResult.FAILED),
            ),
        )
        assertEquals(4 to 6, Logic.cleanDaysInLast30(c, 5 * day + HOUR_MS, ZoneOffset.UTC))
        // Only the last 30 days count once the challenge is older than that.
        assertEquals(30 to 30, Logic.cleanDaysInLast30(c, 100 * day, ZoneOffset.UTC))
    }

    @Test
    fun levelLadderUsesRealisticSteps() {
        assertEquals(72, Logic.nextLevelGoal(24))
        assertEquals(168, Logic.nextLevelGoal(72))
        assertEquals(336, Logic.nextLevelGoal(200))
        assertEquals(8760 * 2, Logic.nextLevelGoal(8760))
    }

    @Test
    fun savingsFollowTotalCleanTime() {
        val c = challenge(start = 0).copy(minutesPerDay = 60, moneyPerDay = 100.0)
        assertEquals(36 * 60L, Logic.savedMinutes(c, 36 * 24 * HOUR_MS))
        assertEquals(150.0, Logic.savedMoney(c, 36 * HOUR_MS), 0.001)
    }

    @Test
    fun startOptionsLandOnFreshStarts() {
        // Wednesday 15 Jan 2025, 10:00 UTC.
        val now = java.time.ZonedDateTime.of(2025, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        fun at(y: Int, m: Int, d: Int, h: Int) = java.time.ZonedDateTime.of(y, m, d, h, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(now, startTimeFor(StartOption.NOW, now, ZoneOffset.UTC))
        assertEquals(at(2025, 1, 16, 6), startTimeFor(StartOption.TOMORROW, now, ZoneOffset.UTC))
        assertEquals(at(2025, 1, 20, 0), startTimeFor(StartOption.NEXT_MONDAY, now, ZoneOffset.UTC))
        assertEquals(at(2025, 2, 1, 0), startTimeFor(StartOption.NEXT_MONTH, now, ZoneOffset.UTC))
    }

    @Test
    fun heatmapBucketsByWeekdayAndDayPart() {
        // 1 Jan 1970 was a Thursday.
        val thursdayNight = 23 * HOUR_MS
        val data = AppData(
            urges = listOf(
                UrgeLog("a", "c1", thursdayNight, 3),
                UrgeLog("b", "c1", thursdayNight + 1000, 3),
            ),
        )
        val grid = riskHeatmap(data, ZoneOffset.UTC)
        assertEquals(2, grid[3][DayPart.NIGHT.ordinal])
        assertEquals(2, grid.sumOf { it.sum() })
    }

    @Test
    fun checkInSlotsFireOnceEachAfterTheirTime() {
        val day = 24 * HOUR_MS
        val times = listOf(21 * 60, 9 * 60)
        val at8 = 3 * day + 8 * HOUR_MS
        val at930 = 3 * day + 9 * HOUR_MS + 30 * 60_000
        val at22 = 3 * day + 22 * HOUR_MS
        assertEquals(emptyList<Int>(), dueCheckInSlots(at8, times, emptySet(), ZoneOffset.UTC))
        assertEquals(listOf(9 * 60), dueCheckInSlots(at930, times, emptySet(), ZoneOffset.UTC))
        assertEquals(emptyList<Int>(), dueCheckInSlots(at930, times, setOf(9 * 60), ZoneOffset.UTC))
        assertEquals(listOf(21 * 60), dueCheckInSlots(at22, times, setOf(9 * 60), ZoneOffset.UTC))
    }

    @Test
    fun nextCheckInIsTheSoonestUpcomingTime() {
        val day = 24 * HOUR_MS
        val times = listOf(9 * 60, 21 * 60)
        assertEquals(3 * day + 21 * HOUR_MS, nextCheckInAt(3 * day + 10 * HOUR_MS, times, ZoneOffset.UTC))
        assertEquals(4 * day + 9 * HOUR_MS, nextCheckInAt(3 * day + 22 * HOUR_MS, times, ZoneOffset.UTC))
        assertNull(nextCheckInAt(0, emptyList(), ZoneOffset.UTC))
        assertEquals("9:05 PM", formatTimeOfDay(21 * 60 + 5))
        assertEquals("12:00 AM", formatTimeOfDay(0))
        assertEquals("12:30 PM", formatTimeOfDay(12 * 60 + 30))
    }

    @Test
    fun checkedInTodayComparesLocalDates() {
        val day = 24 * HOUR_MS
        val c = challenge(start = 0).copy(lastCheckIn = 2 * day + 23 * HOUR_MS)
        assertTrue(checkedInToday(c, 2 * day + 23 * HOUR_MS + 1000, ZoneOffset.UTC))
        assertFalse(checkedInToday(c, 3 * day + HOUR_MS, ZoneOffset.UTC))
        assertFalse(checkedInToday(challenge(start = 0), 0, ZoneOffset.UTC))
        assertEquals(3L, dayNumber(c, 2 * day + HOUR_MS))
    }

    @Test
    fun aJustRestartedStreakCountsAsStarted() {
        // The screen's clock lags up to a second behind a restart that just happened.
        assertTrue(Logic.hasStarted(challenge(start = 10_500), now = 10_000))
        // A challenge scheduled for later is not started yet.
        assertFalse(Logic.hasStarted(challenge(start = 10 * HOUR_MS), now = 0))
        assertFalse(Logic.hasStarted(challenge(start = null), now = 0))
    }

    @Test
    fun formatsDurations() {
        assertEquals("7 days", formatGoal(168))
        assertEquals("1 day 6 hours", formatGoal(30))
        assertEquals("1 hour", formatGoal(1))
        assertEquals("1d 2h 3m", formatDuration(26 * HOUR_MS + 3 * MINUTE_MS))
        assertEquals("1h 30m", formatMinutes(90))
        assertEquals("45 min", formatMinutes(45))
    }

    @Test
    fun usageWindowStartsAtLaterOfMidnightAndAttemptStart() {
        val day2 = 24 * HOUR_MS
        assertEquals(day2, Logic.usageWindowStart(challenge(start = 0), day2 + 5 * HOUR_MS, ZoneOffset.UTC))
        val restarted = day2 + 3 * HOUR_MS
        assertEquals(restarted, Logic.usageWindowStart(challenge(start = restarted), day2 + 5 * HOUR_MS, ZoneOffset.UTC))
    }

    @Test
    fun overLimitHasOneMinuteGrace() {
        val c = challenge(start = 0, limitMinutes = 30)
        assertFalse(Logic.isOverLimit(c, 30 * MINUTE_MS))
        assertTrue(Logic.isOverLimit(c, 31 * MINUTE_MS))
        assertFalse(Logic.isOverLimit(challenge(start = 0, limitMinutes = 0), 20_000))
    }

    @Test
    fun foregroundTimeSumsSessionsAcrossActivities() {
        val fb = "com.facebook.katana"
        val events = listOf(
            // Already open at the window start: first event is a pause.
            UsageEvent(fb, "Main", 10, false),
            // Session switching between two activities of the same app.
            UsageEvent(fb, "Main", 100, true),
            UsageEvent(fb, "Video", 150, true),
            UsageEvent(fb, "Main", 160, false),
            UsageEvent(fb, "Video", 200, false),
            // Other apps are ignored.
            UsageEvent("other.app", "X", 210, true),
            // Still open at the end of the window.
            UsageEvent(fb, "Main", 300, true),
        )
        val result = foregroundTimeByPackage(events, setOf(fb), from = 0, to = 400)
        assertEquals(10L + 100L + 100L, result[fb])
        assertNull(result["other.app"])
    }

    @Test
    fun insightsCountTriggersAndDayParts() {
        val night = 23 * HOUR_MS
        val morning = 8 * HOUR_MS
        val c = challenge(
            start = null,
            attempts = listOf(
                Attempt(0, night, AttemptResult.FAILED, listOf(Trigger.STRESS)),
                Attempt(0, 24 * HOUR_MS, AttemptResult.COMPLETED),
            ),
        )
        val data = AppData(
            challenges = listOf(c),
            urges = listOf(
                UrgeLog("u1", "c1", morning, 3, listOf(Trigger.STRESS, Trigger.BOREDOM)),
                UrgeLog("u2", "", morning, 3, source = UrgeSource.KEYWORD, resisted = false),
            ),
        )
        val insights = computeInsights(data, now = 0, zone = ZoneOffset.UTC)
        assertEquals(Trigger.STRESS to 2, insights.triggerCounts.first())
        assertEquals(1, insights.dayPartCounts.first { it.first == DayPart.NIGHT }.second)
        assertEquals(2, insights.dayPartCounts.first { it.first == DayPart.MORNING }.second)
        assertEquals(50, insights.successRate)
        assertEquals(1, insights.urgesHandled)
    }

    @Test
    fun currencyFollowsThePhoneRegion() {
        assertEquals("$", localCurrencySymbol(Locale.US))
        assertEquals("€", localCurrencySymbol(Locale.GERMANY))
        assertEquals("৳", localCurrencySymbol(Locale("bn", "BD")))
        assertEquals("৳", localCurrencySymbol(Locale("en", "BD")))
        // No region set: fall back instead of crashing.
        assertEquals("$", localCurrencySymbol(Locale("en")))
    }

    @Test
    fun moneyFormatting() {
        assertEquals("$120", formatMoney(120.4, "$"))
        assertEquals("৳12.50", formatMoney(12.5, "৳"))
        assertEquals("BDT 120", formatMoney(120.0, "BDT"))
    }
}
