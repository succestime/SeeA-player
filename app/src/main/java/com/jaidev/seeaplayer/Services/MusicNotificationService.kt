package com.jaidev.seeaplayer.Services

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.jaidev.seeaplayer.MP3playerActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.MP3FileData

class MusicNotificationService : Service() {

    private lateinit var mediaSession: MediaSessionCompat
    private var mp3FileData: MP3FileData? = null
    private var isPlaying: Boolean = true  // Default to true to show pause icon initially

    override fun onCreate() {
        super.onCreate()

        // Initialize the media session
        mediaSession = MediaSessionCompat(this, "MusicNotificationService")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mp3FileData = intent?.getParcelableExtra("mp3FileData")

        // Check if the action is to toggle play/pause
        val action = intent?.action
        when (action) {
            "ACTION_PLAY_PAUSE" -> {
                isPlaying = !isPlaying
                updateNotification()
                sendBroadcast(Intent("TOGGLE_PLAY_PAUSE"))
            }
            "ACTION_NEXT" -> sendBroadcast(Intent("NEXT_TRACK"))
            "ACTION_PREVIOUS" -> sendBroadcast(Intent("PREVIOUS_TRACK"))
        }

        // Start foreground service with notification
        startForeground(1, createNotification())

        return START_NOT_STICKY
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotification(): Notification {
        val channelId = "music_channel"
        val channelName = "Music Playback"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager =
                getSystemService(NotificationManager::class.java) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val playPauseAction = NotificationCompat.Action(
            if (isPlaying) R.drawable.round_pause_circle_outline_24 else R.drawable.round_play_circle_outline_24,
            if (isPlaying) "Pause" else "Play",
            getPendingIntent("ACTION_PLAY_PAUSE")
        )

        val nextAction = NotificationCompat.Action(
            R.drawable.round_skip_next_24,
            "Next",
            getPendingIntent("ACTION_NEXT")
        )

        val prevAction = NotificationCompat.Action(
            R.drawable.round_mp3_skip_previous_24,
            "Previous",
            getPendingIntent("ACTION_PREVIOUS")
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.play_rectangle_icon)
            .setContentTitle(mp3FileData?.title ?: "Unknown Title")
            .setContentText("Music Player")
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MP3playerActivity::class.java).apply {
            this.action = action
        }
        return PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateNotification() {
        val notificationManager =
            getSystemService(NotificationManager::class.java) as NotificationManager
        notificationManager.notify(1, createNotification())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }
}
