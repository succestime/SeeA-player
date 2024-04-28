
package com.jaidev.seeaplayer.recantFragment

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
import android.view.Window
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
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.PlaybackIconsAdapter
import com.jaidev.seeaplayer.allAdapters.RecentVideoAdapter
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.dataClass.IconModel
import com.jaidev.seeaplayer.dataClass.RecantVideo
import com.jaidev.seeaplayer.databinding.ActivityRePlayerBinding
import com.jaidev.seeaplayer.databinding.BoosterBinding
import com.jaidev.seeaplayer.databinding.SpeedDialogBinding
import java.io.File
import java.text.DecimalFormat
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.system.exitProcess


class ReVideoPlayerActivity : AppCompatActivity(), AudioManager.OnAudioFocusChangeListener , GestureDetector.OnGestureListener {
    private lateinit var binding: ActivityRePlayerBinding
    private lateinit var playPauseBtn: ImageButton
    private lateinit var fullScreenBtn: ImageButton
    private lateinit var videoTitle: TextView
    private lateinit var gestureDetectorCompat: GestureDetectorCompat
    private lateinit var adapter: RecentVideoAdapter
    private var isSwipingForward = false
    private var currentSwipeX = 0f
    private var currentSwipeY = 0f
    private var initialPosition = 0L
    private lateinit var durationChangeTextView: TextView
    private var isSwipingToChangeDuration = false
    private var currentProgress: Int = 0
    private val iconModelArrayList = ArrayList<IconModel>()
    private lateinit var playbackIconsAdapter: PlaybackIconsAdapter
    private lateinit var recyclerViewIcons: RecyclerView
    var expand = false
    var nightMode: View? = null
    var dark: Boolean = false
    var mute: Boolean = false
    lateinit var dialogProperties: DialogProperties
    lateinit var filePickerDialog: FilePickerDialog
    lateinit var uriSubtitle: Uri
    private lateinit var eqContainer: FrameLayout
    private var isPlayingBeforePause = false // Flag to track if video was playing before going into background
    private lateinit var player: ExoPlayer
    lateinit var mAdView: AdView

    // horizontal recyclerView variables
    companion object {
        private var audioManager: AudioManager? = null
        private lateinit var player: ExoPlayer
        lateinit var recantPlayerList : ArrayList<RecantVideo>
        var position: Int = -1
        private var repeat: Boolean = false
        private var isFullscreen: Boolean = false
        private var isLocked: Boolean = false
        @SuppressLint("StaticFieldLeak")
        private lateinit var trackSelector: DefaultTrackSelector
        private lateinit var loudnessEnhancer: LoudnessEnhancer
        private var speed: Float = 1.0f
        private var timer: Timer? = null
        var nowPlayingId: String = ""
        var pipStatus: Int = 0
        private var brightness: Int = 0
        private var volume: Int = 0
        private const val MAX_DURATION_CHANGE = 10 * 1000L // Maximum duration change in milliseconds
        private const val SWIPE_THRESHOLD = 50 // Swipe threshold in pixels
        private const val MAX_PROGRESS = 100

    }


    @SuppressLint("ObsoleteSdkInt", "SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRePlayerBinding.inflate(layoutInflater)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityRePlayerBinding.inflate(layoutInflater)
        setTheme(R.style.coolBlueNav)
        setContentView(binding.root)

        MobileAds.initialize(this){}
        mAdView = findViewById(R.id.adView)


        // banner ads


        initializePlayer()

        videoTitle = findViewById(R.id.videoTitle)
        playPauseBtn = findViewById(R.id.playPauseBtn)
        fullScreenBtn = findViewById(R.id.fullScreenBtn)
        durationChangeTextView = findViewById(R.id.durationChangeTextView)
        durationChangeTextView.visibility = View.GONE

        nightMode = findViewById(R.id.night_mode)
        recyclerViewIcons = findViewById(R.id.horizontalRecyclerview)
        eqContainer = findViewById<FrameLayout>(R.id.eqFrame)
        gestureDetectorCompat = GestureDetectorCompat(this, this)

        // for immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        dialogProperties = DialogProperties()
        filePickerDialog = FilePickerDialog(this@ReVideoPlayerActivity)
        filePickerDialog.setTitle("Select a Subtitle File")
        filePickerDialog.setPositiveBtnName("OK")
        filePickerDialog.setNegativeBtnName("Cancel")


        horizontalIconList()
        try {
            if (intent.data?.scheme.contentEquals("content")) {
                recantPlayerList = ArrayList()
                position = 0
                val cursor = contentResolver.query(
                    intent.data!!,
                    arrayOf(MediaStore.Video.Media.DATA),
                    null,
                    null,
                    null
                )
                cursor?.let {
                    it.moveToFirst()
                    val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    val file = File(path)
                    val video = RecantVideo(
                        id = "", title = file.name, duration = 0L,
                        artUri = Uri.fromFile(file), path = path, size = "", timestamp = System.currentTimeMillis()
                    )
                    recantPlayerList.add(video)
                    cursor.close()
                }
                createPlayer()
                initializeBinding()
            } else {
                initializeLayout()
                initializeBinding()
            }

        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
        }


    }

