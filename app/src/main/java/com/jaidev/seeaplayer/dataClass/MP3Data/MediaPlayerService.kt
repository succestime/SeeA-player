package com.jaidev.seeaplayer.dataClass.MP3Data

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder

class MediaPlayerService : Service() {

    private lateinit var mediaPlayer: MediaPlayer

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mp3FilePath = intent?.getStringExtra("mp3FilePath")

        if (mp3FilePath != null) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(mp3FilePath)
                prepare()
                start()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
