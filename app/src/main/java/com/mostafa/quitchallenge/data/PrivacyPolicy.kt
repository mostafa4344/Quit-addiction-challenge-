package com.mostafa.quitchallenge.data

// Generated together with docs/privacy-policy.html so both always say the same thing.

const val PRIVACY_EFFECTIVE_DATE = "30 October 2026"

const val PRIVACY_POLICY_URL = "https://mostafa4344.github.io/Quit-addiction-challenge-/privacy-policy.html"

const val PRIVACY_INTRO = "Quit Challenge (\"the app\") helps you quit habits and addictions with timed challenges, check-ins and app time limits. It is built so that your information never leaves your phone. This policy explains what the app stores, why it asks for each permission, and the choices you have."

val PRIVACY_SECTIONS: List<Pair<String, List<String>>> = listOf(
    "Summary" to listOf(
        "We do not collect, sell or share any personal data.",
        "There is no account, no server and no sign-in. Everything you enter stays in the app's private storage on your phone.",
        "The app does not have the Internet permission, so it cannot send your data anywhere.",
        "There are no ads, analytics, crash reporting or third-party tracking tools in the app.",
    ),
    "Information stored on your phone" to listOf(
        "Your challenges: names, goals, reasons for quitting, start dates, attempts, slips, check-ins and notes you write.",
        "Urge logs, the pressures you select (for example stress or boredom), your if-then plans and optional time or money estimates.",
        "For app time-limit challenges: the names of the apps you choose to limit and how long you used them each day, calculated on the phone.",
        "Your settings, such as check-in times. If you turn on the app lock, a salted, one-way hash of your PIN (never the PIN itself).",
        "This information is used only to show your progress, statistics and reminders inside the app.",
    ),
    "Permissions and why they are used" to listOf(
        "Usage access: to measure how long you use the apps you choose to limit, the same data Android's Digital Wellbeing uses. It is read on the phone and never sent anywhere.",
        "List of installed apps: to let you pick which apps to limit. Only apps you select are saved.",
        "Notifications: for your check-ins, break reminders, milestones and encouragement.",
        "Display over other apps: to show a full-screen \"Time for a break\" or \"Time limit crossed\" screen over the app you are using.",
        "Battery optimisation settings: the app may ask you to allow it to run unrestricted so reminders arrive on time. You decide.",
        "Run at startup: to reschedule your check-in reminders after the phone restarts.",
        "Fingerprint / face unlock: optional, to open the app lock. The app never receives your biometric data; Android only tells it whether unlock succeeded.",
        "You can turn any of these off at any time in Android Settings. Features that depend on them will then stop working.",
    ),
    "Backups" to listOf(
        "Save backup creates a file with your data in a location you choose (for example Google Drive). That file is under your control; keep it private.",
        "Android may also back up the app's data to your Google account through Android's built-in backup, if you have it turned on. That backup is handled by Google under Google's privacy policy. You can turn it off in your phone's backup settings.",
    ),
    "Security" to listOf(
        "Your data is kept in the app's private storage, which other apps cannot read.",
        "With the app lock on, the app asks for your PIN or fingerprint, re-locks after a minute in the background and hides its content from the recent apps screen.",
    ),
    "Your choices and deleting your data" to listOf(
        "Delete a challenge in the app to remove it and its logs.",
        "Clear the app's storage in Android Settings, or uninstall the app, to delete all data on the phone.",
        "Because we do not hold any of your data, there is nothing stored with us to request or delete.",
    ),
    "Children" to listOf(
        "The app is not directed at children under 13 and is not designed to be used by them.",
    ),
    "Changes to this policy" to listOf(
        "If this policy changes, the new version will be published on this page and in the app with a new effective date.",
    ),
    "Contact" to listOf(
        "Questions about this policy or the app: open an issue at https://github.com/mostafa4344/Quit-addiction-challenge-/issues (please don't post private details there)",
    ),
)
