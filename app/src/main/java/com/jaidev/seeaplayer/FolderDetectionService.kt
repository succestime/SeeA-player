package com.jaidev.seeaplayer
import android.app.IntentService
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import com.jaidev.seeaplayer.MainActivity.Companion.folderList
import com.jaidev.seeaplayer.dataClass.Folder
import java.util.UUID


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
        val projection = arrayOf(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
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
            while (cursor.moveToNext()) {
                val folderName = cursor.getString(folderNameColumn)
                if (!currentFolders.contains(folderName)) {
                    newFolders.add(folderName)
                }
            }
        }

        return newFolders
    }

    private fun updateFolderList(newFolders: List<String>) {
        // Add new folders to your app's folder list
        folderList.addAll(newFolders.map { Folder(UUID.randomUUID().toString(), it) })
        // Notify your app's UI to update with the new folders
        // You may need to use a mechanism like EventBus or LiveData to communicate with your UI
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the content observer when the service is destroyed
        applicationContext.contentResolver.unregisterContentObserver(contentObserver)
    }
}

