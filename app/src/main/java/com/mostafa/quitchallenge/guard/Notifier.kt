package com.mostafa.quitchallenge.guard

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mostafa.quitchallenge.MainActivity
import com.mostafa.quitchallenge.R

object Notifier {
    private const val CHANNEL = "challenge_alerts"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Challenge alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Break reminders, time limits and challenge updates"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun show(
        context: Context,
        id: Int,
        title: String,
        text: String,
        actions: List<Pair<String, PendingIntent>> = emptyList(),
    ) {
        if (!canPost(context)) return
        createChannel(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
        actions.forEach { (label, intent) -> builder.addAction(0, label, intent) }
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }
}
