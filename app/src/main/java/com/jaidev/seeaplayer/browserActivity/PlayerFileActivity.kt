package com.jaidev.seeaplayer.browserActivity

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bullhead.equalizer.EqualizerFragment
import com.bullhead.equalizer.Settings
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
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.PlaybackIconsAdapter
import com.jaidev.seeaplayer.dataClass.IconModel
import com.jaidev.seeaplayer.databinding.ActivityPlayerFileBinding
import com.jaidev.seeaplayer.databinding.BoosterBinding
import com.jaidev.seeaplayer.databinding.SpeedDialogBinding
import java.io.File
import java.text.DecimalFormat
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import kotlin.math.abs
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
    private var initialSpeed: Float = 1.0f

    private var timer: Timer? = null
    lateinit var dialogProperties: DialogProperties
    lateinit var filePickerDialog: FilePickerDialog
    private lateinit var eqContainer: FrameLayout
    private lateinit var fullScreenBtn: ImageButton
    private var videoTitle: String? = null
    private var isLocked: Boolean = false
    private var repeat: Boolean = false
    private var audioManager: AudioManager? = null
    private lateinit var binding: ActivityPlayerFileBinding
    private var isFullscreen: Boolean = false
    private lateinit var gestureDetectorCompat: GestureDetectorCompat
    private var videoUriList: ArrayList<Uri>? = null
    private var videoTitleList: ArrayList<String>? = null
    private var currentIndex: Int = 0
    private var isPlayingBeforePause = false
    private var isSwipingToChangeDuration = false
    private var isSwipingForward = false
    private var currentSwipeX = 0f
    private var currentSwipeY = 0f
    private var initialPosition = 0L
    private lateinit var durationChangeTextView: TextView
    private var currentProgress: Int = 0
    lateinit var mAdView: AdView
    private lateinit var startLinkTubeActivityLauncher: ActivityResultLauncher<Intent>
    private var isSleepTimerRunning = false
    private var isBoosterRunning = false
    private var isSpeedRunning = false
    private var sleepTimerPosition = -1
    private var boosterPosition = -1
    private var speedPosition = -1
    companion object {
        private var brightness: Int = 0
        private var volume: Int = 0
        var pipStatus: Int = 0
        private const val MAX_DURATION_CHANGE =
            180 * 1000L // Maximum duration change in milliseconds
        private const val SWIPE_THRESHOLD = 50 // Swipe threshold in pixels
        private const val MAX_PROGRESS = 100
        private lateinit var loudnessEnhancer: LoudnessEnhancer
    }
    private val videoHistory = mutableListOf<Pair<Uri, String>>()
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingInflatedId", "ObsoleteSdkInt", "NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Clear the FLAG_FULLSCREEN flag to show the status bar
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        // Make the status bar transparent
        window.statusBarColor = Color.BLACK
        // Hide the action bar if you have one
        supportActionBar?.hide()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityPlayerFileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.adView)

        gestureDetectorCompat = GestureDetectorCompat(this, this)

        // for immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                // Check if the intent action is ACTION_VIEW for shared video
                intent.data?.let { videoUri ->
                    videoTitle = getVideoTitle(videoUri) ?: "Open with Video"
                    initializePlayer(videoUri, videoTitle)
                }
            }
            Intent.ACTION_SEND -> {
                // Check if the intent action is ACTION_SEND for shared video
                (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { videoUri ->
                    videoTitle = getVideoTitle(videoUri) ?: "Shared Video"
                    initializePlayer(videoUri, videoTitle)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                // Handle multiple URIs
                val videoUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                videoUriList = videoUris?.filterNotNull() as ArrayList<Uri>?
                videoTitleList = videoUriList?.map { getVideoTitle(it) ?: "Shared Video" } as ArrayList<String>?
                currentIndex = -1
                if (videoUriList.isNullOrEmpty().not()) {
                    playNextVideo()
                } else {
                    Toast.makeText(this, "No Videos to Play", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                // Get the video URI and title from the intent extras
                val videoUriString = intent.getStringExtra("videoUri")
                val videoUri = Uri.parse(videoUriString)
                videoTitle = intent.getStringExtra("videoTitle")
                initializePlayer(videoUri, videoTitle)
            }
        }


        binding.playerView.setOnApplyWindowInsetsListener { view, insets ->
            val systemWindowInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                0,
                systemWindowInsets.top,
                systemWindowInsets.right,
                systemWindowInsets.bottom
            )
            insets
        }
        startLinkTubeActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // This block will be called when the launched activity is finished
            binding.progress.visibility = View.GONE
        }
    }

    private fun playNextVideo() {
        if (videoUriList != null && videoUriList!!.isNotEmpty() && currentIndex < videoUriList!!.size - 1) {
            currentIndex++
            val nextUri = videoUriList!![currentIndex]
            val nextTitle = videoTitleList!![currentIndex]
            initializePlayer(nextUri, nextTitle)
        } else {
            Toast.makeText(this, "No Next Video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playPreviousVideo() {
        if (currentIndex > 0) {
            currentIndex--
            val previousUri = videoUriList!![currentIndex]
            val previousTitle = videoTitleList!![currentIndex]
            initializePlayer(previousUri, previousTitle)
        } else {
            Toast.makeText(this, "No Previous Video", Toast.LENGTH_SHORT).show()
        }
    }



    private fun getVideoTitle(uri: Uri): String? {
        var title: String? = null
        if (uri.scheme == "content") {
            val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val titleIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    title = it.getString(titleIndex)
                }
            }
        } else if (uri.scheme == "file") {
            title = File(uri.path!!).name
        }
        return title
    }
    private fun initializePlayer(videoUri: Uri, videoTitle: String?) {
        // Initialize PlayerView
        playerView = findViewById(R.id.playerView)

        // Check if the player is already initialized
        if (!::player.isInitialized) {
            // Initialize ExoPlayer
            player = SimpleExoPlayer.Builder(this).build()
            playerView.player = player

            // Initialize play/pause button and other UI elements here
            setupPlayerControls()
        }

        // Set the media item
        val mediaItem = MediaItem.fromUri(videoUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        // Update video title
        videoTitleTextView = findViewById(R.id.videoTitle)
        videoTitleTextView.text = videoTitle

        // Update the currentIndex based on the URI
        currentIndex = videoUriList?.indexOf(videoUri) ?: 0
    }

    private fun setupPlayerControls() {
        // Initialize play/pause button
        playPauseBtn = findViewById(R.id.playPauseBtn)
        playPauseBtn.setImageResource(R.drawable.round_pause_24) // Set initial icon to pause
        playPauseBtn.setOnClickListener {
            if (player.isPlaying) {
                val adRequest = AdRequest.Builder().build()
                mAdView.loadAd(adRequest)
                // Check if the banner ad is loaded
                mAdView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        binding.adsLayout.visibility = View.VISIBLE
                    }
                }
                pauseVideo()
            } else {
                // If the banner ad is not loaded, hide the ads layout
                if (!mAdView.isLoading) {
                    binding.adsLayout.visibility = View.GONE
                }
                playVideo()
            }
        }

        binding.adsRemove.setOnClickListener {
            binding.adsLayout.visibility = View.GONE
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
        // Initialize next button
        findViewById<ImageButton>(R.id.nextBtn).setOnClickListener {
            if (repeat) {
                Toast.makeText(this, "Repeat mode is on", Toast.LENGTH_SHORT).show()
                // If repeat mode is enabled, do nothing and call the functionality
                // This will not play the previous video
                // You can add a message or just leave it empty
            } else {
                playNextVideo()
                findViewById<ImageButton>(R.id.playPauseBtn).setImageResource(R.drawable.round_pause_24)

            }
        }

        // Initialize previous button
        findViewById<ImageButton>(R.id.prevBtn).setOnClickListener {
            if (repeat) {
                Toast.makeText(this, "Repeat mode is on", Toast.LENGTH_SHORT).show()
                // If repeat mode is enabled, do nothing and call the functionality
                // This will not play the previous video
                // You can add a message or just leave it empty
            } else {
                playPreviousVideo()
                findViewById<ImageButton>(R.id.playPauseBtn).setImageResource(R.drawable.round_pause_24)
            }

        }
        val lockBtn = findViewById<ImageButton>(R.id.openButton)

        lockBtn.setOnClickListener {
            if (!isLocked) {
                // For hiding
                isLocked = true
                binding.playerView.useController = false
                binding.playerView.isDoubleTapEnabled = false
                binding.playerView.hideController()
                binding.lockButton.visibility = View.VISIBLE
                lockBtn.visibility = View.GONE
                Handler().postDelayed({
                    binding.lockButton.visibility = View.INVISIBLE
                }, 2000)
            } else {
                // For showing
                isLocked = false
                binding.playerView.useController = true
                binding.playerView.showController()
                binding.lockButton.visibility = View.GONE
                lockBtn.visibility = View.VISIBLE
            }
        }
        binding.lockButton.setOnClickListener {
            if (!isLocked) {
                // For hiding
                isLocked = true
                binding.playerView.useController = false
                binding.playerView.isDoubleTapEnabled = false
                binding.playerView.hideController()
                binding.lockButton.visibility = View.VISIBLE
                lockBtn.visibility = View.GONE
                Handler().postDelayed({
                    binding.lockButton.visibility = View.INVISIBLE
                }, 2000)
            } else {
                // For showing
                isLocked = false
                binding.playerView.useController = true
                binding.playerView.showController()
                binding.lockButton.visibility = View.GONE
                lockBtn.visibility = View.VISIBLE
            }
        }
        binding.playerView.setOnClickListener {
            // Show lock button if locked when touched
            if (isLocked) {
                binding.lockButton.visibility = View.VISIBLE
                // Schedule to hide lock button after 2 seconds
                Handler().postDelayed({
                    binding.lockButton.visibility = View.INVISIBLE
                }, 2000)
            } else {
                binding.playerView.isDoubleTapEnabled = true
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
        durationChangeTextView = findViewById(R.id.durationChangeTextView)
        durationChangeTextView.visibility = View.GONE
        loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
        loudnessEnhancer.enabled = true
        horizontalIconList()
        setupSwipeGesture()

        binding.playerView.setControllerVisibilityListener { visibility ->
            val lockBtn = findViewById<ImageButton>(R.id.openButton)

            // Check if the screen orientation is portrait
            val isPortrait =
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

            if (isPortrait) {
                lockBtn.visibility = View.GONE
            } else {
                if (isLocked) {
                    lockBtn.visibility = View.VISIBLE
                } else {
                    lockBtn.visibility =
                        if (binding.playerView.isControllerVisible) View.VISIBLE else View.INVISIBLE
                }
            }
            // Show or hide the status bar based on playerView visibility
            if (binding.playerView.isControllerVisible) {
                showStatusBar()
            } else {
                hideStatusBar()
            }
        }
        // Add listener to handle playback state changes
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    if (videoUriList.isNullOrEmpty()) {
                        finish() // Finish the activity if no more videos are left
                    } else {
                        playNextVideo() // Play the next video if available
                    }
                }
            }
        })

    }

    private fun showStatusBar() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        binding.playerView.showController() // Show player controller
    }

    private fun hideStatusBar() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        binding.playerView.hideController() // Hide player controller
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun horizontalIconList() {
        iconModelArrayList.add(IconModel(R.drawable.round_navigate_next,"", android.R.color.white))
        iconModelArrayList.add(IconModel(R.drawable.round_nights_stay,"Night Mode", android.R.color.white))
        iconModelArrayList.add(IconModel(R.drawable.round_speed,"Speed", android.R.color.white))
        iconModelArrayList.add(IconModel(R.drawable.round_screen_rotation,"Rotate", android.R.color.white))
        iconModelArrayList.add(IconModel(R.drawable.round_volume_off,"Mute", android.R.color.white))
        nightMode = findViewById(R.id.night_mode)
        recyclerViewIcons = findViewById(R.id.horizontalRecyclerview)
        eqContainer = findViewById<FrameLayout>(R.id.eqFrame)


        playbackIconsAdapter = PlaybackIconsAdapter(iconModelArrayList, this , dark)
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
                                        R.drawable.search_link_tube,
                                        "Link Tube",
                                        android.R.color.white
                                    )
                                )
                                iconModelArrayList.add(
                                    IconModel(
                                        R.drawable.round_speaker,
                                        "Booster",
                                        android.R.color.white
                                    )
                                )
                                iconModelArrayList.add(
                                    IconModel(
                                        R.drawable.round_sleep_timer,
                                        "Sleep Timer",
                                        android.R.color.white
                                    )
                                )
                                iconModelArrayList.add(
                                    IconModel(
                                        R.drawable.round_picture_in_picture_alt,
                                        "PIP Mode",
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
                            iconModelArrayList[position] = IconModel(R.drawable.round_nights_stay, "Night Mode" ,    iconBackground = R.drawable.ripple_circle)
                            playbackIconsAdapter.notifyDataSetChanged()
                            dark = false
                        } else {
                            nightMode?.visibility = View.VISIBLE
                            iconModelArrayList[position] = IconModel(R.drawable.round_nights_stay, "Day Mode",    iconBackground = R.drawable.ripple_circle_cool_blue)
                            playbackIconsAdapter.notifyDataSetChanged()
                            dark = true
                        }

                    }

                    2 -> {
                        speedPosition = position
                        setupSpeedDialog()
                    }
                    3 -> {
                        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            playbackIconsAdapter.notifyDataSetChanged()
                            findViewById<ImageButton>(R.id.fullScreenBtn).visibility = View.VISIBLE
                            findViewById<ImageButton>(R.id.repeatBtn).visibility = View.VISIBLE
                            findViewById<ImageButton>(R.id.openButton).visibility = View.VISIBLE

                        } else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            playbackIconsAdapter.notifyDataSetChanged()
                            findViewById<ImageButton>(R.id.fullScreenBtn).visibility = View.GONE
                            findViewById<ImageButton>(R.id.repeatBtn).visibility = View.GONE
                            findViewById<ImageButton>(R.id.openButton).visibility = View.GONE
                        }
                    }

                    4 ->{
                        if (mute) {
                            player.setVolume(100F)
                            iconModelArrayList[position] = IconModel(R.drawable.round_volume_off, "Mute" ,    iconBackground = R.drawable.ripple_circle)
                            playbackIconsAdapter.notifyDataSetChanged()
                            mute = false
                        } else {
                            player.setVolume(0F)
                            iconModelArrayList[position] =
                                IconModel(R.drawable.round_volume_up, "Mute" ,    iconBackground = R.drawable.ripple_circle_cool_blue)
                            playbackIconsAdapter.notifyDataSetChanged()
                            mute = true
                        }
                    }

                    5 -> {
                        binding.progress.visibility = View.VISIBLE // Show progress
                        Handler(Looper.getMainLooper()).postDelayed({
                            // Launch LinkTubeActivity
                            val intent = Intent(this@PlayerFileActivity, LinkTubeActivity::class.java)
                            startLinkTubeActivityLauncher.launch(intent)
                        }, 100) // Delay of 500 milliseconds
                                      }
                    6 -> {
                        boosterPosition = position

                        setupBoosterDialog()
                    }
                    7 -> {
                        sleepTimerPosition = position
                        setupSleepTimer()
                    }

                    8 -> {

                        setupPIPMode()
                    }


                    9 -> {
                        if (eqContainer.visibility == View.GONE) {
                            eqContainer.visibility = View.VISIBLE
                        } else {
                            // Check if the fragment is already added
                            val fragment = supportFragmentManager.findFragmentById(R.id.eqFrame)
                            if (fragment != null) {
                                // Fragment is already added, no need to replace it again
                                return
                            }
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


    @SuppressLint("SetTextI18n")
    fun setupBoosterDialog() {
        // dialog.dismiss()
        val customDialogB =
            LayoutInflater.from(this).inflate(R.layout.booster, binding.root, false)
        val bindingB = BoosterBinding.bind(customDialogB)
        val initialProgress = loudnessEnhancer.targetGain.toInt() / 100
        val dialogB = MaterialAlertDialogBuilder(this).setView(customDialogB)
            .setOnCancelListener { playVideo() }
            .setPositiveButton("Done") { _, _ ->
                val newProgress = bindingB.verticalBar.progress
                if (newProgress != initialProgress || newProgress == 0) {
                    if (newProgress == 0) {
                        isBoosterRunning = false
                        iconModelArrayList[boosterPosition] = IconModel(
                            R.drawable.round_speaker,
                            "Booster",
                            iconBackground = R.drawable.ripple_circle
                        )
                    } else {
                        loudnessEnhancer.setTargetGain(newProgress * 100)
                        isBoosterRunning = true
                        iconModelArrayList[boosterPosition] = IconModel(
                            R.drawable.round_speaker,
                            "Booster",
                            iconBackground = R.drawable.ripple_circle_cool_blue
                        )
                    }
                    playbackIconsAdapter.notifyItemChanged(boosterPosition)
                }
                playVideo()
            }
            .setNegativeButton("Cancel") { self, _ ->
                self.dismiss()
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
                binding.playerView.showContextMenu()
                playVideo()
             pipStatus = 0
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
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        // Handle the case when PIP mode is entered or exited
        if (isInPictureInPictureMode) {
            // Handle the case when entering PIP mode
            playVideo()
            playPauseBtn.setImageResource(R.drawable.round_pause_24)
        } else {
            pauseVideo()
            playPauseBtn.setImageResource(R.drawable.round_pause_24)
        }

        if (pipStatus != 0) {
            recreate()
            playInFullscreen(true)
            val intent = Intent(this, PlayerFileActivity::class.java)
            when (pipStatus) {
                1 -> {
                    intent.putParcelableArrayListExtra("videoUriList", videoUriList)
                    intent.putStringArrayListExtra("videoTitleList", videoTitleList)
                    intent.putExtra("currentIndex", currentIndex)
                }
                2 -> {
                    intent.putParcelableArrayListExtra("videoUriList", videoUriList)
                    intent.putStringArrayListExtra("videoTitleList", videoTitleList)
                    intent.putExtra("currentIndex", currentIndex)
                }
            }
            startActivity(intent)
        }
    }
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isInPictureInPictureMode) {
            finish()
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.eqFrame)
        if (eqContainer.visibility == View.GONE) {
            super.onBackPressed()
        } else {
            if (fragment != null && fragment.isVisible && eqContainer.visibility == View.VISIBLE) {
                eqContainer.visibility = View.GONE
            } else {
                player.release()
                super.onBackPressed()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        player.pause()
        player.release()

        audioManager?.abandonAudioFocus(this)

    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange <= 0) pauseVideo()
    }
    override fun onResume() {
        super.onResume()
        if (isPlayingBeforePause) {
            player.play()
        }
        if (audioManager == null) audioManager =
            getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager!!.requestAudioFocus(
            this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
        )
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
            .setPositiveButton("Done") { self, _ ->
                if (speed != initialSpeed) {
                    if (speed == 1.0f) {
                        isSpeedRunning = false
                        iconModelArrayList[speedPosition] = IconModel(
                            R.drawable.round_speed,
                            "Speed",
                            iconBackground = R.drawable.ripple_circle
                        )
                    } else {
                        isSpeedRunning = true
                        iconModelArrayList[speedPosition] = IconModel(
                            R.drawable.round_speed,
                            "Speed",
                            iconBackground = R.drawable.ripple_circle_cool_blue
                        )
                    }
                    playbackIconsAdapter.notifyItemChanged(speedPosition)
                }
                self.dismiss()
            }
            .setNegativeButton("Cancel") { self, _ ->
                self.dismiss()

            }
            .setBackground(getSemiTransparentGrayDrawable())
            .create()
        dialogS.show()
        bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
        bindingS.minusBtn.setOnClickListener {
            changeSpeed(isIncrement = false)
            bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
            updateSpeedIcon()

        }
        bindingS.plusBtn.setOnClickListener {
            changeSpeed(isIncrement = true)
            bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
            updateSpeedIcon()

        }
    }
    private fun updateSpeedIcon() {
        if (speed == 1.0f) {
            isSpeedRunning = false
            iconModelArrayList[speedPosition] = IconModel(
                R.drawable.round_speed,
                "Speed",
                iconBackground = R.drawable.ripple_circle
            )
        } else {
            isSpeedRunning = true
            iconModelArrayList[speedPosition] = IconModel(
                R.drawable.round_speed,
                "Speed",
                iconBackground = R.drawable.ripple_circle_cool_blue
            )
        }
        playbackIconsAdapter.notifyItemChanged(speedPosition)
    }
    @SuppressLint("SetTextI18n")
    fun setupSleepTimer() {
        if (timer != null)
        // Show an alert dialog to ask the user if they want to stop the timer
            MaterialAlertDialogBuilder(this@PlayerFileActivity)
                .setTitle("Do you want to stop the timer?")
                .setMessage("If you want to stop the sleep timer, click on Stop")
                .setPositiveButton("Stop") { dialog, _ ->
                    // Cancel the ongoing timer
               timer?.cancel()
                timer = null
                    isSleepTimerRunning = false
                    // Update the icon and background
                    iconModelArrayList[sleepTimerPosition] = IconModel(
                        R.drawable.round_sleep_timer,
                        "Sleep Timer",
                        iconBackground = R.drawable.ripple_circle
                    )
                    playbackIconsAdapter.notifyItemChanged(sleepTimerPosition)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
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
                    isSleepTimerRunning = true
                    // Update the icon and background
                    iconModelArrayList[sleepTimerPosition] = IconModel(
                        R.drawable.round_sleep_timer,
                        "Sleep Timer",
                        iconBackground = R.drawable.ripple_circle_cool_blue
                    )
                    playbackIconsAdapter.notifyItemChanged(sleepTimerPosition)

                    self.dismiss()
                    playVideo()
                }
                .setNegativeButton("Cancel") { self, _ ->
                    self.dismiss()

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
        event: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {

        val sWidth = Resources.getSystem().displayMetrics.widthPixels
        val sHeight = Resources.getSystem().displayMetrics.heightPixels

        val border = 600 * Resources.getSystem().displayMetrics.density.toInt()
        if (event.x < border || event.y < border || event.x > sWidth - border || event.y > sHeight - border)
            return false

        if(abs(distanceX) < abs(distanceY)){
            if(event.x < sWidth/2){
                val increase = distanceY > 0
                val newValue = if (increase) brightness + 1 else brightness - 1
                if (newValue in 0..15) brightness = newValue
                setScreenBrightness(brightness)
            } else {
                val maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val increase = distanceY > 0
                val newValue = if (increase) volume + 1 else volume - 1
                if (newValue in 0..maxVolume) volume = newValue
                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC,
                    volume, 0)
            }

            return true
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipeGesture() {
        binding.playerView.setOnTouchListener { view, motionEvent ->
            if (view.id != R.id.playerView) {
                // Touch event is outside the DoubleTapPlayerView, skip duration change logic
                isSwipingToChangeDuration = false
                hideProgressBars()
                return@setOnTouchListener false
            }

            if (!isLocked) {
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Initialize the swipe variables
                        currentSwipeX = motionEvent.x
                        currentSwipeY = motionEvent.y
                        initialPosition = player.currentPosition

                        isSwipingToChangeDuration = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Calculate the swipe distances
                        val deltaX = motionEvent.x - currentSwipeX
                        val deltaY = motionEvent.y - currentSwipeY

                        // Check if the swipe distance exceeds the threshold
                        if (abs(deltaX) > SWIPE_THRESHOLD) {
                            // User is swiping horizontally for duration change
                            isSwipingToChangeDuration = true
                            hideProgressBars()
                            // Determine the direction of the swipe
                            isSwipingForward = deltaX > 0

                            // Calculate the duration change based on the swipe distance
                            val durationChange =
                                (abs(deltaX) / binding.root.width) * MAX_DURATION_CHANGE

                            // Update the duration TextView
                            updateDurationTextView(durationChange, isSwipingForward)
                        } else if (abs(deltaY) > SWIPE_THRESHOLD) {
                            // User is swiping vertically for volume or brightness change
                            isSwipingToChangeDuration = false
                            showProgressBars(motionEvent.x, deltaY)

                            if (motionEvent.x < binding.root.width / 2) {
                                val increase = deltaY > 0
                                val newValue = if (increase) brightness - 1 else brightness + 1
                                if (newValue in 0..15) brightness = newValue
                                setScreenBrightness(brightness)
                            } else {
                                // For volume change
                                val maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val increase = deltaY > 0
                                val newValue = if (increase) volume - 1 else volume + 1
                                if (newValue in 0..maxVolume) volume = newValue
                               audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC,
                                    volume, 0)
                            }



                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {

                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        WindowInsetsControllerCompat(window, binding.root).let { controller ->
                            controller.hide(WindowInsetsCompat.Type.systemBars())
                            controller.systemBarsBehavior =
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        }
                        if (isSwipingToChangeDuration) {
                            // Get the displayed duration text from the TextView
                            val displayedText = durationChangeTextView.text.toString()

                            // Extract the duration change from the displayed text using regex
                            val regex = """\[ ([^\]]*) \]""".toRegex()
                            val matchResult = regex.find(displayedText)
                            val durationChangeText = matchResult?.groups?.get(1)?.value

                            // Convert the duration change text to milliseconds
                            val durationChangeMillis = parseDurationTextToMillis(durationChangeText)

                            // Apply the duration change based on the direction
                            if (isSwipingForward) {
                                player.seekTo(initialPosition + durationChangeMillis)
                            } else {
                                player.seekTo(initialPosition - durationChangeMillis)
                            }

                            // Hide the duration TextView
                            durationChangeTextView.visibility = View.GONE

                            // Reset the flag
                            isSwipingToChangeDuration = false

                        }
                        hideProgressBars()


                    }

                }

            }


            isSwipingToChangeDuration
        }
    }


    private fun showProgressBars(x: Float, deltaY: Float) {
        val progressChange = deltaY / binding.root.height * MAX_PROGRESS

        // Update the progress based on the direction of the swipe
        currentProgress = if (x < binding.root.width / 2) {
            // For the brightness progress bar
            (binding.brtProgress.progress ?: 0) - progressChange.toInt()
        } else {
            // For the volume progress bar
            (binding.volProgress.progress ?: 0) - progressChange.toInt()
        }

        // Update the progress bar visibility and progress value
        if (x < binding.root.width / 2) {
            binding.volProgressContainer.visibility = View.GONE
            binding.brtProgressContainer.visibility = View.VISIBLE
            binding.brtProgress.progress = currentProgress.coerceIn(0, MAX_PROGRESS)

        } else {
            binding.volProgressContainer.visibility = View.VISIBLE
            binding.brtProgressContainer.visibility = View.GONE
            binding.volProgress.progress = currentProgress.coerceIn(0, MAX_PROGRESS)
        }
    }


    private fun hideProgressBars() {
        // Hide both volume and brightness progress bars
        binding.volProgressContainer.visibility = View.GONE
        binding.brtProgressContainer.visibility = View.GONE


    }
    private fun updateDurationTextView(durationChange: Float, isForward: Boolean) {
        val actualDurationMinSec = getMinSecFormat(player.duration)
        val changingDurationSecMillisec = getSecMillisecFormat(durationChange)

        // Determine the sign based on the swipe direction
        val sign = if (isForward) "+" else "-"

        // Get the string from resources
        val formatString = getString(R.string.durationChangeTextView)

        // Use String.format to replace placeholders with actual values
        val text = String.format(formatString, actualDurationMinSec, "$sign$changingDurationSecMillisec")

        durationChangeTextView.text = text
        durationChangeTextView.visibility = View.VISIBLE

        // Calculate the center of the screen
        val centerX = binding.root.width / 2
        val centerY = binding.root.height / 2

        // Calculate the position of the TextView
        val x = (centerX - durationChangeTextView.width / 2).toFloat()
        val y = (centerY - durationChangeTextView.height / 2).toFloat()

        // Set the new position
        durationChangeTextView.x = x
        durationChangeTextView.y = y

        // Hide the TextView after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            durationChangeTextView.visibility = View.GONE
        }, 1000)
    }
    private fun getMinSecFormat(duration: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(duration)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    private fun getSecMillisecFormat(durationChange: Float): String {
        val minutes = durationChange.toLong() / 60000
        val seconds = (durationChange.toLong() % 60000) / 1000
        return String.format("%02d:%02d", minutes, seconds)
    }
    private fun parseDurationTextToMillis(durationText: String?): Long {
        if (durationText == null) {
            return 0
        }

        // Assuming the format is "mm:ss"
        val parts = durationText.split(":")
        if (parts.size == 2) {
            val minutes = parts[0].toLong()
            val seconds = parts[1].toLong()
            return TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds)
        }

        return 0
    }

    private fun setScreenBrightness(value: Int){
        val d = 1.0f/15
        val lp = this.window.attributes
        lp.screenBrightness = d * value
        this.window.attributes = lp
    }

}
