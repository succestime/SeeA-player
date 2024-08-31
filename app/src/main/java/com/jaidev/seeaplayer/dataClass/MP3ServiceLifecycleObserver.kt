package com.jaidev.seeaplayer.dataClass

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.jaidev.seeaplayer.MP3playerActivity
import com.jaidev.seeaplayer.Services.MP3Service

class MP3ServiceLifecycleObserver(private val service: MP3Service) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onAppDestroyed() {
        service.stopService()
        MP3playerActivity.musicMP3Service = null
    }
}
