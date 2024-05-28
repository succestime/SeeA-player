package com.jaidev.seeaplayer.dataClass

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast

object FileDownloader {

    private var downloadManager: DownloadManager? = null

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun downloadFile(context: Context, fileUrl: String, fileName: String) {
        downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(fileUrl))
            .setTitle(fileName)
            .setDescription("Downloading")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("image/svg+xml")  // Explicitly setting the MIME type for SVG files

        val downloadId = downloadManager?.enqueue(request)

        // Register a BroadcastReceiver to receive the download complete event
        val onComplete = object : BroadcastReceiver() {
            @SuppressLint("Range")
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    // Download completed
                    val query = DownloadManager.Query().setFilterById(downloadId!!)
                    val cursor = downloadManager?.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val status =
                            cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            // Download was successful, handle the downloaded file
                            val localUri =
                                cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                            Toast.makeText(
                                context,
                                "File downloaded successfully: $localUri",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.d("FileDownloader", "File downloaded successfully: $localUri")
                            // Perform any other required actions here
                        } else {
                            val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                            Log.e("FileDownloader", "Download failed with status: $status, reason: $reason")
                        }
                        cursor.close()
                    } else {
                        Log.e("FileDownloader", "Cursor is null or empty")
                    }
                } else {
                    Log.e("FileDownloader", "Download ID mismatch: expected $downloadId, got $id")
                }
            }
        }

        // Register the BroadcastReceiver
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }
}
