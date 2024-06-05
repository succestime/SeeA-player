
package com.jaidev.seeaplayer.dataClass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.google.android.material.color.MaterialColors
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.musicActivity.FavouriteActivity
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


data class Music(val id : String, var title : String, val album : String,val artist : String, val duration : Long = 0 , var path : String,val size : String
                 , var artUri : Uri, val albumId : String
) {


}


class Playlist{
    lateinit var name: String
    lateinit var playlist: ArrayList<Music>
    lateinit var createdBy: String
    lateinit var createdOn: String
}
class MusicPlaylist{
    var ref: ArrayList<Playlist> = ArrayList()
}

fun formatDuration(duration: Long):String{
    val minutes = TimeUnit.MINUTES.convert(duration , TimeUnit.MILLISECONDS)
    val seconds = (TimeUnit.SECONDS.convert(duration , TimeUnit.MILLISECONDS) -
            minutes*TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES))
    return String.format("%02d:%02d" , minutes , seconds)

}

fun getImgArt(path: String): ByteArray? {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    return retriever.embeddedPicture
}

fun setSongPosition(increment: Boolean) {

    if(!PlayerMusicActivity.repeat){
        if (increment) {
            if (PlayerMusicActivity.musicListPA.size - 1 == PlayerMusicActivity.songPosition)
                PlayerMusicActivity.songPosition = 0
            else ++PlayerMusicActivity.songPosition
        } else {
            if (0 == PlayerMusicActivity.songPosition)
                PlayerMusicActivity.songPosition = PlayerMusicActivity.musicListPA.size - 1
            else --PlayerMusicActivity.songPosition
        }

    }
}
fun exitApplication() {
    if (PlayerMusicActivity.musicService != null) {
        //  PlayerMusicActivity.musicService!!.audioManager.abandonAudioFocus(PlayerMusicActivity.musicService)
        PlayerMusicActivity.musicService!!.stopForeground(true)
        PlayerMusicActivity.musicService!!.mediaPlayer!!.release()
        PlayerMusicActivity.musicService = null
    }
    exitProcess(1)
}

fun favouriteChecker(id : String) : Int {
    PlayerMusicActivity.isFavourite = false
    FavouriteActivity.favouriteSongs.forEachIndexed { index, music ->
        if (id == music.id){
            PlayerMusicActivity.isFavourite = true
            return index
        }
    }
    return  -1
}

fun checkPlaylist(playlist: ArrayList<Music>) : ArrayList<Music>{
    playlist.forEachIndexed { index, music ->
        val file = File(music.path)
        if (!file.exists())
            playlist.removeAt(index)
    }
    return playlist
}

fun setDialogBtnBackground(context: Context, dialog: AlertDialog){
    //setting button text
    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
        MaterialColors.getColor(context, R.attr.dialogTextColor, Color.WHITE)
    )
    dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
        MaterialColors.getColor(context, R.attr.dialogTextColor, Color.WHITE)
    )

    //setting button background
    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setBackgroundColor(
        MaterialColors.getColor(context, R.attr.dialogBtnBackground, Color.RED)
    )
    dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setBackgroundColor(
        MaterialColors.getColor(context, R.attr.dialogBtnBackground, Color.RED)
    )
}

fun getMainColor(img: Bitmap): Int {
    val newImg = Bitmap.createScaledBitmap(img, 1,1 , true)
    val color = newImg.getPixel(0, 0)
    newImg.recycle()
    return color
}

