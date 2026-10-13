package com.mostafa.quitchallenge.data

import kotlinx.serialization.Serializable

/** Pressures that push someone back toward the habit, each with a coping tip. */
@Serializable
enum class Trigger(val label: String, val emoji: String, val tip: String) {
    STRESS("স্ট্রেস / চাপ", "😣", "৫ মিনিট গভীর শ্বাস নিন (৪ সেকেন্ড টানুন, ৭ সেকেন্ড ধরে রাখুন, ৮ সেকেন্ডে ছাড়ুন) অথবা একটু হেঁটে আসুন।"),
    BOREDOM("একঘেয়েমি", "😐", "আগে থেকে একটা 'বোর হলে করবো' লিস্ট রাখুন — বই পড়া, ব্যায়াম, নতুন কোনো স্কিল শেখা।"),
    LONELINESS("একাকীত্ব", "😔", "কোনো বন্ধু বা পরিবারের কাউকে ফোন দিন, মানুষের মাঝে বের হয়ে যান।"),
    TIREDNESS("ক্লান্তি / ঘুম কম", "🥱", "ক্লান্ত থাকলে মন দুর্বল হয় — সময়মতো ঘুমান, ফোন বিছানা থেকে দূরে রাখুন।"),
    ANXIETY("দুশ্চিন্তা", "😰", "চিন্তাগুলো খাতায় লিখে ফেলুন, তারপর ছোট একটা কাজ শেষ করুন।"),
    SOCIAL("বন্ধুদের চাপ", "👥", "আগে থেকেই 'না' বলার একটা বাক্য ঠিক করে রাখুন, ঝুঁকির জায়গা এড়িয়ে চলুন।"),
    HABIT("অভ্যাস / নির্দিষ্ট সময়", "⏰", "যে সময়ে বেশি ইচ্ছা হয়, সেই সময়ে অন্য একটা রুটিন বসিয়ে দিন।"),
    SADNESS("মন খারাপ", "😢", "অনুভূতিটা মেনে নিন, পছন্দের কাজ করুন বা কাছের কারো সাথে কথা বলুন।"),
    ANGER("রাগ", "😠", "জায়গা বদলান, ঠান্ডা পানি খান, ধীরে ধীরে ১০ পর্যন্ত গুনুন।"),
    CELEBRATION("আনন্দ / উদযাপন", "🎉", "উদযাপনের অন্য উপায় ঠিক করে রাখুন — পছন্দের খাবার, ঘোরাঘুরি।"),
}

@Serializable
enum class Category(val label: String, val emoji: String) {
    SOCIAL_MEDIA("সোশ্যাল মিডিয়া", "📱"),
    GAMING("মোবাইল / গেম", "🎮"),
    SMOKING("ধূমপান", "🚬"),
    PORN("পর্নোগ্রাফি", "🚫"),
    JUNK_FOOD("জাঙ্ক ফুড", "🍔"),
    OTHER("অন্যান্য", "⭐"),
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
) {
    val isActive: Boolean get() = currentStart != null
    val attemptNumber: Int get() = attempts.size + 1
    val failures: Int get() = attempts.count { it.result == AttemptResult.FAILED }
    val completions: Int get() = attempts.count { it.result == AttemptResult.COMPLETED }
}

/** An urge the user felt and resisted. */
@Serializable
data class UrgeLog(
    val id: String,
    val challengeId: String,
    val time: Long,
    val intensity: Int,
    val triggers: List<Trigger> = emptyList(),
    val note: String = "",
)

@Serializable
data class AppData(
    val challenges: List<Challenge> = emptyList(),
    val urges: List<UrgeLog> = emptyList(),
)
