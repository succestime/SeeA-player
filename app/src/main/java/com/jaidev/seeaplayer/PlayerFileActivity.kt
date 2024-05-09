package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bullhead.equalizer.EqualizerFragment
import com.bullhead.equalizer.Settings
import com.developer.filepicker.model.DialogConfigs
import com.developer.filepicker.model.DialogProperties
import com.developer.filepicker.view.FilePickerDialog
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.allAdapters.PlaybackIconsAdapter
import com.jaidev.seeaplayer.dataClass.IconModel
import com.jaidev.seeaplayer.databinding.ActivityPlayerFileBinding
import com.jaidev.seeaplayer.databinding.BoosterBinding
import com.jaidev.seeaplayer.databinding.SpeedDialogBinding
import java.io.File
import java.text.DecimalFormat
import java.util.Timer
import java.util.TimerTask
import kotlin.system.exitProcess

class PlayerFileActivity : AppCompatActivity() , GestureDetector.OnGestureListener, AudioManager.OnAudioFocusChangeListener {
    private lateinit var playerView: PlayerView
    private lateinit var videoTitleTextView: TextView
    private lateinit var playPauseBtn: ImageButton
    private lateinit var player: ExoPlayer
    private val iconModelArrayList = ArrayList<IconModel>()
    private lateinit var playbackIconsAdapter: PlaybackIconsAdapter
    private lateinit var recyclerViewIcons: RecyclerView
    var expand = false
    var nightMode: View? = null
    var dark: Boolean = false
    var mute: Boolean = false
    private var speed: Float = 1.0f
    private var timer: Timer? = null
    private lateinit var loudnessEnhancer: LoudnessEnhancer
    lateinit var dialogProperties: DialogProperties
    lateinit var filePickerDialog: FilePickerDialog
    lateinit var uriSubtitle: Uri
    private lateinit var eqContainer: FrameLayout
    private lateinit var fullScreenBtn: ImageButton
    private var videoTitle: String? = null
    private var isLocked: Boolean = false
    private var isPlaying: Boolean = false
    private var repeat: Boolean = false
    private lateinit var binding: ActivityPlayerFileBinding
    private var isFullscreen: Boolean = false
    private lateinit var gestureDetectorCompat: GestureDetectorCompat
    private var videoUriList: ArrayList<Uri>? = null
    private var videoTitleList: ArrayList<String>? = null
    private var currentIndex: Int = 0
    private var isPlayingBeforePause = false
    @SuppressLint("MissingInflatedId", "ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityPlayerFileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        videoUriList = intent.getParcelableArrayListExtra("videoUriList")
        videoTitleList = intent.getStringArrayListExtra("videoTitleList")

        gestureDetectorCompat = GestureDetectorCompat(this, this)

        // Hide the status bar (system UI)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }
        // Get the video URI and title from the intent extras
        val videoUriString = intent.getStringExtra("videoUri")
        val videoUri = Uri.parse(videoUriString)
        videoTitle = intent.getStringExtra("videoTitle")

        // Initialize PlayerView
        playerView = findViewById(R.id.playerView)

        // Initialize ExoPlayer
        player = SimpleExoPlayer.Builder(this).build()
        playerView.player = player

        // Set up MediaItem
        val mediaItem = MediaItem.fromUri(videoUri)
        player.setMediaItem(mediaItem)
        player.prepare()
player.play()
        // Display video title
        videoTitleTextView = findViewById(R.id.videoTitle)
        videoTitleTextView.text = videoTitle

        // Initialize play/pause button
        playPauseBtn = findViewById(R.id.playPauseBtn)
        playPauseBtn.setImageResource(R.drawable.round_pause_24) // Set initial icon to pause
        playPauseBtn.setOnClickListener {
            if (isPlaying) {
                pauseVideo()
            } else {
                playVideo()
            }
            isPlaying = !isPlaying

        }

        // Initialize back 10 seconds button
        findViewById<ImageButton>(R.id.back10secondBtn).setOnClickListener {
            player.seekTo(player.currentPosition.minus(10000) ?: 0)
        }

        // Initialize forward 10 seconds button
        findViewById<ImageButton>(R.id.forward10secondBtn).setOnClickListener {
            player.seekTo(player.currentPosition.plus(10000) ?: 0)
        }
        findViewById<ImageButton>(R.id.backBtn).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.nextBtn).setOnClickListener {
            playNextVideo()
        }

