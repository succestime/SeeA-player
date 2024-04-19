package com.jaidev.seeaplayer.dataClass

import android.net.Uri

data class VideoData(val id : String, var title : String, val duration : Long = 0, val folderName : String, val size : String,
                     var path : String, var artUri : Uri ,val dateAdded: Long?,var isNew: Boolean  , var selected: Boolean = false) {

//    companion object {
//
//        // Key for SharedPreferences
//        const val PREF_FILE_ITEM_PREFIX = "file_item_"
//
//        // Create a unique key for a specific FileItem
//        fun getPrefKey(fileItem: VideoData): String {
//            return PREF_FILE_ITEM_PREFIX + fileItem.path.hashCode()
//        }
//    }
//
//

}
