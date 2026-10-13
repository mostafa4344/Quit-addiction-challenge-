package com.mostafa.quitchallenge.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** PIN + optional fingerprint/face lock for the whole app. */
object AppLock {
    private const val PREFS = "app_lock"
    private const val KEY_HASH = "pin_hash"
    private const val KEY_SALT = "pin_salt"
    private const val KEY_LENGTH = "pin_length"
    private const val KEY_BIOMETRIC = "biometric"
    private const val KEY_FAILS = "fails"
    private const val KEY_LOCKED_UNTIL = "locked_until"

    /** Re-lock only after the app has been in the background this long, so granting a permission doesn't re-lock it. */
    const val RELOCK_AFTER_MS = 60_000L
    private const val MAX_TRIES = 5
    private const val COOLDOWN_MS = 30_000L

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).contains(KEY_HASH)

    fun pinLength(context: Context): Int = prefs(context).getInt(KEY_LENGTH, 4)

    fun setPin(context: Context, pin: String) {
        require(PinHasher.isValidPin(pin))
        val salt = PinHasher.newSalt()
        prefs(context).edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, PinHasher.hash(pin, salt))
            .putInt(KEY_LENGTH, pin.length)
            .remove(KEY_FAILS)
            .remove(KEY_LOCKED_UNTIL)
            .apply()
    }

    fun disable(context: Context) {
        prefs(context).edit().clear().apply()
    }

    sealed interface Result {
        data object Ok : Result
        data class Wrong(val triesLeft: Int) : Result
        data class CoolingDown(val secondsLeft: Int) : Result
    }

    fun cooldownSeconds(context: Context, now: Long = System.currentTimeMillis()): Int {
        val until = prefs(context).getLong(KEY_LOCKED_UNTIL, 0L)
        return if (until > now) ((until - now + 999) / 1000).toInt() else 0
    }

    fun verify(context: Context, pin: String, now: Long = System.currentTimeMillis()): Result {
        val p = prefs(context)
        cooldownSeconds(context, now).takeIf { it > 0 }?.let { return Result.CoolingDown(it) }
        val salt = p.getString(KEY_SALT, null) ?: return Result.Ok
        val hash = p.getString(KEY_HASH, null) ?: return Result.Ok
        if (PinHasher.matches(pin, salt, hash)) {
            p.edit().remove(KEY_FAILS).apply()
            return Result.Ok
        }
        val fails = p.getInt(KEY_FAILS, 0) + 1
        return if (fails >= MAX_TRIES) {
            p.edit().putInt(KEY_FAILS, 0).putLong(KEY_LOCKED_UNTIL, now + COOLDOWN_MS).apply()
            Result.CoolingDown((COOLDOWN_MS / 1000).toInt())
        } else {
            p.edit().putInt(KEY_FAILS, fails).apply()
            Result.Wrong(MAX_TRIES - fails)
        }
    }

    fun biometricAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    fun biometricEnabled(context: Context): Boolean =
        isEnabled(context) && prefs(context).getBoolean(KEY_BIOMETRIC, false) && biometricAvailable(context)

    fun setBiometric(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    fun promptBiometric(activity: FragmentActivity, onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Quit Challenge")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }
}
