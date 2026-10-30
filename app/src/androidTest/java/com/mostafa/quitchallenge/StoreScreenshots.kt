package com.mostafa.quitchallenge

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.mostafa.quitchallenge.data.AppData
import com.mostafa.quitchallenge.data.AppGraph
import com.mostafa.quitchallenge.data.Attempt
import com.mostafa.quitchallenge.data.AttemptResult
import com.mostafa.quitchallenge.data.Category
import com.mostafa.quitchallenge.data.Challenge
import com.mostafa.quitchallenge.data.GuardSettings
import com.mostafa.quitchallenge.data.HOUR_MS
import com.mostafa.quitchallenge.data.MINUTE_MS
import com.mostafa.quitchallenge.data.Trigger
import com.mostafa.quitchallenge.data.UrgeLog
import com.mostafa.quitchallenge.guard.WarningActivity
import com.mostafa.quitchallenge.ui.Onboarding
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * Takes the Google Play screenshots with realistic demo data and a clean status bar.
 * Runs only when asked: -Pandroid.testInstrumentationRunnerArguments.storeShots=true
 */
@RunWith(AndroidJUnit4::class)
class StoreScreenshots {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val pkg = context.packageName

    private fun shell(cmd: String): String = device.executeShellCommand(cmd)

    private fun shot(name: String) {
        Thread.sleep(800)
        shell("screencap -p $DIR/$name.png")
    }

    private fun waitText(t: String, timeout: Long = 10_000) {
        device.wait(Until.hasObject(By.textContains(t)), timeout)
    }

    private fun tap(t: String) {
        waitText(t)
        device.findObject(By.textContains(t))?.click()
    }

    private fun scrollDown(times: Int = 1) {
        repeat(times) {
            val area = device.findObjects(By.scrollable(true)).maxByOrNull { it.visibleBounds.height() } ?: return
            val r = area.visibleBounds
            val x = r.left + r.width() / 8
            device.swipe(x, r.top + r.height() * 8 / 10, x, r.top + r.height() * 2 / 10, 80)
            Thread.sleep(500)
        }
    }

