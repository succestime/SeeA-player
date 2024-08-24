package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.format.DateUtils
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.FileProvider
import com.jaidev.seeaplayer.Services.MediaPlayerService
import com.jaidev.seeaplayer.dataClass.MP3FileData
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import java.io.File

class MP3playerActivity : AppCompatActivity() {

    private lateinit var playPauseBtn: ImageView
    private lateinit var pervBtn: ImageView
    private lateinit var nextBtn: ImageView
    private lateinit var seekBar: AppCompatSeekBar
    private lateinit var elapsedTimeLabel: TextView
    private lateinit var remainingTimeLabel: TextView
    private lateinit var titleTextView: TextView
    private lateinit var shareMP3Layout: LinearLayout



    companion object{
        var isReceiverRegistered = false
        lateinit var mp3Files: ArrayList<MP3FileData>
        var currentIndex: Int = 0
        var mediaPlayerService: MediaPlayerService? = null
    }

    private var isBound = false

    private val trackTitleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val newTitle = intent.getStringExtra("trackTitle")
            newTitle?.let {
                titleTextView.text = it
            }
        }
    }

    private val playPauseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isPlaying = intent.getBooleanExtra("isPlaying", false)
            updatePlayPauseButton(isPlaying)
            if (isPlaying) {
                updateSeekBar()
            }
        }
    }


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaPlayerService.MediaPlayerBinder
            mediaPlayerService = binder.getService()
            isBound = true


                mediaPlayerService?.setMp3Files(mp3Files, currentIndex)
                mediaPlayerService?.onTrackCompleteListener = {
                    mediaPlayerService?.moveToNextTrack()
                }

                playMP3File(mp3Files[currentIndex])

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaPlayerService = null
            isBound = false
        }
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag", "ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this, theme)
        setContentView(R.layout.activity_mp3player)
