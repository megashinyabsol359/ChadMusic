package com.example.chadmusic

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.util.Log

class MusicService : Service() {

    private val TAG = "MusicService"

    private lateinit var playerManager: MediaPlayerManager
    private lateinit var notificationManager: MusicNotificationManager
    private lateinit var mediaSession: MediaSession

    private var musicList = ArrayList<String>()
    private var musicTitles = ArrayList<String>()

    override fun onCreate() {
        super.onCreate()

        playerManager = MediaPlayerManager(this)
        notificationManager = MusicNotificationManager(this)

        mediaSession = MediaSession(this, "ChadMusicSession")
        val stateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS
            )
        mediaSession.setPlaybackState(stateBuilder.build())
        mediaSession.isActive = true

        Log.d(TAG, "MusicService created")
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val action = intent?.action
        Log.d(TAG, "Received action: $action")

        when (action) {

            // bắt đầu phát
            "ACTION_START" -> {
                musicList = intent.getStringArrayListExtra("music_list") ?: ArrayList()
                musicTitles = intent.getStringArrayListExtra("music_titles") ?: ArrayList()

                if (musicList.isNotEmpty()) {
                    playerManager.setPlaylist(musicList, musicTitles)
                    playerManager.play()

                    startForeground(
                        notificationManager.getNotificationId(),
                        notificationManager.createNotification(
                            playerManager.getCurrentTitle(),
                            true
                        )
                    )
                }
            }

            "ACTION_PREVIOUS" -> {
                playerManager.previous()
                updateNotification()
            }

            "ACTION_PLAY" -> {
                playerManager.play()
                updateNotification()
            }

            "ACTION_PAUSE" -> {
                playerManager.pause()
                updateNotification()
            }

            "ACTION_NEXT" -> {
                playerManager.next()
                updateNotification()
            }
        }

        return START_STICKY
    }


    private fun updateNotification() {
        val notification = notificationManager.createNotification(
            playerManager.getCurrentTitle(),
            playerManager.isPlaying()
        )

        startForeground(notificationManager.getNotificationId(), notification)
    }


    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
        mediaSession.release()
        Log.d(TAG, "MusicService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
