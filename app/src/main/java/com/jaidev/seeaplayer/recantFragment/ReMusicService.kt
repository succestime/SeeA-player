package com.jaidev.seeaplayer.recantFragment

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat

class ReMusicService:Service()  {
    //  var reMediaPlayer: MediaPlayer? = null
    private var myBinder = MyBinder()
    var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession : MediaSessionCompat
    //private lateinit var runnable: Runnable
    override fun onBind(intent: Intent?): IBinder {
        mediaSession = MediaSessionCompat(baseContext , "My Music")
        return myBinder
    }

    inner class MyBinder : Binder() {
        fun reCurrentService(): ReMusicService {
            return this@ReMusicService
        }
    }


}