package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.jaidev.seeaplayer.dataClass.formatDuration
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.musicActivity.NotificationReceiver
import com.jaidev.seeaplayer.recantFragment.ReMusicPlayerActivity


class MusicService:Service(), AudioManager.OnAudioFocusChangeListener {
    private var myBinder = MyBinder()
    var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession : MediaSessionCompat
    private lateinit var runnable: Runnable

    override fun onBind(intent: Intent?): IBinder {
        mediaSession = MediaSessionCompat(baseContext , "My Music")
        myBinder = MyBinder()
        return myBinder
    }

    inner class MyBinder : Binder() {
        fun currentService(): MusicService {
            return this@MusicService
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag", "ForegroundServiceType")
    fun showNotification(playPauseBtn : Int) {
        val intent = Intent(baseContext, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val prevIntent = Intent(
            baseContext,
            NotificationReceiver::class.java
        ).setAction(ApplicationClass.PREVIOUS)
        val prevPendingIntent = PendingIntent.getBroadcast(
            baseContext,
            0,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playIntent =
            Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.PLAY)
        val playPendingIntent = PendingIntent.getBroadcast(
            baseContext,
            0,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent =
            Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.NEXT)
        val nextPendingIntent = PendingIntent.getBroadcast(
            baseContext,
            0,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val exitIntent =
            Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.EXIT)
        val exitPendingIntent = PendingIntent.getBroadcast(
            baseContext,
            0,
            exitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val imgArt =
            getImgArt(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].path)
        val image = if (imgArt != null) {
            BitmapFactory.decodeByteArray(imgArt, 0, imgArt.size)
        } else {
            BitmapFactory.decodeResource(resources, R.drawable.speaker)
        }

        val notification =
            androidx.core.app.NotificationCompat.Builder(baseContext, ApplicationClass.CHANNEL_ID)
                .setContentIntent(contentIntent)
                .setContentTitle(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].title)
                .setContentText(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].artist)
                .setSmallIcon(R.drawable.music_icon)
                .setLargeIcon(image)
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                )
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .addAction(R.drawable.ic_previous_icon, "Previous", prevPendingIntent)
                .addAction(playPauseBtn, "Play", playPendingIntent)
                .addAction(R.drawable.ic_next_icon, "Next", nextPendingIntent)
                .addAction(R.drawable.exit_icon, "Exit", exitPendingIntent)
                .build()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            val playbackSpeed = if(PlayerMusicActivity.isPlaying) 1F else 0F
            mediaSession.setMetadata(
                MediaMetadataCompat.Builder()
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer!!.duration.toLong())
                .build())
            val playBackState = PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer!!.currentPosition.toLong(), playbackSpeed)
                .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                .build()
            mediaSession.setPlaybackState(playBackState)
            mediaSession.setCallback(object: MediaSessionCompat.Callback(){

                //called when headphones buttons are pressed
                //currently only pause or play music on button click
                override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                    if(PlayerMusicActivity.isPlaying){
                        //pause music
                        PlayerMusicActivity.binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
                        NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.play_icon)
                        PlayerMusicActivity.isPlaying = false
                        mediaPlayer!!.pause()
                        showNotification(R.drawable.play_icon)
                    }else{
                        //play music
                        PlayerMusicActivity.binding.playPauseBtnPA.setIconResource(R.drawable.ic_pause_icon)
                        NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.ic_pause_icon)
                        PlayerMusicActivity.isPlaying = true
                        mediaPlayer!!.start()
                        showNotification(R.drawable.ic_pause_icon)
                    }
                    // Update notification
                    showNotification(if (PlayerMusicActivity.isPlaying) R.drawable.ic_pause_icon else R.drawable.play_icon)
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }
                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    mediaPlayer!!.seekTo(pos.toInt())
                    val playBackStateNew = PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer!!.currentPosition.toLong(), playbackSpeed)
                        .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                        .build()
                    mediaSession.setPlaybackState(playBackStateNew)
                    showNotification(if (PlayerMusicActivity.isPlaying) R.drawable.ic_pause_icon else R.drawable.play_icon)
                }
            })
        }
        startForeground(13, notification)
    }


        fun createMediaPlayer() {
            try {
                if (PlayerMusicActivity.musicService!!.mediaPlayer == null) PlayerMusicActivity.musicService!!.mediaPlayer =
                    MediaPlayer()
                PlayerMusicActivity.musicService!!.mediaPlayer!!.reset()
                PlayerMusicActivity.musicService!!.mediaPlayer!!.setDataSource(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].path)
                PlayerMusicActivity.musicService!!.mediaPlayer!!.prepare()
                PlayerMusicActivity.binding.playPauseBtnPA.setIconResource(R.drawable.ic_pause_icon)
                PlayerMusicActivity.musicService!!.showNotification(R.drawable.ic_pause_icon)
                PlayerMusicActivity.binding.tvSeekBarStart.text =
                    formatDuration(mediaPlayer!!.currentPosition.toLong())
                PlayerMusicActivity.binding.tvSeekBarEnd.text =
                    formatDuration(mediaPlayer!!.duration.toLong())
                PlayerMusicActivity.binding.seekBarPA.progress = 0
                PlayerMusicActivity.binding.seekBarPA.max = mediaPlayer!!.duration
                PlayerMusicActivity.nowMusicPlayingId = PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].id
            } catch (e: Exception) {
                return
            }

        }

    fun seekBarSetup(){
        runnable = Runnable {
            PlayerMusicActivity.binding.tvSeekBarStart.text = formatDuration(mediaPlayer!!.currentPosition.toLong())
            PlayerMusicActivity.binding.seekBarPA.progress = mediaPlayer!!.currentPosition
            Handler(Looper.getMainLooper()).postDelayed(runnable, 200)

        }
        Handler(Looper.getMainLooper()).postDelayed(runnable, 0)
    }

    fun reSeekSetup(){
        runnable = Runnable {
            ReMusicPlayerActivity.binding.tvSeekBarStart1.text = formatDuration(mediaPlayer!!.currentPosition.toLong())
            ReMusicPlayerActivity.binding.seekBarRPA.progress = mediaPlayer!!.currentPosition
            Handler(Looper.getMainLooper()).postDelayed(runnable, 200)

        }
        Handler(Looper.getMainLooper()).postDelayed(runnable, 0)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        TODO("Not yet implemented")
    }


}
