package com.jaidev.seeaplayer.dataClass

import android.content.Context

object SearchHistoryManager {
    private const val SEARCH_HISTORY_PREF = "search_history_pref"
    private const val MAX_SEARCH_HISTORY_SIZE = 10 // Maximum number of search history items to keep

    fun saveSearchHistory(context: Context, searchTopic: String) {
        val sharedPreferences = context.getSharedPreferences(SEARCH_HISTORY_PREF, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Retrieve the current search history set
        val searchHistorySet = getSearchHistory(context).toMutableSet()

        // Add the new search topic to the set
        searchHistorySet.add(searchTopic)

        // If the size exceeds the maximum limit, remove the oldest search topic
        if (searchHistorySet.size > MAX_SEARCH_HISTORY_SIZE) {
            searchHistorySet.remove(searchHistorySet.first())
        }

        // Save the updated search history set
        editor.putStringSet(SEARCH_HISTORY_PREF, searchHistorySet)
        editor.apply()
    }

    fun getSearchHistory(context: Context): List<String> {
        val sharedPreferences = context.getSharedPreferences(SEARCH_HISTORY_PREF, Context.MODE_PRIVATE)
        val searchHistorySet = sharedPreferences.getStringSet(SEARCH_HISTORY_PREF, setOf()) ?: setOf()
        return searchHistorySet.toList()
    }
}
