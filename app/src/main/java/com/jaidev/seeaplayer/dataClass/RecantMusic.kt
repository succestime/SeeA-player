package com.jaidev.seeaplayer.dataClass

import android.net.Uri
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.songPosition
import com.jaidev.seeaplayer.recantFragment.ReMusicPlayerActivity
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

data class RecantMusic(val title: String,
                       val artist: String,
                       val album: String,
                       val timestamp: Long,
                       val id: String,
                       val duration: Long = 0,
                       val path: String,
                       val albumArtUri: Uri ,

                       val size: String,
                       ) {


}
fun reFormatDuration(duration: Long):String{
    val minutes = TimeUnit.MINUTES.convert(duration , TimeUnit.MILLISECONDS)
    val seconds = (TimeUnit.SECONDS.convert(duration , TimeUnit.MILLISECONDS) -
            minutes* TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES))
    return String.format("%02d:%02d" , minutes , seconds)

}

fun reSetSongPosition(increment: Boolean) {

    if(!ReMusicPlayerActivity.repeat){
        if (increment) {
            if (ReMusicPlayerActivity.reMusicListPA.size - 1 == songPosition)
               songPosition = 0
            else ++songPosition
        } else {
            if (0 == ReMusicPlayerActivity.songPosition)
               songPosition = ReMusicPlayerActivity.reMusicListPA.size - 1
            else --songPosition
        }

    }
}
fun exitReApplication() {
    if (ReMusicPlayerActivity.musicService != null) {
        //  PlayerMusicActivity.musicService!!.audioManager.abandonAudioFocus(PlayerMusicActivity.musicService)
        ReMusicPlayerActivity.musicService!!.stopForeground(true)
        ReMusicPlayerActivity.musicService!!.mediaPlayer!!.release()
        ReMusicPlayerActivity.musicService = null
    }
    exitProcess(1)
}
