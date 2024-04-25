package com.jaidev.seeaplayer.Services

import android.content.Context
import android.content.SharedPreferences
import com.jaidev.seeaplayer.dataClass.FileItem

class FileItemPreferences(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("file_item_prefs", Context.MODE_PRIVATE)
    }

    // Save updated FileItem to SharedPreferences
    fun saveFileItem(fileItem: FileItem) {
        val key = FileItem.getPrefKey(fileItem)
        prefs.edit().putString(key, fileItem.originalFileName).apply()
    }

    // Retrieve FileItem's originalFileName from SharedPreferences
    fun getOriginalFileName(fileItem: FileItem): String {
        val key = FileItem.getPrefKey(fileItem)
        return prefs.getString(key, fileItem.originalFileName) ?: fileItem.originalFileName
    }
}
