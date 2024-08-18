package com.jaidev.seeaplayer.dataClass


data class PlaylistMusic(
    val id: Long,
    var name: String,
    var music: List<String> = listOf() // Initialize with an empty list or fetch actual music if needed

    ) {


}

