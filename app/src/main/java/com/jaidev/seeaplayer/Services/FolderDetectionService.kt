package com.jaidev.seeaplayer.Services

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import com.jaidev.seeaplayer.MainActivity.Companion.folderList
import com.jaidev.seeaplayer.dataClass.Folder

class FolderDetectionService : IntentService("FolderDetectionService") {

    private val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            // Trigger folder detection logic whenever a change occurs in MediaStore
            detectNewFolders()
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        // Register content observer to listen for changes in MediaStore
        applicationContext.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
        // Initial folder detection
        detectNewFolders()
    }

    private fun detectNewFolders() {
        val currentFolders = getCurrentFolders()
        val newFolders = getNewFolders(currentFolders)
        updateFolderList(newFolders)
    }

    private fun getCurrentFolders(): List<String> {
        // Retrieve the list of folders currently known to your app
        return folderList.map { it.folderName }
    }

    private fun getNewFolders(currentFolders: List<String>): List<String> {
        // Query media storage for new folders
        val newFolders = mutableListOf<String>()
        val projection = arrayOf(
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID
        )
        val selection = "${MediaStore.Video.Media.DATE_ADDED} >= ?"
        val selectionArgs = arrayOf((System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000).toString()) // Adjust the time frame as needed
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        applicationContext.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val folderNameColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val folderIdColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
            while (cursor.moveToNext()) {
                val folderName = cursor.getString(folderNameColumn)
                val folderId = cursor.getString(folderIdColumn)
                if (!currentFolders.contains(folderName)) {
                    newFolders.add(folderId)
                }
            }
        }

        return newFolders
    }

    private fun updateFolderList(newFolders: List<String>) {
        newFolders.forEach { folderId ->
            val folderName = getFolderNameById(folderId)
            val videoCount = getVideoCountInFolder(folderId)  // Get video count
            folderList.add(Folder(id = folderId, folderName = folderName, videoCount = videoCount))
        }
        // Notify your app's UI to update with the new folders
        // You may need to use a mechanism like EventBus or LiveData to communicate with your UI
    }

    @SuppressLint("Range")
    private fun getFolderNameById(folderId: String): String {
        var folderName = "Unknown"
        val projection = arrayOf(
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )
        val selection = "${MediaStore.Video.Media.BUCKET_ID} = ?"
        applicationContext.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(folderId),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                folderName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
            }
        }
        return folderName
    }

    @SuppressLint("Range")
    private fun getVideoCountInFolder(folderId: String): Int {
        val projection = arrayOf(MediaStore.Video.Media._ID)
        val selection = "${MediaStore.Video.Media.BUCKET_ID} = ?"
        val cursor = this.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(folderId),
            null
        )
        val count = cursor?.count ?: 0
        cursor?.close()
        return count
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the content observer when the service is destroyed
        applicationContext.contentResolver.unregisterContentObserver(contentObserver)
    }
}