    private fun launch() {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(pkg).depth(0)), 15_000)
    }

    private fun cleanStatusBar() {
        shell("settings put global sysui_demo_allowed 1")
        fun demo(extra: String) = shell("am broadcast -a com.android.systemui.demo -e command $extra")
        demo("enter")
        demo("clock -e hhmm 0941")
        demo("battery -e level 100 -e plugged false")
        demo("network -e wifi show -e level 4 -e fully true")
        demo("network -e mobile show -e datatype none -e level 4")
        demo("notifications -e visible false")
    }

    private fun seed() {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val day = 24 * HOUR_MS
        fun at(daysAgo: Int, hour: Int, minute: Int = 0) =
            LocalDate.now(zone).minusDays(daysAgo.toLong()).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

        val social = Challenge(
            id = "demo-social",
            name = "No social media scrolling",
            category = Category.SOCIAL_MEDIA,
            goalHours = 30 * 24,
            why = "More time and attention for my family",
            createdAt = now - 26 * day,
            currentStart = now - 12 * day - 5 * HOUR_MS - 23 * MINUTE_MS,
            attempts = listOf(
                Attempt(now - 26 * day, now - 21 * day, AttemptResult.COMPLETED),
                Attempt(now - 21 * day, now - 12 * day - 6 * HOUR_MS, AttemptResult.FAILED, listOf(Trigger.BOREDOM, Trigger.TIREDNESS)),
            ),
            minutesPerDay = 150,
            lastCheckIn = now - 2 * HOUR_MS,
        )
        val smoking = Challenge(
            id = "demo-smoking",
            name = "Quit smoking",
            category = Category.SMOKING,
            goalHours = 21 * 24,
            why = "Breathe freely and play football again",
            createdAt = now - 6 * day,
            currentStart = now - 5 * day - 2 * HOUR_MS,
            moneyPerDay = 3.5,
            currency = "$",
        )
        val games = Challenge(
            id = "demo-games",
            name = "No games before bed",
            category = Category.GAMING,
            goalHours = 7 * 24,
            createdAt = now - 2 * day,
            currentStart = now - 1 * day - 19 * HOUR_MS,
        )
        val urgeTimes = listOf(
            Triple(1, 22, Trigger.BOREDOM), Triple(2, 23, Trigger.TIREDNESS), Triple(3, 13, Trigger.STRESS),
            Triple(4, 22, Trigger.BOREDOM), Triple(5, 21, Trigger.LONELINESS), Triple(6, 23, Trigger.BOREDOM),
            Triple(8, 18, Trigger.STRESS), Triple(9, 22, Trigger.TIREDNESS), Triple(10, 9, Trigger.HABIT),
            Triple(11, 22, Trigger.BOREDOM), Triple(13, 20, Trigger.SOCIAL), Triple(15, 23, Trigger.BOREDOM),
            Triple(16, 14, Trigger.STRESS), Triple(18, 22, Trigger.LONELINESS),
        )
        val urges = urgeTimes.mapIndexed { i, (daysAgo, hour, trigger) ->
            UrgeLog(
                id = "urge-$i",
                challengeId = if (i % 3 == 0) "demo-smoking" else "demo-social",
                time = at(daysAgo, hour, (i * 7) % 60),
                intensity = 6 + i % 4,
                triggers = listOf(trigger),
                resisted = true,
                intensityAfter = 2 + i % 3,
            )
        }
        AppGraph.repo(context).update {
            AppData(
                challenges = listOf(social, smoking, games),
                urges = urges,
                settings = GuardSettings(dailyCheckIn = true, checkInTimes = listOf(9 * 60, 21 * 60)),
                plans = mapOf(
                    Trigger.BOREDOM to "put the phone in another room and read 10 pages",
                    Trigger.STRESS to "walk around the block for 10 minutes",
                ),
            )
        }
    }

    @Test
    fun capture() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("storeShots") == "true")
        Configurator.getInstance().waitForIdleTimeout = 500
        shell("mkdir -p $DIR")
        // Every protection on, so screens show the app the way it's meant to be used.
        shell("pm grant $pkg android.permission.POST_NOTIFICATIONS")
        shell("appops set $pkg GET_USAGE_STATS allow")
        shell("appops set $pkg SYSTEM_ALERT_WINDOW allow")
        shell("dumpsys deviceidle whitelist +$pkg")
        context.deleteFile("data.json")
        AppGraph.reset()
        Onboarding.markDone(context)
        seed()
        cleanStatusBar()
        device.pressHome()

        launch()
        waitText("No social media scrolling")
        shot("01-home")

        tap("No social media scrolling")
        waitText("Goal: 30 days")
        shot("02-progress")

        tap("I feel an urge")
        waitText("Ride out the urge")
        shot("04-sos-start")
        tap("Start")
        waitText("The urge has passed")
        Thread.sleep(4_000)
        shot("05-sos-surf")
        device.pressBack()
        waitText("Goal: 30 days")
        scrollDown(2)
        shot("03-stats")
        device.pressBack()

        launch()
        tap("Insights")
        waitText("Pressure insights")
        shot("06-insights")
        scrollDown(2)
        shot("07-insights-when")

        tap("Protection")
        waitText("Protection")
        scrollDown(1)
        shot("08-protection")

        WarningActivity.show(
            context,
            "⏳",
            "Time for a break",
            "You've used Instagram for 25 of your 30 minutes today.",
            "demo-social",
        )
        waitText("Time for a break")
        shot("09-break-screen")
        device.pressBack()

        launch()
        device.wait(Until.hasObject(By.res("new_challenge")), 10_000)
        device.findObject(By.res("new_challenge"))?.click()
        waitText("Challenge type")
        shot("10-new-challenge")

        shell("am broadcast -a com.android.systemui.demo -e command exit")
    }

    companion object {
        private const val DIR = "/data/local/tmp/store-shots"
    }
}
