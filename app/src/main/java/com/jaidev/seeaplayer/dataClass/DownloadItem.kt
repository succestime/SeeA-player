package com.jaidev.seeaplayer.dataClass

sealed class DownloadItem(
    var isDownloading: Boolean = false,
    var downloadProgress: Int = 0
) {
    data class ImageItem(val imageUrl: String) : DownloadItem()
    data class VideoItem(val videoName: String, val videoSize: String) : DownloadItem()
    data class PdfItem(val pdfTitle: String, val details: String) : DownloadItem()
    data class ApkItem(val apkName: String, val size: String) : DownloadItem()
}
