
package com.jaidev.seeaplayer.dataClass


import android.media.MediaMetadataRetriever
import android.os.Parcel
import android.os.Parcelable
import com.jaidev.seeaplayer.musicActivity.FavouriteActivity
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

data class Music(
    val id: String,
    var title: String,
    val album: String,
    val artist: String,
    val duration: Long = 0,
    var path: String,
    val size: String,
    var artUri: String,
    val dateAdded: Long?,
    var selected: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(album)
        parcel.writeString(artist)
        parcel.writeLong(duration)
        parcel.writeString(path)
        parcel.writeString(size)
        parcel.writeString(artUri)
        parcel.writeValue(dateAdded)
        parcel.writeByte(if (selected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Music> {
        override fun createFromParcel(parcel: Parcel): Music {
            return Music(parcel)
        }

        override fun newArray(size: Int): Array<Music?> {
            return arrayOfNulls(size)
        }
    }
}


class Playlist{
    lateinit var name: String
    lateinit var playlist: ArrayList<Music>
    lateinit var createdBy: String
    lateinit var createdOn: String
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



