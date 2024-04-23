package com.jaidev.seeaplayer.dataClass

data class SearchHistoryItem(
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