        findViewById<ImageButton>(R.id.prevBtn).setOnClickListener {
            playPreviousVideo()
        }

        binding.lockButton.setOnClickListener {
            if (!isLocked) {
                // for hiding
                isLocked = true
                binding.playerView.hideController()
                binding.playerView.useController = false
                binding.lockButton.setImageResource(R.drawable.round_lock)
            } else {
                // for showing
                isLocked = false
                binding.playerView.useController = true
                binding.playerView.showController()

                binding.lockButton.setImageResource(R.drawable.round_lock_open)
            }
        }

        fullScreenBtn = findViewById(R.id.fullScreenBtn)
        fullScreenBtn.setOnClickListener {
            toggleFullscreen()
        }

        findViewById<ImageButton>(R.id.repeatBtn).setOnClickListener {
            if (repeat) {
                repeat = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                findViewById<ImageButton>(R.id.repeatBtn).setImageResource(R.drawable.round_repeat)
            } else {
                repeat = true
                player.repeatMode = Player.REPEAT_MODE_ONE
                findViewById<ImageButton>(R.id.repeatBtn).setImageResource(R.drawable.round_repeat_on)
            }
        }
        // Play video in fullscreen mode initially
        playInFullscreen(enable = false)
        doubleTapEnable()
        dialogProperties = DialogProperties()
        filePickerDialog = FilePickerDialog(this@PlayerFileActivity)
        filePickerDialog.setTitle("Select a Subtitle File")
        filePickerDialog.setPositiveBtnName("OK")
        filePickerDialog.setNegativeBtnName("Cancel")

