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

    private var isShuffle = false
    private var originalList = ArrayList<String>()
    private var originalTitles = ArrayList<String>()

    override fun onCreate() {
        super.onCreate()

        playerManager = MediaPlayerManager(this)
        notificationManager = MusicNotificationManager(this)

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

                // Lưu playlist gốc để phục hồi khi tắt shuffle
                originalList = ArrayList(musicList)
                originalTitles = ArrayList(musicTitles)

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

            "ACTION_TOGGLE_PLAY" -> {
                if (playerManager.isPlaying()) playerManager.pause()
                else playerManager.play()
                updateNotification()
            }

            "ACTION_PLAY_AT" -> {
                val index = intent.getIntExtra("index", 0)
                playerManager.playAt(index)
            }

            "ACTION_SHUFFLE" -> toggleShuffle()
        }

        return START_STICKY
    }

    private fun toggleShuffle() {
        if (musicList.isEmpty()) return  // tránh crash nếu playlist trống

        isShuffle = !isShuffle
        if (isShuffle) enableShuffle() else disableShuffle()
        broadcastShuffleState()
    }

    private fun enableShuffle() {
        if (musicList.isEmpty()) return

        val currentIndex = playerManager.getCurrentIndex()
        if (currentIndex < 0 || currentIndex >= musicList.size) return

        val currentSong = musicList[currentIndex]
        val currentTitle = musicTitles[currentIndex]

        val newList = ArrayList(musicList)
        val newTitles = ArrayList(musicTitles)

        newList.removeAt(currentIndex)
        newTitles.removeAt(currentIndex)

        fisherYatesShuffle(newList, newTitles)

        newList.add(0, currentSong)
        newTitles.add(0, currentTitle)

        musicList = newList
        musicTitles = newTitles

        playerManager.setPlaylist(musicList, musicTitles)
        playerManager.playAt(0)

        broadcastPlaylistChanged()
    }

    private fun disableShuffle() {
        if (musicList.isEmpty()) return

        val currentSong = musicList.getOrNull(playerManager.getCurrentIndex()) ?: return

        musicList = ArrayList(originalList)
        musicTitles = ArrayList(originalTitles)

        playerManager.setPlaylist(musicList, musicTitles)

        val newIndex = musicList.indexOf(currentSong).coerceAtLeast(0)
        playerManager.playAt(newIndex)

        broadcastPlaylistChanged()
    }

    private fun fisherYatesShuffle(list: ArrayList<String>, titles: ArrayList<String>) {
        val random = java.util.Random()
        for (i in list.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)

            val tmpPath = list[i]
            list[i] = list[j]
            list[j] = tmpPath

            val tmpTitle = titles[i]
            titles[i] = titles[j]
            titles[j] = tmpTitle
        }
    }

    private fun broadcastShuffleState() {
        val intent = Intent("SHUFFLE_STATE_CHANGED")
        intent.putExtra("enabled", isShuffle)
        sendBroadcast(intent)
    }

    private fun broadcastSongChanged() {
        val intent = Intent("MUSIC_CHANGED")
        intent.putExtra("index", playerManager.getCurrentIndex())
        sendBroadcast(intent)
    }

    private fun broadcastPlaylistChanged() {
        val intent = Intent("PLAYLIST_CHANGED").apply {
            putStringArrayListExtra("music_list", musicList)
            putStringArrayListExtra("music_titles", musicTitles)
        }
        sendBroadcast(intent)
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
