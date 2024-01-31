package com.jaidev.seeaplayer.dataClass

import android.media.MediaMetadataRetriever
import android.net.Uri
import com.jaidev.seeaplayer.recantFragment.ReMusicPlayerActivity
import java.util.concurrent.TimeUnit

data class RecantMusic(val title: String,
                       val artist: String,
                       val album: String,
                       val timestamp: Long,
                       val id: String,
                       val duration: Long = 0,
                       val path: String,
                       val albumArtUri: Uri ,
                       val size: String) {

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
            if (ReMusicPlayerActivity.reMusicList.size - 1 == ReMusicPlayerActivity.songPosition)
                ReMusicPlayerActivity.songPosition = 0
            else ++ReMusicPlayerActivity.songPosition
        } else {
            if (0 == ReMusicPlayerActivity.songPosition)
                ReMusicPlayerActivity.songPosition = ReMusicPlayerActivity.reMusicList.size - 1
            else --ReMusicPlayerActivity.songPosition
        }

    }
}
fun reGetImgArt(path: String): ByteArray? {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    return retriever.embeddedPicture
}
