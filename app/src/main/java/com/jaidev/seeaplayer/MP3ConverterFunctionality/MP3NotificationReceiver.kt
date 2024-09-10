
package com.jaidev.seeaplayer.MP3ConverterFunctionality

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.Services.ApplicationClass
import com.jaidev.seeaplayer.Services.MP3ApplicationClass
import com.jaidev.seeaplayer.dataClass.MP3Data.exitMP3Application
import com.jaidev.seeaplayer.dataClass.MP3Data.setMP3SongPosition
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.isPlaying
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.songPosition

class MP3NotificationReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action){
            //only play next or prev song, when music list contains more than one song
            MP3ApplicationClass.PREVIOUS -> {
                prevNextSong(increment = false, context = context!!)
            }
            MP3ApplicationClass.PLAY -> if(isPlaying) pauseMusic() else playMusic()
            MP3ApplicationClass.NEXT -> {
                prevNextSong(increment = true, context = context!!)
            }
            MP3ApplicationClass.EXIT -> {
                exitMP3Application()
            }
            MP3ApplicationClass.REPLAY ->   replay()
            ApplicationClass.FORWARD ->  forward()
        }
    }

    private fun replay() {
        MP3playerActivity.musicMP3Service?.let { musicService ->
            val currentPosition = musicService.mediaPlayer?.currentPosition ?: return
            val newPosition = currentPosition - 10000 // Rewind 10 seconds (10000 milliseconds)

            // Ensure new position doesn't go below 0
            val seekPosition = if (newPosition < 0) 0 else newPosition

            musicService.mediaPlayer?.seekTo(seekPosition)
            playMusic() // Resume playback after rewinding
        }
    }


    private fun forward() {
        MP3playerActivity.musicMP3Service?.let { musicService ->
            val currentPosition = musicService.mediaPlayer?.currentPosition ?: return
            val newPosition = currentPosition + 10000 // Forward 10 seconds (10000 milliseconds)

            // Get the total duration of the media
            val duration = musicService.mediaPlayer?.duration ?: return

            // Ensure new position doesn't exceed the total duration
            val seekPosition = if (newPosition > duration) duration else newPosition

            musicService.mediaPlayer?.seekTo(seekPosition)
            playMusic() // Resume playback after fast-forwarding
        }
    }
    private fun playMusic(){
       isPlaying = true
        MP3playerActivity.musicMP3Service!!.mediaPlayer!!.start()
        MP3playerActivity.musicMP3Service!!.showNotification(R.drawable.round_pause_circle_outline_24)
        MP3playerActivity.binding.playPauseBtn.setImageResource(R.drawable.round_pause_circle_outline_24)
//        //for handling app crash during notification play - pause btn (While app opened through intent)
    try{ MP3NowPlaying.binding.playPauseBtnNP.setImageResource(R.drawable.round_pause_circle_outline_24) }catch (_: Exception){}
    }

    private fun pauseMusic(){
        isPlaying = false
        MP3playerActivity.musicMP3Service!!.mediaPlayer!!.pause()
        MP3playerActivity.musicMP3Service!!.showNotification(R.drawable.round_play_circle_outline_24)
        MP3playerActivity.binding.playPauseBtn.setImageResource(R.drawable.round_play_circle_outline_24)
//        //for handling app crash during notification play - pause btn (While app opened through intent)
   try{ MP3NowPlaying.binding.playPauseBtnNP.setImageResource(R.drawable.round_play_circle_outline_24) }catch (_: Exception){}
    }
    @SuppressLint("SuspiciousIndentation")
    private fun prevNextSong(increment: Boolean, context: Context){
        setMP3SongPosition(increment = increment)
        MP3playerActivity.musicMP3Service!!.createMediaPlayer()
        Glide.with(context)
            .load(MP3playerActivity.mp3MusicPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
            .into(MP3playerActivity.binding.playerActivityMP3Image)
        MP3playerActivity.binding.SongTitle.text = MP3playerActivity.mp3MusicPA[songPosition].title
        Glide.with(context)
            .load(MP3playerActivity.mp3MusicPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
            .into(MP3NowPlaying.binding.songImgNP)
        playMusic()
     MP3NowPlaying.binding.songNameNP.text = MP3playerActivity.mp3MusicPA[songPosition].title


    }
}
