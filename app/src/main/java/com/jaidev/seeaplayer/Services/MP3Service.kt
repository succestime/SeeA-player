package com.jaidev.seeaplayer.Services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jaidev.seeaplayer.MP3ConverterFunctionality.MP3NotificationReceiver
import com.jaidev.seeaplayer.MP3ConverterFunctionality.MP3NowPlaying
import com.jaidev.seeaplayer.MP3ConverterFunctionality.MP3playerActivity
import com.jaidev.seeaplayer.MP3ConverterFunctionality.MP3playerActivity.Companion.mp3Adapter
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.MusicNav
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.MP3Data.exitMP3Application
import com.jaidev.seeaplayer.dataClass.MP3Data.setMP3SongPosition
import com.jaidev.seeaplayer.dataClass.formatDuration
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.isPlaying
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.loudnessEnhancer
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.nowMusicPlayingId
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.songPosition

class MP3Service: Service() , AudioManager.OnAudioFocusChangeListener{
    private var myBinder = MyBinder()
    var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var runnable: Runnable
    private val handler = Handler(Looper.getMainLooper()) // Handler instance
    var isReSeekSetupRunning = false // Flag to indicate whether reSeekSetup() is running
    var isNotificationSeeking = false // Flag to indicate if notification seek is in progress
    private var inactivityHandler: Handler? = null
    private var inactivityRunnable: Runnable? = null
    private val inactivityTimeout = 10 * 60 * 1000L // 10 minutes in milliseconds

    override fun onBind(intent: Intent?): IBinder {
        mediaSession = MediaSessionCompat(baseContext, "MP3 Audio")

        return myBinder
    }

    inner class MyBinder : Binder() {
        fun currentService(): MP3Service {
            return this@MP3Service
        }
    }