    @SuppressLint("NotifyDataSetChanged")
    private fun horizontalIconList() {
        iconModelArrayList.add(IconModel(R.drawable.next_icon,"", android.R.color.white))
        iconModelArrayList.add(IconModel(R.drawable.night_mode,"Night", android.R.color.white))
        iconModelArrayList.add(IconModel(R.drawable.ic_speed_icon,"Speed", android.R.color.white))
        iconModelArrayList.add(IconModel(R.drawable.orientation_icon,"Rotate", android.R.color.white))
        iconModelArrayList.add(IconModel(R.drawable.search_link_tube,"Link Tube", android.R.color.white))


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
                                    R.drawable.next_icon,
                                    "",
                                    android.R.color.white
                                )
                            )
                            iconModelArrayList.add(
                                IconModel(
                                    R.drawable.night_mode,
                                    "Night",
                                    android.R.color.white
                                )
                            )
                            iconModelArrayList.add(
                                IconModel(
                                    R.drawable.ic_speed_icon,
                                    "Speed",
                                    android.R.color.white
                                )
                            )
                            iconModelArrayList.add(
                                IconModel(
                                    R.drawable.orientation_icon,
                                    "Rotate",
                                    android.R.color.white
                                )
                            )
                            iconModelArrayList.add(
                                IconModel(
                                    R.drawable.search_link_tube,
                                    "Link Tube",
                                    android.R.color.white
                                )
                            )
                            playbackIconsAdapter.notifyDataSetChanged()
                            expand = false
                        } else {

                            if (iconModelArrayList.size == 5) {
                                iconModelArrayList.add(
                                    IconModel(
                                        R.drawable.ic_timer_icon,
                                        "Sleep Timer",
                                        android.R.color.white
                                    )
                                )
                                iconModelArrayList.add(
                                    IconModel(
                                        R.drawable.muit2_round,
                                        "Mute",
                                        android.R.color.white
                                    )
                                )
                                iconModelArrayList.add(
                                    IconModel(
                                        R.drawable.ic_booster_icon,
                                        "Booster",
                                        android.R.color.white
                                    )
                                )
                                iconModelArrayList.add(
                                    IconModel(
                                        R.drawable.ic_picture_in_picture_icon,
                                        "PIP Mode",
                                        android.R.color.white
                                    )
                                )

                                iconModelArrayList.add(
                                    IconModel(
                                        R.drawable.ic_subtitles_icon,
                                        "Subtitle",
                                        android.R.color.white
                                    )
                                )
                                iconModelArrayList.add(
                                    IconModel(
                                        R.drawable.equalizer_icon,
                                        "Equalizer",
                                        android.R.color.white
                                    )
                                )


                            }
                            iconModelArrayList[position] = IconModel(R.drawable.ic_back_icon, "")
                            playbackIconsAdapter.notifyDataSetChanged()
                            expand = true
                        }
                    }

                    1 -> {
                        if (dark) {
                            nightMode?.visibility = View.GONE
                            iconModelArrayList[position] = IconModel(R.drawable.night_mode, "Night")
                            playbackIconsAdapter.notifyDataSetChanged()
                            dark = false
                        } else {
                            nightMode?.visibility = View.VISIBLE
                            iconModelArrayList[position] = IconModel(R.drawable.night_mode, "Day")
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
                        } else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            playbackIconsAdapter.notifyDataSetChanged()
                        }
                    }
                    4 -> {
                        startActivity(Intent(this@ReVideoPlayerActivity, LinkTubeActivity::class.java))

                    }
                    5-> {
                        setupSleepTimer()
                    }

                    6 -> {
                        if (mute) {
                            player.setVolume(100F)
                            iconModelArrayList[position] = IconModel(R.drawable.muit2_round, "Mute")
                            playbackIconsAdapter.notifyDataSetChanged()
                            mute = false
                        } else {
                            player.setVolume(0F)
                            iconModelArrayList[position] =
                                IconModel(R.drawable.volume_icon, "Unmute")
                            playbackIconsAdapter.notifyDataSetChanged()
                            mute = true
                        }

                    }

                    7 -> {
                        setupBoosterDialog()
                    }

                    8 -> {
                        setupPIPMode()
                    }

                    9-> {
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

                    10 -> {
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

    private fun getSemiTransparentGrayDrawable(): ColorDrawable {
        val color = Color.parseColor("#011B29")
        return ColorDrawable(color)
    }
    @SuppressLint("SetTextI18n")
    fun setupSleepTimer() {
        if (timer != null)
            Toast.makeText(
                this@ReVideoPlayerActivity,
                "Timer Already Running !\nClose App to Reset Timer ..",
                Toast.LENGTH_SHORT
            ).show()
        else {
            var sleepTime = 15
            val customDialogS = LayoutInflater.from(this@ReVideoPlayerActivity)
                .inflate(R.layout.speed_dialog, binding.root, false)
            val bindingS = SpeedDialogBinding.bind(customDialogS)
            val dialogS = MaterialAlertDialogBuilder(this@ReVideoPlayerActivity).setView(customDialogS)
                .setOnCancelListener { playVideo() }
                .setPositiveButton("Done") { self, _ ->
                    Toast.makeText(this@ReVideoPlayerActivity, "Sleep Timer is start", Toast.LENGTH_SHORT).show()
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

    private fun initializeLayout() {
        when (intent.getStringExtra("class")) {
            "RecantVideo" -> {
                recantPlayerList = ArrayList()
                recantPlayerList.addAll(MainActivity.videoRecantList)
                createPlayer()
            }
        }
    }

    private fun initializePlayer() {
        player = SimpleExoPlayer.Builder(this).build()
        binding.playerView.player = player

        val mediaItem = MediaItem.fromUri("YOUR_VIDEO_URI_HERE")
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
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
    @SuppressLint("SetTextI18n", "SuspiciousIndentation", "ObsoleteSdkInt")
    private fun initializeBinding() {


        findViewById<ImageButton>(R.id.backBtn).setOnClickListener {
            finish()
        }

        playPauseBtn.setOnClickListener {
            if (player.isPlaying) pauseVideo()
            else playVideo()
        }

        findViewById<ImageButton>(R.id.nextBtn).setOnClickListener { nextPrevVideo() }
        findViewById<ImageButton>(R.id.prevBtn).setOnClickListener { nextPrevVideo(isNext = false) }
        findViewById<ImageButton>(R.id.repeatBtn).setOnClickListener {
            if (repeat) {
                repeat = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                findViewById<ImageButton>(R.id.repeatBtn).setImageResource(R.drawable.ic_repeat_off_icon)
            } else {
                repeat = true
                player.repeatMode = Player.REPEAT_MODE_ONE
                findViewById<ImageButton>(R.id.repeatBtn).setImageResource(R.drawable.ic_repeat_on)
            }
        }

        fullScreenBtn.setOnClickListener {
            if (isFullscreen) {
                isFullscreen = false
                playInFullscreen(enable = false)
            } else {
                isFullscreen = true
                playInFullscreen(enable = true)
            }
        }
        binding.lockButton.setOnClickListener {
            if (!isLocked) {
                // for hiding
                isLocked = true
                binding.playerView.hideController()
                binding.playerView.useController = false
                binding.lockButton.setImageResource(R.drawable.ic_lock_close_icon)
            } else {
                // for showing
                isLocked = false
                binding.playerView.useController = true
                binding.playerView.showController()

                binding.lockButton.setImageResource(R.drawable.ic_lock_open_icon)
            }
        }
    }

    private fun createPlayer() {

        try {
            player.release()
        } catch (_: Exception) {
        }
        speed = 1.0f
        trackSelector = DefaultTrackSelector(this)
        videoTitle.isSelected = true
        videoTitle.text = recantPlayerList[position].title
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        doubleTapEnable()
        setupSwipeGesture()

        val mediaItem = MediaItem.fromUri(recantPlayerList[position].artUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        playVideo()



        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) nextPrevVideo()
            }
        })

        playInFullscreen(enable = isFullscreen)

        loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
        loudnessEnhancer.enabled = true

        nowPlayingId = recantPlayerList[position].id

        seekBarFeature()
        binding.playerView.setControllerVisibilityListener {
            when {
                isLocked -> binding.lockButton.visibility = View.VISIBLE
                binding.playerView.isControllerVisible -> binding.lockButton.visibility =
                    View.VISIBLE

                else -> binding.lockButton.visibility = View.INVISIBLE
            }


        }
    }

    private fun playVideo() {
        playPauseBtn.setImageResource(R.drawable.ic_pause_icon)
        nowPlayingId = recantPlayerList[position].id
        player.play()


    }


    private fun pauseVideo() {
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
        playPauseBtn.setImageResource(R.drawable.ic_play_icon)
        player.pause()
    }

    private fun nextPrevVideo(isNext: Boolean = true) {
        if (isNext) setPosition()
        else setPosition(isIncrement = false)
        createPlayer()
    }

    private fun setPosition(isIncrement: Boolean = true) {
        if (!repeat) {
            if (isIncrement) {
                if (recantPlayerList.size - 1 == position)
                    position = 0
                else ++position
            } else {
                if (position == 0)
                    position = recantPlayerList.size - 1
                else --position
            }
        }
    }

    private fun playInFullscreen(enable: Boolean) {
        if (enable) {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            findViewById<ImageButton>(R.id.fullScreenBtn).setImageResource(R.drawable.ic_fullscreen_exit_icon)
        } else {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            findViewById<ImageButton>(R.id.fullScreenBtn).setImageResource(R.drawable.ic_fullscreen_icon)
        }
    }

    private fun changeSpeed(isIncrement: Boolean) {
        if (isIncrement) {
            if (speed < 3.9f) {
                speed += 0.10f  // speed = speed + 0.10f
            }
        } else {
            if (speed > 0.20f) {
                speed -= 0.10f    // speed = speed - 0.10f
            }
        }
        player.setPlaybackSpeed(speed)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if(pipStatus != 0){
            recreate()
            playInFullscreen(true)
            val intent = Intent(this, ReVideoPlayerActivity::class.java)
            when(pipStatus){
                1 -> intent.putExtra("class","RecantVideo")

            }
            startActivity(intent)
        }
        if(isInPictureInPictureMode){ playVideo()
            playPauseBtn.setImageResource(R.drawable.ic_pause_icon)
        } else {
            pauseVideo()
            playPauseBtn.setImageResource(R.drawable.ic_pause_icon)

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
        if (brightness != 0) setScreenBrightness(brightness)
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
                                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                            }



                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {

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

        // Get the string from resources
        val formatString = getString(R.string.durationChangeTextView)

        // Use String.format to replace placeholders with actual values
        val text = String.format(formatString, actualDurationMinSec, changingDurationSecMillisec)

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


    private fun seekBarFeature() =
        findViewById<DefaultTimeBar>(R.id.exo_progress).addListener(object :
            TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                pauseVideo()
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                player.seekTo(position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                playVideo()
            }

        })


    override fun onDown(p0: MotionEvent): Boolean = false
    override fun onShowPress(p0: MotionEvent) = Unit
    override fun onSingleTapUp(p0: MotionEvent): Boolean = false
    override fun onLongPress(p0: MotionEvent) = Unit
    override fun onFling(p0: MotionEvent?, p1: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false
    override fun onScroll(
        e1: MotionEvent?,
        event: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {

        val sWidth = Resources.getSystem().displayMetrics.widthPixels
        val sHeight = Resources.getSystem().displayMetrics.heightPixels

        val border = 100 * Resources.getSystem().displayMetrics.density.toInt()
        if (event.x < border || event.y < border || event.x > sWidth - border || event.y > sHeight - border)
            return false

        if(abs(distanceX) < abs(distanceY)){
            if(event.x < sWidth/2){
                val increase = distanceY > 0
                val newValue = if (increase) brightness + 1 else brightness - 1
                if (newValue in 0..15) brightness = newValue
//                setScreenBrightness(brightness)
            } else {
                val maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val increase = distanceY > 0
                val newValue = if (increase) volume + 1 else volume - 1
                if (newValue in 0..maxVolume) volume = newValue
                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
            }

            return true
        }
        return false
    }


    private fun setScreenBrightness(value: Int){
        val d = 1.0f/15
        val lp = this.window.attributes
        lp.screenBrightness = d * value
        this.window.attributes = lp
    }
}
