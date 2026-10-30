package com.mostafa.quitchallenge

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.mostafa.quitchallenge.data.AppGraph
import com.mostafa.quitchallenge.data.Category
import com.mostafa.quitchallenge.data.Challenge
import com.mostafa.quitchallenge.data.GuardSettings
import com.mostafa.quitchallenge.data.HOUR_MS
import com.mostafa.quitchallenge.guard.CheckIn
import com.mostafa.quitchallenge.guard.CheckInAlarm
import com.mostafa.quitchallenge.guard.CheckInReceiver
import com.mostafa.quitchallenge.guard.LimitMonitor
import com.mostafa.quitchallenge.guard.MilestoneNotifier
import com.mostafa.quitchallenge.guard.ProtectionWatch
import com.mostafa.quitchallenge.guard.RiskReminder
import com.mostafa.quitchallenge.security.AppLock
import com.mostafa.quitchallenge.ui.Onboarding
import com.mostafa.quitchallenge.widget.ChallengeWidget
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

/**
 * Drives the installed app the way a person would (tapping visible text), on every
 * emulator in CI. Screenshots of each step go to /data/local/tmp/qc-shots for review.
 */
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val pkg = context.packageName

    @Before
    fun freshApp() {
        // The UI updates every second; don't wait long for a "quiet" screen.
        Configurator.getInstance().waitForIdleTimeout = 500
        device.pressHome()
        context.deleteFile("data.json")
        listOf("onboarding", "app_lock", "check_in", "limit_alerts", "milestone_notified", "risk_reminder", "protection_watch")
            .forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        AppGraph.reset()
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant $pkg android.permission.POST_NOTIFICATIONS")
        shell("mkdir -p $SHOTS")
    }

    // ---------------------------------------------------------------- helpers

    private fun shell(cmd: String): String = device.executeShellCommand(cmd)

    private fun shot(name: String) {
        shell("screencap -p $SHOTS/api${Build.VERSION.SDK_INT}-$name.png")
    }

    private fun launch() {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        assertTrue("app did not open", device.wait(Until.hasObject(By.pkg(pkg).depth(0)), 15_000))
    }

    private fun find(selector: BySelector, what: String, timeout: Long = 10_000): UiObject2 =
        device.wait(Until.findObject(selector), timeout) ?: run {
            shot("MISSING-${what.filter { it.isLetterOrDigit() }.take(30)}")
            fail("Not on screen: \"$what\"")
            error("unreachable")
        }

    private fun text(t: String, timeout: Long = 10_000) = find(By.textContains(t), t, timeout)

    private fun tap(t: String) = text(t).click()

    /**
     * Scrolls until [t] is visible. Swipes along the left edge of the scrollable area so the
     * gesture never starts on a floating button.
     */
    private fun scrollTo(t: String): UiObject2 {
        device.findObject(By.textContains(t))?.let { return it }
        repeat(15) {
            val area = device.findObjects(By.scrollable(true)).maxByOrNull { it.visibleBounds.height() }
                ?: return text(t)
            val r = area.visibleBounds
            val x = r.left + r.width() / 8
            device.swipe(x, r.top + r.height() * 3 / 4, x, r.top + r.height() / 3, 20)
            if (device.findObject(By.textContains(t)) != null) {
                // Let the scroll settle before handing back an object to click.
                Thread.sleep(700)
                return text(t, 2_000)
            }
        }
        return text(t, 2_000)
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        for (i in 0 until 50) {
            if (condition()) return
            Thread.sleep(100)
        }
        shot("MISSING-${what.filter { it.isLetterOrDigit() }.take(30)}")
        fail("Never happened: $what")
    }

    private fun newChallenge() = find(By.res("new_challenge"), "New challenge button").click()

    private fun skipOnboarding() {
        Onboarding.markDone(context)
    }

    private fun createStayAwayChallenge() {
        newChallenge()
        text("Challenge type")
        shot("new-challenge-top")
        scrollTo("I commit to this challenge").click()
        scrollTo("Start challenge").click()
        text("Goal: 7 days")
    }

    // ---------------------------------------------------------------- tests

    @Test
    fun firstRun_onboardingLeadsToHomeWithSetupBanner() {
        launch()
        text("Welcome to Quit Challenge")
        shot("01-onboarding-welcome")
        tap("Next")
        text("Set up protection")
        shot("02-onboarding-permissions")
        tap("Next")
        text("Keep it private")
        shot("03-onboarding-lock")
        tap("Start using the app")
        text("Protection isn't fully on")
        shot("04-home-empty")
        scrollTo("No challenges yet")
    }

    @Test
    fun stayAwayChallenge_slipCheckInSosRotateDelete() {
        skipOnboarding()
        launch()
        createStayAwayChallenge()
        shot("10-detail-new")

        // Slip: only the streak restarts, with a supportive message.
        tap("I slipped")
        text("Slipped? That's okay")
        shot("11-slip-dialog")
        scrollTo("Restart my streak").click()
        text("A fresh start")
        shot("12-fresh-start")
        tap("OK")

        // Check in from inside the app.
        scrollTo("Check in: I stayed on track today").click()
        text("Checked in today")

        // SOS urge surfing, end to end.
        tap("I feel an urge")
        text("Ride out the urge")
        shot("13-sos-rate")
        tap("Start")
        text("The urge has passed")
        shot("14-sos-surf")
        tap("The urge has passed")
        text("You rode it out")
        scrollTo("Save").click()
        text("Goal: 7 days")

        // History keeps the slip.
        scrollTo("Slipped, then restarted")
        shot("15-detail-history")

        // Rotation keeps the screen and the data.
        device.setOrientationLeft()
        text("no social media")
        shot("16-detail-landscape")
        device.setOrientationNatural()

        // Delete.
        find(By.desc("Delete"), "Delete icon").click()
        text("Delete this challenge?")
        find(By.text("Delete"), "Delete button").click()
        scrollTo("No challenges yet")
    }

    @Test
    fun appLimitChallenge_pickAppAndSeeUsageCard() {
        skipOnboarding()
        launch()
        newChallenge()
        tap("App time limit")
        scrollTo("Choose apps").click()
        text("Search apps")
        shot("20-app-picker")
        // "Settings" is near the end of the alphabetical list.
        scrollTo("Settings").click()
        tap("Done")
        scrollTo("I commit to this challenge").click()
        scrollTo("Start challenge").click()
        text("Today's app time")
        shot("21-app-limit-no-access")

        shell("appops set $pkg GET_USAGE_STATS allow")
        device.pressBack()
        scrollTo("day on Settings").click() // open the challenge card again
        text("used of", 40_000)
        shot("22-app-limit-usage")
    }

    @Test
    fun tabs_insightsProtectionCheckInTimesAndPrivacy() {
        skipOnboarding()
        launch()
        find(By.text("Insights"), "Insights tab").click()
        text("Pressure insights")
        shot("30-insights")
        find(By.text("Protection"), "Protection tab").click()
        text("Protection 🛡")
        shot("31-protection-top")
        scrollTo("Twice").click()
        waitFor("check-in times saved") { AppGraph.repo(context).data.value.settings.checkInTimes == listOf(9 * 60, 21 * 60) }
        shot("32-check-in-times")
        scrollTo("Privacy policy: your data never leaves this phone").click()
        text("Effective date")
        shot("33-privacy")
        device.pressBack()
        text("Protection 🛡")
    }

    @Test
    fun appLock_pinUnlocksTheApp() {
        skipOnboarding()
        AppLock.setPin(context, "1234")
        try {
            launch()
            text("Quit Challenge is locked")
            shot("40-lock-screen")
            listOf("1", "2", "3", "4").forEach { find(By.text(it), "key $it").click() }
            scrollTo("No challenges yet")
        } finally {
            AppLock.disable(context)
        }
    }

    @Test
    fun backgroundWork_checkInNotificationAndAnswer() {
        val now = System.currentTimeMillis()
        val minute = LocalTime.now().let { it.hour * 60 + it.minute }
        val repo = AppGraph.repo(context)
        repo.update {
            it.copy(
                challenges = listOf(
                    Challenge(
                        id = "bg-test",
                        name = "Background test",
                        category = Category.OTHER,
                        goalHours = 168,
                        createdAt = now - 3 * HOUR_MS,
                        currentStart = now - 3 * HOUR_MS,
                    ),
                ),
                settings = GuardSettings(dailyCheckIn = true, checkInTimes = listOf(minute)),
            )
        }

        // None of the background jobs may crash, with or without permissions.
        LimitMonitor.check(context)
        ProtectionWatch.check(context)
        MilestoneNotifier.check(context)
        RiskReminder.check(context)
        ChallengeWidget.updateAll(context)
        CheckInAlarm.schedule(context)

        CheckIn.check(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        val posted = nm.activeNotifications.any {
            it.notification.extras.getString(Notification.EXTRA_TITLE)?.contains("Daily check-in") == true
        }
        assertTrue("check-in notification was not posted", posted)
        device.openNotification()
        device.wait(Until.hasObject(By.textContains("Daily check-in")), 5_000)
        shot("50-check-in-notification")
        device.pressBack()

        // Answer "on track" the way the notification button does.
        context.sendBroadcast(
            Intent(context, CheckInReceiver::class.java)
                .setAction(CheckIn.ACTION_ON_TRACK)
                .putExtra(CheckIn.EXTRA_ID, "bg-test"),
        )
        var checkedIn: Long? = null
        for (i in 0 until 50) {
            checkedIn = repo.data.value.challenges.first().lastCheckIn
            if (checkedIn != null) break
            Thread.sleep(100)
        }
        assertNotNull("check-in answer was not saved", checkedIn)
    }

    companion object {
        private const val SHOTS = "/data/local/tmp/qc-shots"
    }
}
