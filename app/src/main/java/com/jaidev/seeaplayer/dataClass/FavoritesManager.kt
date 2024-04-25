
package com.jaidev.seeaplayer

//import android.content.Context
//import android.content.Context.MODE_PRIVATE
//import com.google.gson.GsonBuilder
//import com.google.gson.reflect.TypeToken
//import com.jaidev.seeaplayer.dataClass.Music
//
//
//object FavoritesManager {
//    var favouriteSongs: ArrayList<Music> = ArrayList()
//    fun loadFavorites(context: Context) {
//FavouriteActivity.favouriteSongs = ArrayList()
//        val editor = context.getSharedPreferences("FAVOURITES", Context.MODE_PRIVATE)
//        val jsonString = editor.getString("FavouriteSongs", null)
//        val typeToken = object : TypeToken<ArrayList<Music>>(){}.type
//        if (jsonString != null) {
//            val data : ArrayList<Music> = GsonBuilder().create().fromJson(jsonString, typeToken)
//                 FavouriteActivity.favouriteSongs.addAll(data)
//        }
//    }
//    fun saveFavorites(context: Context) {
//
//        val editor = context.getSharedPreferences("FAVOURITES", MODE_PRIVATE).edit()
//        val jsonString = GsonBuilder().create().toJson(FavouriteActivity.favouriteSongs)
//        editor.putString("FavouriteSongs", jsonString)
//        editor.apply()
//    }
//}
