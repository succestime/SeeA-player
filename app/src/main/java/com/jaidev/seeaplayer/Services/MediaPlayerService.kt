package com.jaidev.seeaplayer.Services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.jaidev.seeaplayer.MP3playerActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.MP3FileData

class MediaPlayerService : Service() {

    companion object {
        const val ACTION_PLAY = "action_play"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_NEXT = "action_next"
        const val ACTION_PREVIOUS = "action_previous"

    }

    var isPlaying: Boolean = false
    private var mp3FileTitle: String? = null
    private val channelId = "media_playback_channel"
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaPlayer: MediaPlayer
    private val binder = MediaPlayerBinder()


    var onTrackCompleteListener: (() -> Unit)? = null
    private var currentIndex: Int = 0
    private var mp3Files: ArrayList<MP3FileData> = arrayListOf()
    private lateinit var mediaButtonReceiver: MediaButtonReceiver

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class MediaPlayerBinder : Binder() {
        fun getService(): MediaPlayerService = this@MediaPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "MediaPlayerService")
        createNotificationChannel()

        mediaButtonReceiver = MediaButtonReceiver()
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(this, MediaButtonReceiver::class.java)
        val mediaButtonPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, PendingIntent.FLAG_IMMUTABLE)
        mediaSession.setMediaButtonReceiver(mediaButtonPendingIntent)

        setupMediaSessionCallbacks()
    }

    private fun setupMediaSessionCallbacks() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaSession.setMetadata(
                MediaMetadataCompat.Builder().putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION, getDuration().toLong()
                ).build()
            )

            updatePlaybackStateWithPosition()
            mediaSession.setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    super.onPlay()
                    handlePlayPause()
                }

                override fun onPause() {
                    super.onPause()

                    handlePlayPause()
                }

                override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                    handlePlayPause()
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }

                override fun onSkipToNext() {
                    super.onSkipToNext()
                    moveToNextTrack()
                }

                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    moveToPreviousTrack()
                }

                override fun onStop() {
                    super.onStop()
                    stopSelf()
                }

                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    mediaPlayer.seekTo(pos.toInt())
                    updatePlaybackStateWithPosition()
                }
            })
        }
    }

    // Setter for mp3Files and currentIndex
    fun setMp3Files(files: ArrayList<MP3FileData>, index: Int) {
        mp3Files = files
        currentIndex = index
    }

    // In MediaPlayerService
    fun getCurrentTrackTitle(): String? {
        return mp3FileTitle
    }
    private fun handlePlayPause() {
        if (isPlaying) {
            pauseAudio()
        } else {
            resumeAudio()
        }
        updateNotification()
        broadcastPlayPauseState(isPlaying) // Broadcast the state change
        broadcastTrackTitle() // Broadcast the track title as well
    }

    fun playMP3(filePath: String, title: String) {
        mp3FileTitle = title

        if (::mediaPlayer.isInitialized) {
            mediaPlayer.reset()
        } else {
            mediaPlayer = MediaPlayer()
        }

        try {
            mediaPlayer.setDataSource(filePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
            isPlaying = true

            updatePlaybackStateWithPosition()
            updateNotification()
            broadcastTrackTitle()
            broadcastPlayPauseState(true) // Broadcast the play state

            mediaPlayer.setOnCompletionListener {
                isPlaying = false
                onTrackCompleteListener?.invoke()
                startForeground(1, getNotification())
                broadcastTrackTitle()
                updatePlaybackStateWithPosition()
                updateNotification()
                broadcastPlayPauseState(false) // Broadcast the pause state after completion
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseAudio() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
            updatePlaybackStateWithPosition()
            updateNotification()
            broadcastTrackTitle()
            broadcastPlayPauseState(false) // Broadcast the pause state
        }
    }

    fun resumeAudio() {
        if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
            mediaPlayer.start()
            isPlaying = true
            updatePlaybackStateWithPosition()
            updateNotification()
            broadcastTrackTitle()
            broadcastPlayPauseState(true) // Broadcast the play state
        }
    }


    fun getCurrentPosition(): Int {
        return if (::mediaPlayer.isInitialized) mediaPlayer.currentPosition else 0
    }

    fun getDuration(): Int {
        return if (::mediaPlayer.isInitialized) mediaPlayer.duration else 0
    }

    fun seekTo(position: Int) {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.seekTo(position)
            updatePlaybackStateWithPosition()
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Media Playback",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Media playback controls"
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getNotification(): Notification {
        val intent = Intent(this, MP3playerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val prevAction = NotificationCompat.Action(
            R.drawable.round_mp3_skip_previous_24, "Previous",
            PendingIntent.getService(
                this, 2,
                Intent(this, MediaPlayerService::class.java).apply { action = ACTION_PREVIOUS },
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        val nextAction = NotificationCompat.Action(
            R.drawable.round_skip_next_24, "Next",
            PendingIntent.getService(
                this, 3,
                Intent(this, MediaPlayerService::class.java).apply { action = ACTION_NEXT },
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.round_pause_circle_outline_24, "Pause",
                PendingIntent.getService(
                    this, 1,
                    Intent(this, MediaPlayerService::class.java).apply { action = ACTION_PAUSE },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            NotificationCompat.Action(
                R.drawable.round_play_circle_outline_24, "Play",
                PendingIntent.getService(
                    this, 1,
                    Intent(this, MediaPlayerService::class.java).apply { action = ACTION_PLAY },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        val largeIcon: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.music_speaker_three)

        return NotificationCompat.Builder(this, channelId).apply {
            setContentTitle(mp3FileTitle ?: "Playing music")
            setSmallIcon(R.mipmap.ic_logo_o)
            setContentIntent(pendingIntent)
            if (largeIcon != null) {
                setLargeIcon(largeIcon)
            }
            addAction(prevAction)
            addAction(playPauseAction)
            addAction(nextAction)
            setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
            )
            priority = NotificationCompat.PRIORITY_DEFAULT
        }.build()
    }


    private fun updatePlaybackStateWithPosition() {
        val playbackSpeed = if (isPlaying) 1F else 0F
        val playbackState = PlaybackStateCompat.Builder()
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                getCurrentPosition().toLong(),
                playbackSpeed // Playback speed
            )
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setBufferedPosition(getDuration().toLong())
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    fun updateNotification() {
        startForeground(1, getNotification())
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                resumeAudio()
                broadcastPlayPauseState(true)
            }
            ACTION_PAUSE -> {
                pauseAudio()
                broadcastPlayPauseState(false)
            }
            ACTION_NEXT -> moveToNextTrack()
            ACTION_PREVIOUS -> moveToPreviousTrack()
        }

        updateNotification()
        return START_NOT_STICKY
    }

    private fun broadcastPlayPauseState(isPlaying: Boolean) {
        val intent = Intent("PLAY_PAUSE_ACTION")
        intent.putExtra("isPlaying", isPlaying)
        sendBroadcast(intent)
    }
    private fun broadcastTrackTitle() {
        val intent = Intent("TRACK_TITLE")
        intent.putExtra("trackTitle", mp3FileTitle)
        sendBroadcast(intent)
    }

    fun getCurrentTrackIndex(): Int {
        return currentIndex
    }

    fun moveToNextTrack() {
        if (currentIndex < mp3Files.size - 1) {
            currentIndex++

        }else{
            currentIndex = 0
        }
        playMP3(mp3Files[currentIndex].path, mp3Files[currentIndex].title)
        broadcastPlayPauseState(true) // Ensure the correct state is broadcasted after moving to the next track

    }

    fun moveToPreviousTrack() {
        if (currentIndex > 0) {
            currentIndex--

        }else{
            currentIndex = mp3Files.size -1
        }
        playMP3(mp3Files[currentIndex].path, mp3Files[currentIndex].title)
    }



    override fun onDestroy() {
        mediaPlayer.stop()
        mediaPlayer.release()
        mediaSession.release()
        super.onDestroy()
    }
}
