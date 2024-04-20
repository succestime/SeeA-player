package com.jaidev.seeaplayer.dataClass

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PlaylistSharedPreferences {
    private const val PREF_KEY_PLAYLISTS = "playlists"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
    }

    fun savePlaylists(context: Context, playlists: List<Playlist>) {
        val editor = getSharedPreferences(context).edit()
        val json = Gson().toJson(playlists)
        editor.putString(PREF_KEY_PLAYLISTS, json)
        editor.apply()
    }

    fun getPlaylists(context: Context): ArrayList<Playlist> {
        val json = getSharedPreferences(context).getString(PREF_KEY_PLAYLISTS, null)
        return if (json != null) {
            Gson().fromJson(json, object : TypeToken<ArrayList<Playlist>>() {}.type)
        } else {
            ArrayList()
        }
    }
}

