package com.example.chadmusic

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

class MediaPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var playlist = ArrayList<String>()
    private var titleList = ArrayList<String>()
    private var currentIndex = 0

    private val TAG = "MediaPlayerManager"

    // Callback thông báo bài thay đổi
    var onSongChanged: ((index: Int) -> Unit)? = null

    fun setPlaylist(paths: ArrayList<String>, titles: ArrayList<String>) {
        playlist = paths
        titleList = titles
        currentIndex = 0
    }

    fun getCurrentIndex(): Int = currentIndex
    fun getCurrentTitle(): String = titleList.getOrNull(currentIndex) ?: "Không có tiêu đề"

    fun play() {
        if (playlist.isEmpty()) return

        val path = playlist[currentIndex]

        // Nếu đang pause → resume
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
            return
        }

        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            return
        }

        try {
            resetPlayer()
            mediaPlayer = MediaPlayer()

            mediaPlayer?.setDataSource(path)

            mediaPlayer?.setOnPreparedListener {
                it.start()
            }

            mediaPlayer?.setOnCompletionListener {
                next()
            }

            mediaPlayer?.prepareAsync()

        } catch (e: Exception) {
            Log.e(TAG, "Không thể phát file: $path")
            e.printStackTrace()
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun next() {
        if (playlist.isEmpty()) return

        currentIndex = (currentIndex + 1) % playlist.size
        onSongChanged?.invoke(currentIndex)
        forceRestartSong()
    }

    fun previous() {
        if (playlist.isEmpty()) return

        currentIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
        onSongChanged?.invoke(currentIndex)
        forceRestartSong()
    }

    // ------------------- HÀM MỚI: PHÁT BÀI THEO INDEX -------------------
    fun playAt(index: Int) {
        if (index in playlist.indices) {
            currentIndex = index
            onSongChanged?.invoke(currentIndex)
            forceRestartSong()
        }
    }
    // ----------------------------------------------------------------------

    private fun forceRestartSong() {
        resetPlayer()
        mediaPlayer = null
        play()
    }

    private fun resetPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
        } catch (e: Exception) {
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    fun release() {
        resetPlayer()
    }
}