        horizontalIconList()


    }


    @SuppressLint("NotifyDataSetChanged")
    private fun horizontalIconList() {
        iconModelArrayList.add(IconModel(R.drawable.round_navigate_next,"", android.R.color.white))
        iconModelArrayList.add(IconModel(R.drawable.round_nights_stay,"Night Mode", android.R.color.white))
        iconModelArrayList.add(IconModel(R.drawable.round_speed,"Speed", android.R.color.white))
        iconModelArrayList.add(IconModel(R.drawable.round_screen_rotation,"Rotate", android.R.color.white))
        iconModelArrayList.add(IconModel(R.drawable.round_volume_off,"Rotate", android.R.color.white))
        nightMode = findViewById(R.id.night_mode)
        recyclerViewIcons = findViewById(R.id.horizontalRecyclerview)
        eqContainer = findViewById<FrameLayout>(R.id.eqFrame)


        playbackIconsAdapter = PlaybackIconsAdapter(iconModelArrayList, this)
        val layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, true)
        recyclerViewIcons.layoutManager = layoutManager
        recyclerViewIcons.adapter = playbackIconsAdapter
        playbackIconsAdapter.notifyDataSetChanged()

        playbackIconsAdapter.setOnItemClickListener(object : PlaybackIconsAdapter.OnItemClickListener {
            @SuppressLint("Range", "SourceLockedOrientationActivity")
            override fun onItemClick(position: Int) {
                when (position) {
                    0 -> {
                        if (expand) {
                            iconModelArrayList.clear()
                            iconModelArrayList.add(
                                IconModel(
                                    R.drawable.round_navigate_next,
                                    "",
                                    android.R.color.white
                                )
                            )
                            iconModelArrayList.add(
                                IconModel(
                                    R.drawable.round_nights_stay,
                                    "Night Mode",
                                    android.R.color.white
                                )
                            )
                            iconModelArrayList.add(
                                IconModel(
                                    R.drawable.round_speed,

                                    "Speed",
                                    android.R.color.white
                                )
                            )
                            iconModelArrayList.add(
                                IconModel(
                                    R.drawable.round_screen_rotation,
                                    "Rotate",
                                    android.R.color.white
                                )
                            )
                            iconModelArrayList.add(
                                IconModel(
                                    R.drawable.round_volume_off,
                                    "Mute",
                                    android.R.color.white
                                )
                            )
                            playbackIconsAdapter.notifyDataSetChanged()
                            expand = false
                        } else {

                            if (iconModelArrayList.size == 5) {
                                iconModelArrayList.add(
                                    IconModel(
                                        R.drawable.round_sleep_timer,
                                        "Sleep Timer",
                                        android.R.color.white
                                    )
                                )

                                iconModelArrayList.add(
                                    IconModel(
                                        R.drawable.round_subtitles,
                                        "Subtitle",
                                        android.R.color.white
                                    )
                                )
                                iconModelArrayList.add(
                                    IconModel(
                                        R.drawable.round_graphic_eq,
                                        "Equalizer",
                                        android.R.color.white
                                    )
                                )


                            }
                            iconModelArrayList[position] = IconModel(R.drawable.round_back, "")
                            playbackIconsAdapter.notifyDataSetChanged()
                            expand = true
                        }
                    }

                    1 -> {
                        if (dark) {
                            nightMode?.visibility = View.GONE
                            iconModelArrayList[position] = IconModel(R.drawable.round_nights_stay, "Night")
                            playbackIconsAdapter.notifyDataSetChanged()
                            dark = false
                        } else {
                            nightMode?.visibility = View.VISIBLE
                            iconModelArrayList[position] = IconModel(R.drawable.round_nights_stay, "Day")
                            playbackIconsAdapter.notifyDataSetChanged()
                            dark = true
                        }

                    }

                    2 -> {
                        setupSpeedDialog()
                    }
                    3 -> {
                        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            playbackIconsAdapter.notifyDataSetChanged()
                            findViewById<ImageButton>(R.id.back10secondBtn).visibility = View.VISIBLE
                            findViewById<ImageButton>(R.id.forward10secondBtn).visibility = View.VISIBLE

                        } else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            playbackIconsAdapter.notifyDataSetChanged()

                            findViewById<ImageButton>(R.id.back10secondBtn).visibility = View.GONE
                            findViewById<ImageButton>(R.id.forward10secondBtn).visibility = View.GONE
                        }
                    }

                   4 ->{     setupSleepTimer()   }

                    5 -> {
                        if (mute) {
                            player.setVolume(100F)
                            iconModelArrayList[position] = IconModel(R.drawable.round_volume_off, "Mute")
                            playbackIconsAdapter.notifyDataSetChanged()
                            mute = false
                        } else {
                            player.setVolume(0F)
                            iconModelArrayList[position] =
                                IconModel(R.drawable.round_volume_up, "Unmute")
                            playbackIconsAdapter.notifyDataSetChanged()
                            mute = true
                        }
                    }

                   6 -> {
                        dialogProperties.selection_mode = DialogConfigs.SINGLE_MODE
                        dialogProperties.extensions = arrayOf(".srt")
                        dialogProperties.root = File("/storage/emulated/0")

                        filePickerDialog.setDialogSelectionListener { files ->
                            for (path in files) {
                                val file = File(path)
                                uriSubtitle = Uri.parse(file.absolutePath)
                                // Further actions after selecting a subtitle file
                            }
                        }

                        filePickerDialog.properties = dialogProperties
                        filePickerDialog.show()
                    }

                    7 -> {
                        if (eqContainer.visibility == View.GONE) {
                            eqContainer.visibility = View.VISIBLE
                        }
                        val sessionId = player.audioSessionId
                        Settings.isEditing = false
                        val equalizerFragment = EqualizerFragment.newBuilder()
                            .setAccentColor(Color.parseColor("#4285F4"))
                            .setAudioSessionId(sessionId)
                            .build()

                        supportFragmentManager.beginTransaction()
                            .replace(R.id.eqFrame, equalizerFragment)
                            .commit()

                        playbackIconsAdapter.notifyDataSetChanged()
                    }


                    else -> {
                        // Handle any other positions if needed
                    }
                }
            }
        })

    }

    private fun pauseVideo() {
        playPauseBtn.setImageResource(R.drawable.round_play)
        player.pause()
    }

    private fun playVideo() {
        playPauseBtn.setImageResource(R.drawable.round_pause_24)
        player.play()
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        playInFullscreen(enable = isFullscreen)
    }

    private fun playInFullscreen(enable: Boolean) {
        if (enable) {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            findViewById<ImageButton>(R.id.fullScreenBtn).setImageResource(R.drawable.round_halfscreen)
        } else {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            findViewById<ImageButton>(R.id.fullScreenBtn).setImageResource(R.drawable.round_fullscreen)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun doubleTapEnable() {
        binding.playerView.player = player
        binding.ytOverlay.performListener(object : YouTubeOverlay.PerformListener {
            override fun onAnimationEnd() {
                binding.ytOverlay.visibility = View.GONE
            }

            override fun onAnimationStart() {
                binding.ytOverlay.visibility = View.VISIBLE
            }
        })
        binding.ytOverlay.player(player)

        binding.playerView.setOnTouchListener { _, motionEvent ->
            binding.playerView.isDoubleTapEnabled = false

            if (!isLocked) {
                binding.playerView.isDoubleTapEnabled = true
                gestureDetectorCompat.onTouchEvent(motionEvent)

                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    // for immersive mode
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    WindowInsetsControllerCompat(window, binding.root).let { controller ->
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            }




            return@setOnTouchListener false
        }
    }

    override fun onDown(p0: MotionEvent): Boolean = false
    override fun onShowPress(p0: MotionEvent) = Unit
    override fun onSingleTapUp(p0: MotionEvent): Boolean = false
    override fun onLongPress(p0: MotionEvent) = Unit
    override fun onFling(
        p0: MotionEvent?,
        p1: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean = false

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean = false


    private fun playNextVideo() {
        if (videoUriList != null && currentIndex < videoUriList!!.size - 1) {
            currentIndex++
        } else if (videoUriList != null && videoUriList!!.isNotEmpty()) {
            // If currentIndex is already at the last video, loop back to the first video
            currentIndex = 0
        }
        playVideoAtCurrentIndex()
    }

    private fun playPreviousVideo() {
        if (videoUriList != null && currentIndex > 0) {
            currentIndex--
        } else if (videoUriList != null && videoUriList!!.isNotEmpty()) {
            // If currentIndex is already at the first video, loop to the last video
            currentIndex = videoUriList!!.size - 1
        }
        playVideoAtCurrentIndex()
    }


    private fun playVideoAtCurrentIndex() {
        if (videoUriList != null && currentIndex in videoUriList!!.indices) {
            val videoUri = videoUriList!![currentIndex]
            val videoTitle = videoTitleList!![currentIndex]

            // Check if the player is already playing the same video
            val currentMediaItem = player.currentMediaItem
            if (currentMediaItem != null && currentMediaItem.mediaId == videoUri.toString()) {
                // If the player is already playing the same video, seek to the beginning and play
                player.seekTo(0)
            } else {
                // Update player with new media item
                val mediaItem = MediaItem.fromUri(videoUri)
                player.setMediaItem(mediaItem)
                player.prepare()
                // Set the video title
                videoTitleTextView.text = videoTitle
            }
            // Always play from the beginning
            player.play()

        } else {
            Toast.makeText(this, "There are no videos to play", Toast.LENGTH_SHORT).show()

            // Handle the case where either videoUriList is null or currentIndex is out of bounds
            // For example, you might display a toast message or perform some other action
        }
    }
//    private fun releasePlayer() {
//        player.release()
//        // You may need to release other player resources here
//    }
    override fun onDestroy() {
        super.onDestroy()
        player.pause()
        player.release()


    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange <= 0) pauseVideo()
    }
    override fun onResume() {
        super.onResume()
        if (isPlayingBeforePause) {
            player.play()
        }
    }
    override fun onPause() {
        super.onPause()
        if (player.isPlaying) {
            isPlayingBeforePause = true
            player.pause()

        } else {

            isPlayingBeforePause = false
        }
    }
    @SuppressLint("SetTextI18n")
    fun setupSpeedDialog() {
//        dialog.dismiss()
        playVideo()
        val customDialogS =
            LayoutInflater.from(this).inflate(R.layout.speed_dialog, binding.root, false)
        val bindingS = SpeedDialogBinding.bind(customDialogS)
        val dialogS = MaterialAlertDialogBuilder(this).setView(customDialogS)
            .setOnCancelListener { playVideo() }
            .setPositiveButton("Done") { _, _ ->
//                dialog.dismiss()
            }
            .setBackground(getSemiTransparentGrayDrawable())
            .create()
        dialogS.show()
        bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
        bindingS.minusBtn.setOnClickListener {
            changeSpeed(isIncrement = false)
            bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
        }
        bindingS.plusBtn.setOnClickListener {
            changeSpeed(isIncrement = true)
            bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
        }
    }

    @SuppressLint("SetTextI18n")
    fun setupSleepTimer() {
        if (timer != null)
            Toast.makeText(
                this@PlayerFileActivity,
                "Timer Already Running !\nClose App to Reset Timer ..",
                Toast.LENGTH_SHORT
            ).show()
        else {
            var sleepTime = 15
            val customDialogS = LayoutInflater.from(this@PlayerFileActivity)
                .inflate(R.layout.speed_dialog, binding.root, false)
            val bindingS = SpeedDialogBinding.bind(customDialogS)
            val dialogS = MaterialAlertDialogBuilder(this@PlayerFileActivity).setView(customDialogS)
                .setOnCancelListener { playVideo() }
                .setPositiveButton("Done") { self, _ ->
                    Toast.makeText(this@PlayerFileActivity, "Sleep Timer is start", Toast.LENGTH_SHORT).show()
                    timer = Timer()
                    val task = object : TimerTask() {
                        override fun run() {
                            moveTaskToBack(true)
                            exitProcess(1)
                        }
                    }

                   timer!!.schedule(task, sleepTime * 60 * 1000.toLong())
                    self.dismiss()
                    playVideo()
                }
                .setBackground(getSemiTransparentGrayDrawable())
                .create()
            dialogS.show()
            bindingS.speedText.text = "$sleepTime Min"
            bindingS.minusBtn.setOnClickListener {
                if (sleepTime > 15) sleepTime -= 15
                bindingS.speedText.text = "$sleepTime Min"
            }
            bindingS.plusBtn.setOnClickListener {
                if (sleepTime < 1000) sleepTime += 15
                bindingS.speedText.text = "$sleepTime Min"
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun setupBoosterDialog() {
        // dialog.dismiss()
        val customDialogB =
            LayoutInflater.from(this).inflate(R.layout.booster, binding.root, false)
        val bindingB = BoosterBinding.bind(customDialogB)
        val dialogB = MaterialAlertDialogBuilder(this).setView(customDialogB)
            .setOnCancelListener { playVideo() }
            .setPositiveButton("Done") { _, _ ->
              loudnessEnhancer.setTargetGain(bindingB.verticalBar.progress * 100)
                playVideo()
                // dialog.dismiss()
            }
            .setBackground(getSemiTransparentGrayDrawable())
            .create()
        dialogB.show()
        bindingB.verticalBar.progress = loudnessEnhancer.targetGain.toInt() / 100
        bindingB.progressText.text =
            "Audio Booster\n\n${loudnessEnhancer.targetGain.toInt() / 10}"
        bindingB.verticalBar.setOnProgressChangeListener {
            bindingB.progressText.text = "Audio Booster\n\n${it * 10}"
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    fun setupPIPMode() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                android.os.Process.myUid(),
                packageName
            ) == AppOpsManager.MODE_ALLOWED
        } else {
            false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (status) {
                this.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                // dialog.dismiss()
                binding.playerView.showController()
                playVideo()
                PlayerActivity.pipStatus = 0
            } else {
                val intent = Intent(
                    "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        } else {
            Toast.makeText(this, "Feature Not Supported!!", Toast.LENGTH_SHORT).show()
            // dialog.dismiss()
            playVideo()
        }
    }
    private fun changeSpeed(isIncrement: Boolean) {
        if (isIncrement) {
            if (speed < 2.9f) {
                speed += 0.10f  // speed = speed + 0.10f
            }
        } else {
            if (speed > 0.20f) {
                speed -= 0.10f    // speed = speed - 0.10f
            }
        }
        player.setPlaybackSpeed(speed)
    }

    private fun getSemiTransparentGrayDrawable(): ColorDrawable {
        val color = Color.parseColor("#011B29")
        return ColorDrawable(color)
    }
}
