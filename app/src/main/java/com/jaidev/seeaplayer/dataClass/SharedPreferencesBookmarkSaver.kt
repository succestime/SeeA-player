package com.jaidev.seeaplayer.dataClass

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jaidev.seeaplayer.allAdapters.BookmarkAdapter

class SharedPreferencesBookmarkSaver(private val context: Context) : BookmarkAdapter.BookmarkSaver {
    private val sharedPreferences = context.getSharedPreferences("Bookmarks", Context.MODE_PRIVATE)

    override fun saveBookmarks(bookmarkList: ArrayList<Bookmark>) {
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(bookmarkList)
        editor.putString("bookmarkList", json)
        editor.apply()
    }

    fun loadBookmarks(): ArrayList<Bookmark> {
        val json = sharedPreferences.getString("bookmarkList", null)
        return if (json != null) {
            val type = object : TypeToken<ArrayList<Bookmark>>() {}.type
            Gson().fromJson(json, type)
        } else {
            arrayListOf()
        }
    }
}