    fun stopService() {
        stopForeground(true)
        stopSelf()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        handler.removeCallbacks(runnable)
        MP3playerActivity.musicMP3Service = null // Ensure service reference is cleared
        val intent = Intent(MusicNav.ACTION_HIDE_MP3_NOW_PLAYING)
        sendBroadcast(intent)
    }
    fun showNotification(playPauseBtn: Int) {
        createNotificationChannel()
        val intent = Intent(baseContext, MainActivity::class.java)
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val contentIntent = PendingIntent.getActivity(this, 0,
            intent, flag)

        val prevIntent = Intent(baseContext, MP3NotificationReceiver::class.java).setAction(
            MP3ApplicationClass.PREVIOUS
        )
        val prevPendingIntent = PendingIntent.getBroadcast(baseContext, 0, prevIntent, flag)

        val replayIntent = Intent(baseContext, MP3NotificationReceiver::class.java).setAction(
            MP3ApplicationClass.REPLAY
        )
        val replayPendingIntent = PendingIntent.getBroadcast(baseContext, 0, replayIntent,flag)

        val playIntent = Intent(baseContext, MP3NotificationReceiver::class.java).setAction(
            MP3ApplicationClass.PLAY
        )
        val playPendingIntent = PendingIntent.getBroadcast(baseContext, 0, playIntent, flag)

        val forwardIntent = Intent(baseContext, MP3NotificationReceiver::class.java).setAction(
            MP3ApplicationClass.FORWARD
        )
        val forwardPendingIntent = PendingIntent.getBroadcast(baseContext, 0, forwardIntent, flag)

        val nextIntent = Intent(baseContext, MP3NotificationReceiver::class.java).setAction(
            MP3ApplicationClass.NEXT
        )
        val nextPendingIntent = PendingIntent.getBroadcast(baseContext, 0, nextIntent, flag)

        val imgArt = getImgArt(MP3playerActivity.mp3MusicPA[songPosition].path)
        val image = if (imgArt != null) {
            BitmapFactory.decodeByteArray(imgArt, 0, imgArt.size)
        } else {
            BitmapFactory.decodeResource(resources, R.drawable.music_speaker_three)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(baseContext, MP3ApplicationClass.CHANNEL_ID)
        .setContentIntent(contentIntent)
            .setContentTitle(MP3playerActivity.mp3MusicPA[songPosition].title)
            .setSmallIcon(R.drawable.music_icon)
            .setLargeIcon(image)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.round_previous, "Previous", prevPendingIntent)
            .addAction(R.drawable.round_replay_10, "Replay", replayPendingIntent)
            .addAction(playPauseBtn, "Play", playPendingIntent)
            .addAction(R.drawable.round_forward_10, "Forward", forwardPendingIntent)
            .addAction(R.drawable.round_next, "Next", nextPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaSession.setMetadata(
                MediaMetadataCompat.Builder().putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer!!.duration.toLong()
            ).build())

            mediaSession.setPlaybackState(getPlayBackState())
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
                    prevNextSong(increment = true, context = this@MP3Service)
                }

                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    prevNextSong(increment = false, context = this@MP3Service)
                }

                override fun onStop() {
                    super.onStop()
                    exitMP3Application()
                }

                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    mediaPlayer?.seekTo(pos.toInt())
                    isNotificationSeeking = true
                    if (!isReSeekSetupRunning) {
                        mediaSession.setPlaybackState(getPlayBackState())
                    }
                }
            })
        }
        startForeground(1, notification)
    }


    fun createMediaPlayer() {
        try {
            if (mediaPlayer == null) mediaPlayer = MediaPlayer()
         mediaPlayer?.reset()
          mediaPlayer?.setDataSource(MP3playerActivity.mp3MusicPA[songPosition].path)
       mediaPlayer?.prepare()
            MP3playerActivity.binding.playPauseBtn.setImageResource(R.drawable.round_pause_circle_outline_24)
             showNotification(R.drawable.round_pause_circle_outline_24)
            MP3playerActivity.binding.remainingTimeLabelStart.text = formatDuration(mediaPlayer!!.currentPosition.toLong())
            MP3playerActivity.binding.elapsedTimeLabelEnd.text = formatDuration(mediaPlayer!!.duration.toLong())
            MP3playerActivity.binding.seekBarMPA.progress = 0
            MP3playerActivity.binding.seekBarMPA.max = mediaPlayer!!.duration
         nowMusicPlayingId = MP3playerActivity.mp3MusicPA[songPosition].id
          loudnessEnhancer = LoudnessEnhancer(mediaPlayer!!.audioSessionId)
           loudnessEnhancer.enabled = true
        } catch (e: Exception) {
            return
        }
    }

    fun prevNextSong(increment: Boolean, context: Context) {
        setMP3SongPosition(increment = increment)
        createMediaPlayer()
        Glide.with(context)
            .load(MP3playerActivity.mp3MusicPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
            .into(MP3playerActivity.binding.playerActivityMP3Image)

        MP3playerActivity.binding.SongTitle.text = MP3playerActivity.mp3MusicPA[songPosition].title
        Glide.with(context)
            .load(MP3playerActivity.mp3MusicPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
            .into(MP3NowPlaying.binding.songImgNP)

      mp3Adapter.updateCurrentSongPosition(songPosition)

        MP3NowPlaying.binding.songNameNP.text = MP3playerActivity.mp3MusicPA[songPosition].title
        playMusic()

    }

    private fun startInactivityTimer() {
        inactivityHandler = Handler(Looper.getMainLooper())
        inactivityRunnable = Runnable {
            if (!isPlaying) {
                stopService()  // Stop the service after 10 minutes of inactivity
            }
        }
        inactivityHandler?.postDelayed(inactivityRunnable!!, inactivityTimeout)
    }

    private fun stopInactivityTimer() {
        inactivityHandler?.removeCallbacks(inactivityRunnable!!)
    }


    fun getPlayBackState(): PlaybackStateCompat {
        val playbackSpeed = if (isPlaying) 1F else 0F

        return PlaybackStateCompat.Builder()
            .setState(
                if (mediaPlayer?.isPlaying == true) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                mediaPlayer!!.currentPosition.toLong(), playbackSpeed
            )
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .build()
    }
    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = MP3ApplicationClass.CHANNEL_ID
            val channelName = "MP3 Service Channel"
            val channelDescription = "Channel for MP3 Service notifications"
            val importance = NotificationManager.IMPORTANCE_LOW

            val notificationChannel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    fun handlePlayPause() {
        if (isPlaying) {
            MP3playerActivity.binding.playPauseBtn.setImageResource(R.drawable.round_play_circle_outline_24)
            MP3NowPlaying.binding.playPauseBtnNP.setImageResource(R.drawable.round_play_circle_outline_24)
            isPlaying = false
            mediaPlayer?.pause()
            showNotification(R.drawable.round_play_circle_outline_24)
            startInactivityTimer() // Start inactivity timer when paused

        } else {
            MP3playerActivity.binding.playPauseBtn.setImageResource(R.drawable.round_pause_circle_outline_24)
            MP3NowPlaying.binding.playPauseBtnNP.setImageResource(R.drawable.round_pause_circle_outline_24)
            isPlaying = true
            mediaPlayer?.start()
            showNotification(R.drawable.round_pause_circle_outline_24)
        }
        mediaSession.setPlaybackState(getPlayBackState())
    }

    override fun onDestroy() {
        super.onDestroy()
        stopInactivityTimer()  // Clean up the handler and runnable when service is destroyed

    }


    private fun playMusic() {
        isPlaying = true
        mediaPlayer!!.start()
        showNotification(R.drawable.round_pause_circle_outline_24)
        MP3playerActivity.binding.playPauseBtn.setImageResource(R.drawable.round_pause_circle_outline_24)
        try {
            MP3NowPlaying.binding.playPauseBtnNP.setImageResource(R.drawable.round_pause_circle_outline_24)
        } catch (_: Exception) { }
    }


    fun seekBarSetup() {
        try {
            runnable = Runnable {
                MP3playerActivity.binding.remainingTimeLabelStart.text = formatDuration(mediaPlayer!!.currentPosition.toLong())
                MP3playerActivity.binding.seekBarMPA.progress = mediaPlayer!!.currentPosition
                handler.postDelayed(runnable, 100)
            }
            handler.postDelayed(runnable, 0)
        } catch (_: Exception) { }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange <= 0) {
            MP3playerActivity.binding.playPauseBtn.setImageResource(R.drawable.round_play_circle_outline_24)
            MP3NowPlaying.binding.playPauseBtnNP.setImageResource(R.drawable.round_play_circle_outline_24)
            isPlaying = false
            mediaPlayer!!.pause()
            showNotification(R.drawable.round_play_circle_outline_24)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        PlayerMusicActivity.musicService?.stopService()

        return START_STICKY
    }



}