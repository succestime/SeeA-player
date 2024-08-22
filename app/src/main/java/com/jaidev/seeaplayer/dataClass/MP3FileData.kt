package com.jaidev.seeaplayer.dataClass

import android.os.Parcel
import android.os.Parcelable

data class MP3FileData(
    val id: String,
    val title: String,
    val duration: Long = 0,
    val size: String,
    val dateAdded: Long?,
    val path: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeLong(duration)
        parcel.writeString(size)
        parcel.writeValue(dateAdded)
        parcel.writeString(path)
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
