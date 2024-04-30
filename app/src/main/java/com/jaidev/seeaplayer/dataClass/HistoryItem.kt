package com.jaidev.seeaplayer.dataClass

import android.graphics.Bitmap

data class HistoryItem(
    val url: String,
    val title: String,
    var timestamp: Long,
    var imageBitmap: Bitmap?,

){
    override fun equals(other: Any?): Boolean {
        return if (other is HistoryItem) {
            this.url == other.url
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }


}
