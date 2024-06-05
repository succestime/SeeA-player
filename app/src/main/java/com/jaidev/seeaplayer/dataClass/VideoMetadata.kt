package com.jaidev.seeaplayer.dataClass

data class VideoMetadata(
    val title: String,
    val imageUrl: String,
    val resolutions: List<String>,
    val formats: List<String>

)


