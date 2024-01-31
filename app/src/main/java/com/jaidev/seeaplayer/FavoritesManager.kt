package com.jaidev.seeaplayer

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jaidev.seeaplayer.dataClass.Music


object FavoritesManager {
    var favouriteSongs: ArrayList<Music> = ArrayList()

    fun saveFavorites(context: Context) {

        val editor = context.getSharedPreferences("FAVOURITES", Context.MODE_PRIVATE).edit()
        val jsonString = GsonBuilder().create().toJson(favouriteSongs)
        editor.putString("FavouriteSongs", jsonString)
        editor.apply()
    }

    fun loadFavorites(context: Context) {

        val preferences = context.getSharedPreferences("FAVOURITES", Context.MODE_PRIVATE)
        val jsonString = preferences.getString("FavouriteSongs", null)
        val typeToken = object : TypeToken<ArrayList<Music>>() {}.type
        if (jsonString != null) {
            favouriteSongs = GsonBuilder().create().fromJson(jsonString, typeToken)

        }
    }
}