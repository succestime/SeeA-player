package com.jaidev.seeaplayer.dataClass

import android.net.Uri

data class VideoData(val id : String, var title : String, val duration : Long = 0, val folderName : String, val size : String,
                     var path : String, var artUri : Uri ,val dateAdded: Long?,var isNew: Boolean  , var selected: Boolean = false) {

    fun updateAfterRename(newTitle: String, newPath: String, newArtUri: Uri?) {
        title = newTitle
        path = newPath
        if (newArtUri != null) {
            artUri = newArtUri
        }
        // Update any other properties you need to
    }

}
