package com.jaidev.seeaplayer.dataClass.MusicPlaylistData


data class PlaylistMusic(
    val musicid: Long,
    var name: String,
    var music: List<String> = listOf() // Initialize with an empty list or fetch actual music if needed

    ) {


}

