package com.jaidev.seeaplayer.Services


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.exitApplication
import com.jaidev.seeaplayer.dataClass.favouriteChecker
import com.jaidev.seeaplayer.dataClass.setSongPosition
import com.jaidev.seeaplayer.musicActivity.NowPlaying
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity

class NotificationReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action) {
            ApplicationClass.PREVIOUS -> prevNextSong(increment = false, context = context!!)
            ApplicationClass.PLAY -> if(PlayerMusicActivity.isPlaying) pauseMusic() else playMusic()
            ApplicationClass.NEXT -> prevNextSong(increment = true, context = context!!)
            ApplicationClass.EXIT -> exitApplication()
            ApplicationClass.REPLAY -> replay(context = context!!)
            ApplicationClass.FORWARD -> forward(context = context!!)
        }
    }

    private fun replay(context: Context) {
        PlayerMusicActivity.musicService?.let { musicService ->
            val currentPosition = musicService.mediaPlayer?.currentPosition ?: return
            val newPosition = (currentPosition - 10000).coerceAtLeast(0) // Rewind 10 seconds
            musicService.mediaPlayer?.seekTo(newPosition)
            playMusic() // Resume playback after rewinding
        }
    }

    private fun forward(context: Context) {
        PlayerMusicActivity.musicService?.let { musicService ->
            val currentPosition = musicService.mediaPlayer?.currentPosition ?: return
            val duration = musicService.mediaPlayer?.duration ?: return
            val newPosition = (currentPosition + 10000).coerceAtMost(duration) // Forward 10 seconds
            musicService.mediaPlayer?.seekTo(newPosition)
            playMusic() // Resume playback after fast-forwarding
        }
    }
    private fun playMusic() {
        PlayerMusicActivity.musicService?.let { musicService ->
            PlayerMusicActivity.isPlaying = true
            musicService.mediaPlayer?.start()
            musicService.showNotification(R.drawable.round_pause_24)
            PlayerMusicActivity.binding.playPauseBtnPA.setIconResource(R.drawable.round_pause_24)

            try {
                NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.round_pause_24)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun pauseMusic() {
        PlayerMusicActivity.musicService?.let { musicService ->
            PlayerMusicActivity.isPlaying = false
            musicService.mediaPlayer?.pause()
            musicService.showNotification(R.drawable.round_play)
            PlayerMusicActivity.binding.playPauseBtnPA.setIconResource(R.drawable.round_play)

            try {
                NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.round_play)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun prevNextSong(increment: Boolean, context: Context) {
        PlayerMusicActivity.musicService?.let { musicService ->
            setSongPosition(increment = increment)
            musicService.createMediaPlayer()

            PlayerMusicActivity.binding.songImgPA.let {
                Glide.with(context)
                    .load(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].artUri)
                    .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
                    .into(it)
            }

            PlayerMusicActivity.binding.songNamePA.text = PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].title

            NowPlaying.binding.songImgNP.let {
                Glide.with(context)
                    .load(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].artUri)
                    .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
                    .into(it)
            }

            NowPlaying.binding.songNameNP.text = PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].title

            playMusic()

            PlayerMusicActivity.fIndex = favouriteChecker(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].id)

            PlayerMusicActivity.binding.favouriteBtnPA.setImageResource(
                if (PlayerMusicActivity.isFavourite) R.drawable.round_favorite_music
                else R.drawable.round_favorite_border_music
            )
        }
    }
}