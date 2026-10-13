package com.mostafa.quitchallenge

import com.mostafa.quitchallenge.data.AppData
import com.mostafa.quitchallenge.data.Attempt
import com.mostafa.quitchallenge.data.AttemptResult
import com.mostafa.quitchallenge.data.Category
import com.mostafa.quitchallenge.data.Challenge
import com.mostafa.quitchallenge.data.DayPart
import com.mostafa.quitchallenge.data.HOUR_MS
import com.mostafa.quitchallenge.data.Logic
import com.mostafa.quitchallenge.data.Trigger
import com.mostafa.quitchallenge.data.UrgeLog
import com.mostafa.quitchallenge.data.bnDigits
import com.mostafa.quitchallenge.data.computeInsights
import com.mostafa.quitchallenge.data.formatDuration
import com.mostafa.quitchallenge.data.formatGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class LogicTest {
    private fun challenge(start: Long?, goalHours: Int = 24, attempts: List<Attempt> = emptyList()) = Challenge(
        id = "c1",
        name = "Test",
        category = Category.OTHER,
        goalHours = goalHours,
        createdAt = 0,
        currentStart = start,
        attempts = attempts,
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
    fun formatsInBengali() {
        assertEquals("১২৩", "123".bnDigits())
        assertEquals("৭ দিন", formatGoal(168))
        assertEquals("১ দিন ৬ ঘণ্টা", formatGoal(30))
        assertEquals("১ দিন ২ ঘণ্টা ৩ মিনিট", formatDuration(26 * HOUR_MS + 3 * 60_000))
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
            urges = listOf(UrgeLog("u1", "c1", morning, 3, listOf(Trigger.STRESS, Trigger.BOREDOM))),
        )
        val insights = computeInsights(data, now = 0, zone = ZoneOffset.UTC)
        assertEquals(Trigger.STRESS to 2, insights.triggerCounts.first())
        assertEquals(1, insights.dayPartCounts.first { it.first == DayPart.NIGHT }.second)
        assertEquals(1, insights.dayPartCounts.first { it.first == DayPart.MORNING }.second)
        assertEquals(50, insights.successRate)
        assertEquals(1, insights.urgesHandled)
    }
}