supportActionBar?.hide()
        playPauseBtn = findViewById(R.id.playPauseBtn)
        pervBtn = findViewById(R.id.pervBtn)
        nextBtn = findViewById(R.id.nextBtn)
        seekBar = findViewById(R.id.seekBar)
        elapsedTimeLabel = findViewById(R.id.elapsedTimeLabel)
        remainingTimeLabel = findViewById(R.id.remainingTimeLabel)
        titleTextView = findViewById(R.id.titleTextView)
        shareMP3Layout = findViewById(R.id.shareMP3Layout)

        val mp3FilesExtra = intent.getSerializableExtra("mp3Files")
        if (mp3FilesExtra is ArrayList<*>) {
            @Suppress("UNCHECKED_CAST")
            mp3Files = mp3FilesExtra as ArrayList<MP3FileData>
        } else {
            mp3Files = arrayListOf()
        }
        // ... existing setup code ...
        handleIntent(intent)
        currentIndex = intent.getIntExtra("currentIndex", 0)

        Intent(this, MediaPlayerService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        registerReceiver(playPauseReceiver, IntentFilter("PLAY_PAUSE_ACTION"))
        registerReceiver(trackTitleReceiver, IntentFilter("TRACK_TITLE"))
        isReceiverRegistered = true
        playPauseBtn.setOnClickListener {
            if (isBound) {
                if (mediaPlayerService?.isPlaying == true) {
                    mediaPlayerService?.pauseAudio()
                } else {
                    mediaPlayerService?.resumeAudio()
                    updateSeekBar()
                }
                updatePlayPauseButton(mediaPlayerService?.isPlaying == true)
                mediaPlayerService?.updateNotification()
            }
        }
        shareMP3Layout.setOnClickListener {
            if (isBound) {
                val mp3FileData = mp3Files[currentIndex] // Get the current MP3 file
                shareMP3File(mp3FileData)
            }
        }
        nextBtn.setOnClickListener {
            if (isBound) {
                mediaPlayerService?.moveToNextTrack()
                updatePlayPauseButton(mediaPlayerService?.isPlaying == true)
                updateSeekBar()
                updateTrackTitle()  // Update the track title after changing the track
            }
        }

        pervBtn.setOnClickListener {
            if (isBound) {
                mediaPlayerService?.moveToPreviousTrack()
                updatePlayPauseButton(mediaPlayerService?.isPlaying == true)
                updateSeekBar()
                updateTrackTitle()  // Update the track title after changing the track
            }
        }


        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isBound) {
                    mediaPlayerService?.seekTo(progress)
                    updateTimeLabels()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        updateTimeLabels()
    }
    private fun updateTrackTitle() {
        if (isBound) {
            // Assuming mediaPlayerService has a method to get the current track title or MP3FileData
            val currentTrackTitle = mediaPlayerService?.getCurrentTrackTitle()
            titleTextView.text = currentTrackTitle ?: "Unknown Track"
        }
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            handleIntent(it)
        }
    }
    @SuppressLint("ObsoleteSdkInt")
    private fun shareMP3File(mp3FileData: MP3FileData) {
        val mp3File = File(mp3FileData.path)
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Use FileProvider to share the file on Android N and above
            FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                mp3File
            )
        } else {
            Uri.fromFile(mp3File)
        }

        // Create the share intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Start the sharing activity
        startActivity(Intent.createChooser(shareIntent, "Share MP3 file via"))
    }
    private fun handleIntent(intent: Intent) {
        val mp3FilePath = intent.getStringExtra("mp3FilePath")
        val mp3FileTitle = intent.getStringExtra("mp3FileTitle")
        val newMp3Files = intent.getSerializableExtra("mp3Files") as? ArrayList<MP3FileData>
        val newIndex = intent.getIntExtra("currentIndex", 0)

        if (mp3FilePath != null && mp3FileTitle != null) {
            if (isBound && mediaPlayerService != null) {
                // Update the MP3 file list if provided
                newMp3Files?.let {
                    mp3Files = it
                }
                currentIndex = newIndex

                // Play the new file
                playMP3File(mp3Files[currentIndex])
            }
        }
    }





    private fun broadcastNowPlaying() {
        if (isBound) {
            val intent = Intent("NOW_PLAYING_ACTION")
            intent.putExtra("isPlaying", mediaPlayerService?.isPlaying)
            intent.putExtra("trackTitle", mediaPlayerService?.getCurrentTrackTitle())
            sendBroadcast(intent)
            Log.d("MP3playerActivity", "Broadcasting now playing: ${mediaPlayerService?.getCurrentTrackTitle()}")
        }
    }
    private fun playMP3File(mp3FileData: MP3FileData) {
        if (isBound) {
            mediaPlayerService?.playMP3(mp3FileData.path, mp3FileData.title)
            titleTextView.text = mp3FileData.title
            updatePlayPauseButton(mediaPlayerService?.isPlaying == true)
            updateSeekBar()
            broadcastNowPlaying() // Notify other activities about the current track

        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        if (isBound) {
            val iconRes = if (isPlaying) R.drawable.round_pause_circle_outline_24 else R.drawable.round_play_circle_outline_24
            playPauseBtn.setImageResource(iconRes)
        }
        playPauseBtn.setImageResource(
            if (isPlaying) R.drawable.round_pause_circle_outline_24 else R.drawable.round_play_circle_outline_24
        )
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun updateSeekBar() {
        if (isBound && mediaPlayerService?.isPlaying == true) {
            seekBar.progress = mediaPlayerService?.getCurrentPosition() ?: 0
            seekBar.max = mediaPlayerService?.getDuration() ?: 0

            // Re-run this method every 1 second to update the progress
            seekBar.postDelayed({ updateSeekBar() }, 100)

            updateTimeLabels()
        }
    }

    private fun updateTimeLabels() {
        if (isBound) {
            val totalDuration = mediaPlayerService?.getDuration()?.div(1000) ?: 0
            elapsedTimeLabel.text = DateUtils.formatElapsedTime(totalDuration.toLong())
            val currentProgress = mediaPlayerService?.getCurrentPosition()?.div(1000) ?: 0
            remainingTimeLabel.text = DateUtils.formatElapsedTime(currentProgress.toLong())
        }
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        registerReceiver(trackTitleReceiver, IntentFilter("TRACK_TITLE"))
        registerReceiver(playPauseReceiver, IntentFilter("PLAY_PAUSE_ACTION"))
    }

    override fun onPause() {
        super.onPause()
        if (isReceiverRegistered) {
            unregisterReceiver(trackTitleReceiver)
            unregisterReceiver(playPauseReceiver)
            isReceiverRegistered = false
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }

    }


}
