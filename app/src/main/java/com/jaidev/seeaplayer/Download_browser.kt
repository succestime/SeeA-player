package com.jaidev.seeaplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.allAdapters.DownloadBrowserAdapter
import com.jaidev.seeaplayer.dataClass.DownloadItem

class Download_browser : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_browser)


        val recyclerView: RecyclerView = findViewById(R.id.downloadRecyclerView)

        // Create instances of DownloadItem
        val imageItem = DownloadItem.ImageItem("https://example.com/image.jpg")
        val videoItem = DownloadItem.VideoItem("Sample Video", "100 MB")
        val pdfItem = DownloadItem.PdfItem("Sample PDF", "5 MB")
        val apkItem = DownloadItem.ApkItem("Sample App", "20 MB")

        // Populate a list of DownloadItem items
        val items: List<DownloadItem> = listOf(imageItem, videoItem, pdfItem, apkItem)

        // Create and set up the adapter
        val adapter = DownloadBrowserAdapter(items)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
}