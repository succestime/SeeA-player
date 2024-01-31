package com.jaidev.seeaplayer.musicActivity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jaidev.seeaplayer.ApplicationClass
import com.jaidev.seeaplayer.NowPlaying
import com.jaidev.seeaplayer.PlayerMusicActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.exitApplication
import com.jaidev.seeaplayer.dataClass.favouriteChecker
import com.jaidev.seeaplayer.dataClass.setSongPosition

class NotificationReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action){
            //only play next or prev song, when music list contains more than one song
            ApplicationClass.PREVIOUS -> prevNextSong(increment = false, context = context!!)
            ApplicationClass.PLAY -> if(PlayerMusicActivity.isPlaying) pauseMusic() else playMusic()
            ApplicationClass.NEXT -> prevNextSong(increment = true, context = context!!)
            ApplicationClass.EXIT -> {
           exitApplication()
            }
        }
    }
    private fun playMusic(){
        PlayerMusicActivity.isPlaying = true
        PlayerMusicActivity.musicService!!.mediaPlayer!!.start()
        PlayerMusicActivity.musicService!!.showNotification(R.drawable.ic_pause_icon)
        PlayerMusicActivity.binding.playPauseBtnPA.setIconResource(R.drawable.ic_pause_icon)
        //for handling app crash during notification play - pause btn (While app opened through intent)
        try{ NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.ic_pause_icon) }catch (_: Exception){}
    }

    private fun pauseMusic(){
        PlayerMusicActivity.isPlaying = false
        PlayerMusicActivity.musicService!!.mediaPlayer!!.pause()
        PlayerMusicActivity.musicService!!.showNotification(R.drawable.play_music_icon)
        PlayerMusicActivity.binding.playPauseBtnPA.setIconResource(R.drawable.play_music_icon)
        //for handling app crash during notification play - pause btn (While app opened through intent)
        try{ NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.play_music_icon) }catch (_: Exception){}
    }
    @SuppressLint("SuspiciousIndentation")
    private fun prevNextSong(increment: Boolean, context: Context){
        setSongPosition(increment = increment)
       PlayerMusicActivity.musicService!!.createMediaPlayer()
        Glide.with(context)
            .load(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.speaker).centerCrop())
            .into(PlayerMusicActivity.binding.songImgPA)

        PlayerMusicActivity.binding.songNamePA.text = PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].title
        Glide.with(context)
            .load(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.speaker).centerCrop())
            .into(NowPlaying.binding.songImgNP)

        NowPlaying.binding.songNameNP.text = PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].title
        playMusic()

        PlayerMusicActivity.fIndex = favouriteChecker(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].id)

        if(PlayerMusicActivity.isFavourite) PlayerMusicActivity.binding.favouriteBtnPA.setImageResource(
            R.drawable.favorite_icon
        )
        else PlayerMusicActivity.binding.favouriteBtnPA.setImageResource(R.drawable.favorite_empty_icon)
    }
}