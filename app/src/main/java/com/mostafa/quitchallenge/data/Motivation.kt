package com.mostafa.quitchallenge.data

import java.time.LocalDate

private val QUOTES = listOf(
    "Every big change starts with one small 'no'.",
    "You are stronger than your habit.",
    "An urge is like a wave: it rises, peaks, and passes. Just hold on for 15 minutes.",
    "Falling down isn't failure. Staying down is.",
    "Today's struggle is tomorrow's strength.",
    "You don't have to prove anything to anyone. Just win today.",
    "Discipline is remembering what you want most.",
    "One day at a time, a whole life changes.",
    "The time you get back is your real reward.",
    "A broken streak still leaves the lesson. Start again, smarter.",
)

private val RESTART_MESSAGES = listOf(
    "No problem! Falling means you were trying. Your new attempt starts right now 💪",
    "This attempt showed you which pressure makes you weak. Be ready for it this time.",
    "A fresh start is a chance for a new record! Beat your previous best streak 🚀",
)

fun dailyQuote(date: LocalDate = LocalDate.now()): String = QUOTES[date.dayOfYear % QUOTES.size]

fun restartMessage(attemptNumber: Int): String = RESTART_MESSAGES[attemptNumber % RESTART_MESSAGES.size]

fun progressMessage(progress: Float): String = when {
    progress < 0.10f -> "The start is the hardest part, and you're getting through it!"
    progress < 0.25f -> "Going great! The first steps are behind you."
    progress < 0.50f -> "Almost halfway! Your brain is adapting to the new habit."
    progress < 0.75f -> "More than half done! This is no time to give up."
    progress < 1.00f -> "The goal is so close! Stay strong for the last stretch 🔥"
    else -> "You did it! 🏆"
}
