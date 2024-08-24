package com.jaidev.seeaplayer.Services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class MediaButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val serviceIntent = Intent(context, MediaPlayerService::class.java)

        when (action) {
            MediaPlayerService.ACTION_PLAY -> {
                serviceIntent.action = MediaPlayerService.ACTION_PLAY
                context.startService(serviceIntent)
            }
            MediaPlayerService.ACTION_PAUSE -> {
                serviceIntent.action = MediaPlayerService.ACTION_PAUSE
                context.startService(serviceIntent)
            }
            MediaPlayerService.ACTION_NEXT -> {
                serviceIntent.action = MediaPlayerService.ACTION_NEXT
                context.startService(serviceIntent)
            }
            MediaPlayerService.ACTION_PREVIOUS -> {
                serviceIntent.action = MediaPlayerService.ACTION_PREVIOUS
                context.startService(serviceIntent)
            }
        }
    }
}