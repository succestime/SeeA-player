package com.jaidev.seeaplayer.dataClass.MP3Data

import android.content.Context
import android.os.Environment
import android.os.FileObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Define a callback interface in MP3FileObserver
class MP3FileObserver(private val context: Context, private val onFileDeleted: (String) -> Unit) {

    private val customDirectoryName = "SeeA MP3 Audio"
    private val db = MP3AppDatabase.getDatabase(context)
    private val directoryToWatch = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
        customDirectoryName
    ).path

    private val fileObserver = object : FileObserver(directoryToWatch, DELETE) {
        override fun onEvent(event: Int, path: String?) {
            if (event == DELETE && path != null) {
                val deletedFilePath = "$directoryToWatch/$path"
                handleFileDeletion(deletedFilePath)
            }
        }
    }

    fun startWatching() {
        fileObserver.startWatching()
    }

    fun stopWatching() {
        fileObserver.stopWatching()
    }

    private fun handleFileDeletion(filePath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val mp3File = db.mp3FileDao().getAllMP3Files().find { it.path == filePath }
            mp3File?.let {
                db.mp3FileDao().deleteById(it.id)
                // Notify the activity about the deletion
                withContext(Dispatchers.Main) {
                    onFileDeleted(filePath)
                }
            }
        }
    }
}

