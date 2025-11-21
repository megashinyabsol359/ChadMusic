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

        // Callback khi bài hát thay đổi
        playerManager.onSongChanged = { index ->
            broadcastSongChanged()
            updateNotification()
        }

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

            "ACTION_START" -> {
                musicList = intent.getStringArrayListExtra("music_list") ?: ArrayList()
                musicTitles = intent.getStringArrayListExtra("music_titles") ?: ArrayList()

                if (musicList.isNotEmpty()) {
                    playerManager.setPlaylist(musicList, musicTitles)
                    playerManager.play()
                    startForeground(
                        notificationManager.getNotificationId(),
                        notificationManager.createNotification(playerManager.getCurrentTitle(), true)
                    )
                    broadcastSongChanged()
                }
            }

            "ACTION_PREVIOUS" -> playerManager.previous()
            "ACTION_NEXT" -> playerManager.next()

            "ACTION_PLAY" -> playerManager.play()
            "ACTION_PAUSE" -> playerManager.pause()

            // -------- Toggle Play/Pause từ notification ----------
            "ACTION_TOGGLE_PLAY" -> {
                if (playerManager.isPlaying()) playerManager.pause()
                else playerManager.play()
                updateNotification()
            }

            // --------- Phát bài theo index từ RecyclerView ----------
            "ACTION_PLAY_AT" -> {
                val index = intent.getIntExtra("index", 0)
                playerManager.playAt(index)
            }
        }

        return START_STICKY
    }

    // Gửi broadcast bài đang phát để update UI
    private fun broadcastSongChanged() {
        val intent = Intent("MUSIC_CHANGED")
        intent.putExtra("index", playerManager.getCurrentIndex())
        sendBroadcast(intent)
    }

    // Cập nhật notification, icon Play/Pause luôn đồng bộ
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
