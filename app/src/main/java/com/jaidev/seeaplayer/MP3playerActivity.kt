package com.jaidev.seeaplayer

import android.media.MediaPlayer
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.ImageView
import android.widget.SeekBar

import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSeekBar
import com.jaidev.seeaplayer.dataClass.MP3FileData
import com.jaidev.seeaplayer.dataClass.ThemeHelper

class MP3playerActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var playPauseBtn: ImageView
    private lateinit var pervBtn: ImageView
    private lateinit var nextBtn: ImageView
    private lateinit var seekBar: AppCompatSeekBar
    private lateinit var elapsedTimeLabel: TextView
    private lateinit var remainingTimeLabel: TextView
    private lateinit var titleTextView: TextView

    private var isPlaying: Boolean = false
    private var mp3FilePath: String? = null
    private var mp3FileTitle: String? = null
    private lateinit var mp3Files: ArrayList<MP3FileData>
    private var currentIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this, theme)
        setContentView(R.layout.activity_mp3player)

        // Initialize views
        playPauseBtn = findViewById(R.id.playPauseBtn)
        pervBtn = findViewById(R.id.pervBtn)
        nextBtn = findViewById(R.id.nextBtn)
        seekBar = findViewById(R.id.seekBar)
        elapsedTimeLabel = findViewById(R.id.elapsedTimeLabel)
        remainingTimeLabel = findViewById(R.id.remainingTimeLabel)
        titleTextView = findViewById(R.id.titleTextView)

        // Retrieve data from the intent
        mp3Files = intent.getSerializableExtra("mp3Files") as ArrayList<MP3FileData>
        currentIndex = intent.getIntExtra("currentIndex", 0)

        playMP3File(mp3Files[currentIndex])

        // Next button listener
        nextBtn.setOnClickListener {
            if (currentIndex < mp3Files.size - 1) {
                currentIndex++
                playMP3File(mp3Files[currentIndex])
            }
        }

        // Previous button listener
        pervBtn.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                playMP3File(mp3Files[currentIndex])
            }
        }

        // Play/Pause button listener
        playPauseBtn.setOnClickListener {
            if (isPlaying) {
                pauseAudio()
            } else {
                playAudio()
            }
        }

        // Set up the SeekBar and its listener
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                    updateTimeLabels()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Update time labels initially
        updateTimeLabels()
    }

    private fun playMP3File(mp3FileData: MP3FileData) {
        mp3FilePath = mp3FileData.path
        mp3FileTitle = mp3FileData.title

        // Set the title of the MP3 file
        titleTextView.text = mp3FileTitle

        // Initialize MediaPlayer
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.reset()
        } else {
            mediaPlayer = MediaPlayer()
        }

        try {
            mediaPlayer.setDataSource(mp3FilePath)
            mediaPlayer.prepare()
            seekBar.max = mediaPlayer.duration
            playAudio()

            // Set OnCompletionListener to play the next file automatically
            mediaPlayer.setOnCompletionListener {
                if (currentIndex < mp3Files.size - 1) {
                    currentIndex++
                    playMP3File(mp3Files[currentIndex])
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playAudio() {
        mediaPlayer.start()
        isPlaying = true
        playPauseBtn.setImageResource(R.drawable.round_pause_circle_outline_24) // Change to pause icon
        updateSeekBar()
    }

    private fun pauseAudio() {
        mediaPlayer.pause()
        isPlaying = false
        playPauseBtn.setImageResource(R.drawable.round_play_circle_outline_24) // Change to play icon
    }

    private fun updateSeekBar() {
        seekBar.progress = mediaPlayer.currentPosition
        if (isPlaying) {
            seekBar.postDelayed({ updateSeekBar() }, 1000)
        }
        updateTimeLabels()
    }

    private fun updateTimeLabels() {
        val totalDuration = mediaPlayer.duration / 1000  // Convert to seconds
        elapsedTimeLabel.text = DateUtils.formatElapsedTime(totalDuration.toLong())

        val currentProgress = mediaPlayer.currentPosition / 1000  // Convert to seconds
        remainingTimeLabel.text = DateUtils.formatElapsedTime(currentProgress.toLong())
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}

