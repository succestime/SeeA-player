package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

class DownloadCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            // Handle download completion here
            handleDownloadCompletion(context, downloadId)
        }
    }

    @SuppressLint("Range")
    private fun handleDownloadCompletion(context: Context?, downloadId: Long) {
        // Retrieve information about the completed download using the downloadId
        val downloadManager =
            context?.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager?.query(query)

        cursor?.use {
            if (it.moveToFirst()) {
                val status = it.getInt(it.getColumnIndex(DownloadManager.COLUMN_STATUS))
                val filePath = it.getString(it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    // Download was successful, handle the downloaded file
                    filePath?.let { path ->
                        // Example: Display a notification or update UI with the downloaded file
                        displayDownloadNotification(context, path)
                    }
                } else {
                    // Handle download failure or other statuses if needed
                }
            }
        }
    }

    private fun displayDownloadNotification(context: Context?, filePath: String) {
        // Example: Display a notification with the downloaded file path
        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        // Create a notification channel for Android Oreo and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "download_channel",
                "Download Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager?.createNotificationChannel(channel)
        }

        val notificationBuilder = context?.let {
            NotificationCompat.Builder(it, "download_channel")
                .setContentTitle("File Downloaded")
                .setContentText("File downloaded successfully: $filePath")
                .setSmallIcon(R.drawable.video_browser)
                .setAutoCancel(true)
        }

        // Create an intent to open the downloaded file
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                Uri.parse(filePath),
                "application/pdf"
            ) // Set appropriate MIME type based on file type
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingIntent =
            PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationBuilder?.setContentIntent(pendingIntent)

        // Display the notification
        notificationManager?.notify(1, notificationBuilder?.build())
    }


}
