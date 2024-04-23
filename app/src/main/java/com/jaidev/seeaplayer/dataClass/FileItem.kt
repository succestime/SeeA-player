package com.jaidev.seeaplayer.dataClass
import android.net.Uri

data class FileItem(
    var fileName: String,
    var filePath: String,
    val fileSize: Long,
    val fileType: FileType,
    val artUri: Uri,
    val lastModifiedTimestamp: Long,
    var originalFileName: String,
    val websiteName: String? = null, // Define websiteName as a nullable String


) {


    companion object {

        // Key for SharedPreferences
        const val PREF_FILE_ITEM_PREFIX = "file_item_"

        // Create a unique key for a specific FileItem
        fun getPrefKey(fileItem: FileItem): String {
            return PREF_FILE_ITEM_PREFIX + fileItem.filePath.hashCode()
        }


    }

}

enum class FileType {
    PDF,
    IMAGE,
    VIDEO,
    AUDIO,
    WEBSITE,  // New type for website files
    APK,      // New type for APK files
    UNKNOWN
}
