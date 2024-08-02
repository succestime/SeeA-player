package com.jaidev.seeaplayer.dataClass

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class VideoData(val id : String, var title : String, val duration : Long = 0, val folderName : String, val size : String,
                     var path : String, var artUri : Uri ,val dateAdded: Long?,var isNew: Boolean = false , var selected: Boolean = false ,     var isPlayed: Boolean = false  // Flag to check if the video has been played
) :
    Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readParcelable(Uri::class.java.classLoader)!!,
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeLong(duration)
        parcel.writeString(folderName)
        parcel.writeString(size)
        parcel.writeString(path)
        parcel.writeParcelable(artUri, flags)
        parcel.writeValue(dateAdded)
        parcel.writeByte(if (isNew) 1 else 0)
        parcel.writeByte(if (selected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VideoData> {
        override fun createFromParcel(parcel: Parcel): VideoData {
            return VideoData(parcel)
        }

        override fun newArray(size: Int): Array<VideoData?> {
            return arrayOfNulls(size)
        }
    }
}