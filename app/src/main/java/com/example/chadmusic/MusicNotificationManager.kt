package com.example.chadmusic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle

class MusicNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "music_channel"
        private const val NOTIFICATION_ID = 1002
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun createNotification(
        title: String,
        isPlaying: Boolean
    ): Notification {

        // PREVIOUS
        val prevIntent = Intent(context, MusicService::class.java).apply {
            action = "ACTION_PREVIOUS"
        }
        val prevPending = PendingIntent.getService(
            context, 0, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PLAY / PAUSE
        val playIntent = Intent(context, MusicService::class.java).apply {
            action = if (isPlaying) "ACTION_PAUSE" else "ACTION_PLAY"
        }
        val playPending = PendingIntent.getService(
            context, 1, playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // NEXT
        val nextIntent = Intent(context, MusicService::class.java).apply {
            action = "ACTION_NEXT"
        }
        val nextPending = PendingIntent.getService(
            context, 2, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mở app khi bấm vào notification
        val openIntent = Intent(context, MainActivity::class.java)
        val contentPending = PendingIntent.getActivity(
            context, 3, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ảnh album
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "Đang phát nhạc" else "Tạm dừng")
            .setContentIntent(contentPending)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(isPlaying)
            .addAction(R.drawable.ic_prev, "Previous", prevPending)
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                playPending
            )
            .addAction(R.drawable.ic_next, "Next", nextPending)
            .setStyle(
                MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    fun updateNotification(
        title: String,
        isPlaying: Boolean
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(title, isPlaying))
    }

    fun getNotificationId(): Int = NOTIFICATION_ID
}
