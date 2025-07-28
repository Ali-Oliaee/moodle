package com.example.moodle.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.example.moodle.MainActivity
import com.example.moodle.R

object NotificationHelper {

    private const val CHANNEL_ID = "screen_recording_channel"

    fun createNotification(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.screen_recording_title))
            .setContentText(context.getString(R.string.screen_recording_in_progress))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.screen_recording_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = context.getSystemService<NotificationManager>()
            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }
}