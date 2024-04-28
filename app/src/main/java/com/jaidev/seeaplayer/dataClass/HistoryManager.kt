package com.jaidev.seeaplayer.dataClass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object HistoryManager {
    private const val HISTORY_PREF_KEY = "history_items"

    fun addHistoryItem(item: HistoryItem, context: Context) {
        val historyList = getHistoryList(context).toMutableList()

        // Check if the item already exists in the history list based on URL
        val existingItem = historyList.find { it.url == item.url }

        if (existingItem != null) {
            // If the item already exists, remove it first
            historyList.remove(existingItem)
        }

        // Add the new item to the beginning of the list
        historyList.add(0, item)

        // Save the updated history list
        saveHistoryList(historyList, context)
    }


    fun getHistoryList(context: Context): List<HistoryItem> {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val historySet = sharedPreferences.getStringSet(HISTORY_PREF_KEY, setOf()) ?: setOf()

        return historySet.mapNotNull { itemString ->
            val parts = itemString.split("|||")
            if (parts.size == 4) {
                val url = parts[0]
                val title = parts[1]
                val timestamp = parts[2].toLong()
                val imageBase64 = parts[3]
                val imageBitmap = decodeBase64ToBitmap(imageBase64)
                HistoryItem(url, title, timestamp, imageBitmap)
            } else {
                null
            }
        }.sortedByDescending { it.timestamp } // Sort by timestamp in descending order (newest first)
    }

    private fun saveHistoryList(historyList: List<HistoryItem>, context: Context) {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val historySet = historyList.map { item ->
            "${item.url}|||${item.title}|||${item.timestamp}|||${encodeBitmapToBase64(item.imageBitmap)}"
        }.toSet()
        editor.putStringSet(HISTORY_PREF_KEY, historySet)
        editor.apply()
    }

    fun extractWebsiteTitle(url: String): String {
        return Uri.parse(url).host ?: ""
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap?): String {
        if (bitmap != null) {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
        return ""
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        if (base64String.isNotEmpty()) {
            val decodedByteArray = Base64.decode(base64String, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.size)
        }
        return null
    }

    fun deleteHistoryItem(historyItem: HistoryItem, context: Context) {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val historyList = getHistoryList(context).toMutableList()
        val removed = historyList.remove(historyItem)
        if (removed) {
            saveHistoryList(historyList, context)
        }
    }


    fun clearHistory(context: Context) {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(HISTORY_PREF_KEY) // Remove the specific key storing history data
        editor.apply()
    }




}
