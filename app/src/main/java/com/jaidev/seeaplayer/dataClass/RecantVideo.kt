package com.jaidev.seeaplayer.dataClass

import android.net.Uri

data class RecantVideo(var title: String,
                       val timestamp: Long,
                       val id: String, val
                       duration: Long = 0,
                       var path: String,
                       var artUri: Uri,
                       val size : String


) {

}
