package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FileDownloadService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "FileDownloadChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action != null) {
            when (intent.action) {
                "START_DOWNLOAD" -> {
                    val downloadUrl = intent.getStringExtra("downloadUrl")
                    val fileName = intent.getStringExtra("fileName")
                    if (!downloadUrl.isNullOrEmpty() && !fileName.isNullOrEmpty()) {
                        startFileDownload(downloadUrl, fileName)
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startFileDownload(downloadUrl: String, fileName: String) {
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true) // Set according to your requirements

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // Show notification for download in progress
        showDownloadProgressNotification(downloadId, fileName)
    }

    @SuppressLint("ForegroundServiceType", "MissingPermission")
    private fun showDownloadProgressNotification(downloadId: Long, fileName: String) {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading $fileName")
            .setSmallIcon(R.drawable.download_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true) // Remove this line if you want the notification to be persistent
            .build()

        startForeground(NOTIFICATION_ID, notificationBuilder)

        // Update notification progress
        GlobalScope.launch(Dispatchers.IO) {
            var progress = 0
            while (progress < 100) {
                progress += 5 // Simulate download progress increment
                notificationBuilder.setProgress(100, progress, false)
                NotificationManagerCompat.from(this@FileDownloadService).notify(NOTIFICATION_ID,
                    notificationBuilder
                )
                delay(1000) // Simulate delay
            }
            // Download complete, show completion notification
            showDownloadCompleteNotification(fileName)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showDownloadCompleteNotification(fileName: String) {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download complete")
            .setContentText("File $fileName downloaded successfully")
            .setSmallIcon(R.drawable.download_icon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, notificationBuilder.build())
        }
        stopForeground(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "File Downloads"
            val descriptionText = "Notifications for file downloads"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

fun Notification.setProgress(maxProgress: Int, progress: Int, indeterminate: Boolean): Notification {
    return if (this is NotificationCompat.Builder) {
        this.setProgress(maxProgress, progress, indeterminate)
            .build()
    } else {
        // For other types of Notification instances, handle accordingly
        this
    }
}

