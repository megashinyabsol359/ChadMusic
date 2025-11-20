package com.example.chadmusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.chadmusic.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val REQUEST_CODE_STORAGE = 1001
    private val REQUEST_CODE_NOTIFICATION = 1002

    private var musicList = ArrayList<String>()
    private var musicTitles = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Xin quyền storage + notification
        checkStoragePermission()
        checkNotificationPermission()

        // Nút phát nhạc
        binding.btnPlay.setOnClickListener {
            if (musicList.isEmpty()) {
                Toast.makeText(this, "Không tìm thấy nhạc trong /Music/Music", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startMusicService()
        }

        binding.btnPause.setOnClickListener { sendAction("ACTION_PAUSE") }
        binding.btnNext.setOnClickListener { sendAction("ACTION_NEXT") }
        binding.btnPrev.setOnClickListener { sendAction("ACTION_PREVIOUS") }
    }

    // ───────────────────────────────────────────────
    //                XIN QUYỀN THÔNG BÁO
    // ───────────────────────────────────────────────
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION
                )
            }
        }
    }

    // ───────────────────────────────────────────────
    //           LẤY DANH SÁCH NHẠC
    // ───────────────────────────────────────────────
    private fun loadMusicFiles() {
        val selection = "${MediaStore.Audio.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%/Music/Music/%")

        val projection = arrayOf(
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        musicList.clear()
        musicTitles.clear()

        cursor?.use {
            val dataIndex = it.getColumnIndex(MediaStore.Audio.Media.DATA)
            val nameIndex = it.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)

            while (it.moveToNext()) {
                val path = if (dataIndex >= 0) it.getString(dataIndex) else null
                val title = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"

                if (path != null) {
                    musicList.add(path)
                    musicTitles.add(title)
                }
            }
        }

        if (musicList.isNotEmpty()) {
            binding.txtCurrentSong.text = "Tìm thấy ${musicList.size} bài"
        } else {
            binding.txtCurrentSong.text = "Không tìm thấy bài nào"
        }
    }

    // ───────────────────────────────────────────────
    //                  QUYỀN LƯU TRỮ
    // ───────────────────────────────────────────────
    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            // Android 13+
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                    REQUEST_CODE_STORAGE
                )
            } else loadMusicFiles()

        } else {

            // Android 12-
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_STORAGE
                )
            } else loadMusicFiles()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {

            REQUEST_CODE_STORAGE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    loadMusicFiles()
                } else {
                    Toast.makeText(this, "Không có quyền đọc nhạc", Toast.LENGTH_SHORT).show()
                }
            }

            REQUEST_CODE_NOTIFICATION -> {
                if (!(grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    Toast.makeText(this, "Không bật quyền thông báo → không thấy notification", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ───────────────────────────────────────────────
    //           GỬI ACTION CHO SERVICE
    // ───────────────────────────────────────────────
    private fun startMusicService() {
        val intent = Intent(this, MusicService::class.java).apply {
            action = "ACTION_START"
            putStringArrayListExtra("music_list", musicList)
            putStringArrayListExtra("music_titles", musicTitles)
        }
        startService(intent)
    }

    private fun sendAction(action: String) {
        val intent = Intent(this, MusicService::class.java).apply { this.action = action }
        startService(intent)
    }
}
