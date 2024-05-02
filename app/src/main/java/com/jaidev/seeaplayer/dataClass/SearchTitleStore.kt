package com.jaidev.seeaplayer.dataClass

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Define a class to store the titles
class SearchTitleStore {
    companion object {
        private const val SHARED_PREF_NAME = "search_titles"
        private const val KEY_TITLES = "titles"

        // Function to add a title to the list
        fun addTitle(context: Context, title: SearchTitle) {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            val titles = getTitles(context).toMutableList()
            titles.add(title)
            saveTitles(context, titles)
        }

        // Function to get all stored titles
        fun getTitles(context: Context): List<SearchTitle> {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            val jsonTitles = sharedPreferences.getString(KEY_TITLES, null)
            return if (jsonTitles != null) {
                val type = object : TypeToken<List<SearchTitle>>() {}.type
                Gson().fromJson(jsonTitles, type)
            } else {
                emptyList()
            }
        }
        // Function to delete a title from the list
  


        // Function to save titles
        fun saveTitles(context: Context, titles: List<SearchTitle>) {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            val jsonTitles = Gson().toJson(titles)
            sharedPreferences.edit().putString(KEY_TITLES, jsonTitles).apply()
        }


    }
}
