package com.jaidev.seeaplayer.dataClass


data class PlaylistVideo(
    val id: Long,
    var name: String,
    var video: List<String> = listOf() // Initialize with an empty list or fetch actual music if needed

    )


