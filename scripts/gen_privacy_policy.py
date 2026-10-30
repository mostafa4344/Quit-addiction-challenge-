#!/usr/bin/env python3
"""Writes the privacy policy to both places from one source, so they always match:
app/src/main/java/com/mostafa/quitchallenge/data/PrivacyPolicy.kt and docs/privacy-policy.html.
Run from the project folder: python3 scripts/gen_privacy_policy.py
"""
import html, json, sys

import os
REPO = "Quit-addiction-challenge-"  # GitHub repository name, part of the GitHub Pages URL
os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
EFFECTIVE = "30 October 2026"
# Public contact. To use an email instead, put it here and re-run this script.
CONTACT = "open an issue at https://github.com/mostafa4344/Quit-addiction-challenge-/issues (please don't post private details there)"

INTRO = ("Quit Challenge (\"the app\") helps you quit habits and addictions with timed challenges, check-ins and "
         "app time limits. It is built so that your information never leaves your phone. This policy explains "
         "what the app stores, why it asks for each permission, and the choices you have.")

SECTIONS = [
    ("Summary", [
        "We do not collect, sell or share any personal data.",
        "There is no account, no server and no sign-in. Everything you enter stays in the app's private storage on your phone.",
        "The app does not have the Internet permission, so it cannot send your data anywhere.",
        "There are no ads, analytics, crash reporting or third-party tracking tools in the app.",
    ]),
    ("Information stored on your phone", [
        "Your challenges: names, goals, reasons for quitting, start dates, attempts, slips, check-ins and notes you write.",
        "Urge logs, the pressures you select (for example stress or boredom), your if-then plans and optional time or money estimates.",
        "For app time-limit challenges: the names of the apps you choose to limit and how long you used them each day, calculated on the phone.",
        "Your settings, such as check-in times. If you turn on the app lock, a salted, one-way hash of your PIN (never the PIN itself).",
        "This information is used only to show your progress, statistics and reminders inside the app.",
    ]),
    ("Permissions and why they are used", [
        "Usage access: to measure how long you use the apps you choose to limit, the same data Android's Digital Wellbeing uses. It is read on the phone and never sent anywhere.",
        "List of installed apps: to let you pick which apps to limit. Only apps you select are saved.",
        "Notifications: for your check-ins, break reminders, milestones and encouragement.",
        "Display over other apps: to show a full-screen \"Time for a break\" or \"Time limit crossed\" screen over the app you are using.",
        "Battery optimisation settings: the app may ask you to allow it to run unrestricted so reminders arrive on time. You decide.",
        "Run at startup: to reschedule your check-in reminders after the phone restarts.",
        "Fingerprint / face unlock: optional, to open the app lock. The app never receives your biometric data; Android only tells it whether unlock succeeded.",
        "You can turn any of these off at any time in Android Settings. Features that depend on them will then stop working.",
    ]),
    ("Backups", [
        "Save backup creates a file with your data in a location you choose (for example Google Drive). That file is under your control; keep it private.",
        "Android may also back up the app's data to your Google account through Android's built-in backup, if you have it turned on. That backup is handled by Google under Google's privacy policy. You can turn it off in your phone's backup settings.",
    ]),
    ("Security", [
        "Your data is kept in the app's private storage, which other apps cannot read.",
        "With the app lock on, the app asks for your PIN or fingerprint, re-locks after a minute in the background and hides its content from the recent apps screen.",
    ]),
    ("Your choices and deleting your data", [
        "Delete a challenge in the app to remove it and its logs.",
        "Clear the app's storage in Android Settings, or uninstall the app, to delete all data on the phone.",
        "Because we do not hold any of your data, there is nothing stored with us to request or delete.",
    ]),
    ("Children", [
        "The app is not directed at children under 13 and is not designed to be used by them.",
    ]),
    ("Changes to this policy", [
        "If this policy changes, the new version will be published on this page and in the app with a new effective date.",
    ]),
    ("Contact", [
        f"Questions about this policy or the app: {CONTACT}",
    ]),
]

# ---- In-app copy (Kotlin)
kt = ['package com.mostafa.quitchallenge.data', '',
      '// Generated together with docs/privacy-policy.html so both always say the same thing.', '',
      f'const val PRIVACY_EFFECTIVE_DATE = {json.dumps(EFFECTIVE)}', '',
      f'const val PRIVACY_POLICY_URL = {json.dumps(f"https://mostafa4344.github.io/{REPO}/privacy-policy.html")}', '',
      f'const val PRIVACY_INTRO = {json.dumps(INTRO, ensure_ascii=False)}', '',
      'val PRIVACY_SECTIONS: List<Pair<String, List<String>>> = listOf(']
for title, items in SECTIONS:
    kt.append(f'    {json.dumps(title)} to listOf(')
    for it in items:
        kt.append(f'        {json.dumps(it, ensure_ascii=False).replace("$", "${\'$\'}")},')
    kt.append('    ),')
kt.append(')')
open('app/src/main/java/com/mostafa/quitchallenge/data/PrivacyPolicy.kt', 'w').write('\n'.join(kt) + '\n')

# ---- Hosted page
body = []
for title, items in SECTIONS:
    body.append(f'    <h2>{html.escape(title)}</h2>\n    <ul>')
    body += [f'      <li>{html.escape(i)}</li>' for i in items]
    body.append('    </ul>')
page = f'''<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Quit Challenge Privacy Policy</title>
  <style>
    :root {{ --bg: #f5faf8; --text: #1a2321; --muted: #4a5a57; --accent: #00897b; --card: #ffffff; }}
    @media (prefers-color-scheme: dark) {{
      :root {{ --bg: #0f1514; --text: #e3ecea; --muted: #a9b8b5; --accent: #4db6ac; --card: #172120; }}
    }}
    body {{ margin: 0; background: var(--bg); color: var(--text); font: 16px/1.6 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }}
    main {{ max-width: 760px; margin: 0 auto; padding: 32px 16px 64px; }}
    h1 {{ font-size: 1.9rem; margin: 0 0 4px; }}
    h2 {{ font-size: 1.2rem; margin: 32px 0 8px; color: var(--accent); }}
    .meta {{ color: var(--muted); margin: 0 0 24px; }}
    .intro {{ background: var(--card); border-left: 4px solid var(--accent); padding: 16px; border-radius: 8px; }}
    li {{ margin: 6px 0; }}
  </style>
</head>
<body>
  <main>
    <h1>Quit Challenge Privacy Policy</h1>
    <p class="meta">Effective date: {EFFECTIVE}</p>
    <p class="intro">{html.escape(INTRO)}</p>
{chr(10).join(body)}
  </main>
</body>
</html>
'''
open('docs/privacy-policy.html', 'w').write(page)
print("ok")
