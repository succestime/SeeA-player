package com.jaidev.seeaplayer.dataClass

import android.os.Parcel
import android.os.Parcelable
import com.jaidev.seeaplayer.MP3playerActivity
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.songPosition
import kotlin.system.exitProcess

data class MP3FileData(
    val id: String,
    val title: String,
    val duration: Long = 0,
    val size: String,
    val dateAdded: Long?,
    val path: String,
    var artUri: String
) : Parcelable {


    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeLong(duration)
        parcel.writeString(size)
        parcel.writeValue(dateAdded)
        parcel.writeString(path)
        parcel.writeString(artUri)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MP3FileData> {
        override fun createFromParcel(parcel: Parcel): MP3FileData {
            return MP3FileData(parcel)
        }

        override fun newArray(size: Int): Array<MP3FileData?> {
            return arrayOfNulls(size)
        }
    }


}
fun setMP3SongPosition(increment: Boolean) {
        if (increment) {
            if (MP3playerActivity.mp3MusicPA.size - 1 == songPosition)
                songPosition = 0
            else ++songPosition
        } else {
            if (0 == songPosition)
                songPosition = MP3playerActivity.mp3MusicPA.size - 1
            else --songPosition
        }

}
fun exitMP3Application() {
    if (MP3playerActivity.musicMP3Service != null) {
        //  PlayerMusicActivity.musicService!!.audioManager.abandonAudioFocus(PlayerMusicActivity.musicService)
        MP3playerActivity.musicMP3Service!!.stopForeground(true)
        MP3playerActivity.musicMP3Service!!.mediaPlayer!!.release()
        MP3playerActivity.musicMP3Service = null
    }
    exitProcess(1)
}
