package com.jaidev.seeaplayer.Services

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jaidev.seeaplayer.MP3playerActivity
import com.jaidev.seeaplayer.musicActivity.NotificationReceiver

class MP3NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                // Handle play/pause action
                val mp3playerIntent = Intent(context, MP3playerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(mp3playerIntent)
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.jaidev.seeaplayer.ACTION_PLAY_PAUSE"

        fun getPlayPausePendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_PLAY_PAUSE
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
