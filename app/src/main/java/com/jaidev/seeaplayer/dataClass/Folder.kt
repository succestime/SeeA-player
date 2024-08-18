package com.jaidev.seeaplayer.dataClass

data class Folder(
    val id: String,
    val folderName: String,
    val videoCount: Int  // Add this line to store the number of videos in the folder
)
