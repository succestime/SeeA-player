package com.jaidev.seeaplayer.musicActivity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.jaidev.seeaplayer.MP3playerActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.Services.MediaPlayerService
import com.jaidev.seeaplayer.databinding.FragmentMP3NowPlayingBinding

class MP3NowPlaying : Fragment() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: FragmentMP3NowPlayingBinding
    }

    private var isBound = false
    private lateinit var songNameNP: TextView

    private var mediaPlayerService: MediaPlayerService? = null

    private val trackTitleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val newTitle = intent.getStringExtra("trackTitle")
            newTitle?.let {
                songNameNP.text = it
            }
        }
    }

    private val playPauseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isPlaying = intent.getBooleanExtra("isPlaying", false)
            updatePlayPauseButton(isPlaying)
            updatePlayPauseButton(MP3playerActivity.mediaPlayerService?.isPlaying == true)

        }
    }
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaPlayerService.MediaPlayerBinder
            mediaPlayerService = binder.getService()
            isBound = true

            MP3playerActivity.mediaPlayerService?.setMp3Files(
                MP3playerActivity.mp3Files,
                MP3playerActivity.currentIndex
            )

            MP3playerActivity.mediaPlayerService?.onTrackCompleteListener = {
                MP3playerActivity.mediaPlayerService?.moveToNextTrack()
                updateTrackTitle()
                updatePlayPauseButton(true)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaPlayerService = null
            isBound = false
        }
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_m_p3_now_playing, container, false)
        binding = FragmentMP3NowPlayingBinding.bind(view)
        binding.root.visibility = View.GONE
        songNameNP = binding.songNameNP

        requireContext().registerReceiver(playPauseReceiver, IntentFilter("PLAY_PAUSE_ACTION"))
        requireContext().registerReceiver(trackTitleReceiver, IntentFilter("TRACK_TITLE"))
        MP3playerActivity.isReceiverRegistered = true

        initializeLayout()

        Intent(requireContext(), MediaPlayerService::class.java).also { intent ->
            requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        binding.playPauseBtnNP.setOnClickListener {
            if (isBound) {
                if (MP3playerActivity.mediaPlayerService?.isPlaying == true) {
                    MP3playerActivity.mediaPlayerService?.pauseAudio()
                } else {
                    MP3playerActivity.mediaPlayerService?.resumeAudio()
                }
                updatePlayPauseButton(MP3playerActivity.mediaPlayerService?.isPlaying == true)
                MP3playerActivity.mediaPlayerService?.updateNotification()
            }
        }

        binding.nextBtnNP.setOnClickListener {
            if (isBound) {
                MP3playerActivity.mediaPlayerService?.moveToNextTrack()
                updateTrackTitle()
                updatePlayPauseButton(true)
            }
        }

        binding.root.setOnClickListener {
            if (isBound) {
                val intent = Intent(requireContext(), MP3playerActivity::class.java).apply {
                    putExtra("mp3Files", MP3playerActivity.mp3Files)
                    putExtra("currentIndex", MP3playerActivity.mediaPlayerService?.getCurrentTrackIndex() ?: MP3playerActivity.currentIndex)
                }
                startActivity(intent)
            }
        }



        return view
    }

    private fun updateTrackTitle() {
        if (isBound) {
            val currentTrackTitle = MP3playerActivity.mediaPlayerService?.getCurrentTrackTitle()
            songNameNP.text = currentTrackTitle ?: "Unknown Track"
        }
    }
    private fun initializeLayout() {
        updateTrackTitle()
        updatePlayPauseButton(mediaPlayerService?.isPlaying == true)
        binding.root.visibility = View.VISIBLE
    }
    private fun updatePlayPauseButton(isPlaying: Boolean) {
        if (isBound) {
            val iconRes = if (isPlaying) R.drawable.round_pause_circle_outline_24 else R.drawable.round_play_circle_outline_24
            binding.playPauseBtnNP.setIconResource(iconRes)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()

        if (MP3playerActivity.mediaPlayerService != null) {
            requireContext().registerReceiver(trackTitleReceiver, IntentFilter("TRACK_TITLE"))
            requireContext().registerReceiver(playPauseReceiver, IntentFilter("PLAY_PAUSE_ACTION"))
            binding.root.visibility = View.VISIBLE
            initializeLayout()
            updatePlayPauseButton(MP3playerActivity.mediaPlayerService?.isPlaying == true)
        } else {
            binding.root.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(trackTitleReceiver)
        requireContext().unregisterReceiver(playPauseReceiver)
    }
}
