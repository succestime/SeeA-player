
package com.jaidev.seeaplayer.musicActivity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.Services.MusicService
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.dataClass.MusicFavDatabase
import com.jaidev.seeaplayer.dataClass.MusicFavEntity
import com.jaidev.seeaplayer.dataClass.favouriteChecker
import com.jaidev.seeaplayer.dataClass.formatDuration
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.databinding.ActivityPlayerMusicBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PlayerMusicActivity : AppCompatActivity() , ServiceConnection, MediaPlayer.OnCompletionListener, SpeedMusicBottomSheet.SpeedSelectionListener {

    private lateinit var playerMusicLayout: ConstraintLayout
    lateinit var mAdView: AdView
    private var appOpenAd : AppOpenAd? = null
    private lateinit var musicDatabase: MusicFavDatabase

    companion object {

        lateinit var musicListPA: ArrayList<Music>
        var songPosition: Int = 0
        var isPlaying: Boolean = false
        var min10: Boolean = false
        var min15: Boolean = false
        var min20: Boolean = false
        var min30: Boolean = false
        var min60: Boolean = false
        var isFavourite : Boolean = false
        var fIndex : Int = 0
        var musicService: MusicService? = null
        var nowMusicPlayingId : String = ""
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityPlayerMusicBinding
        var repeat: Boolean = false
        lateinit var loudnessEnhancer: LoudnessEnhancer
        private var isAdDisplayed = false
        var isShuffleEnabled = false
        private lateinit var originalMusicListPA: ArrayList<Music> // Original playlist order

        fun updateNextMusicTitle() {
            val nextSongPosition = if (songPosition + 1 < MainActivity.MusicListMA.size) songPosition + 1 else 0 // Assuming looping back to the first song after reaching the end
            val nextMusicTitle = MainActivity.MusicListMA[nextSongPosition].title
            binding.nextMusicTitle.text = nextMusicTitle


        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        musicDatabase = MusicFavDatabase.getDatabase(this)

        MobileAds.initialize(this){}
        mAdView = findViewById(R.id.adView)
        // banner ads
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        // Check if the activity was started with a list of selected music
        val selectedMusicList = intent.getParcelableArrayListExtra<Music>("SelectedMusicList")

        if (selectedMusicList != null && selectedMusicList.isNotEmpty()) {
            musicListPA = ArrayList(selectedMusicList) // Use selected music list
            originalMusicListPA = ArrayList(musicListPA) // Save the original list order
            songPosition = 0 // Start playing from the first song in the list

            // Initialize the music service and start playing
            val intentService = Intent(this, MusicService::class.java)
            bindService(intentService, this, BIND_AUTO_CREATE)
            startService(intentService)

            setLayout() // Update the UI with the current song details
        } else {
            initializeLayout() // Handle other cases, e.g., playing from another source
        }

        unKnown()
        setLayout()
        initializeBinding()
        updateNextMusicTitle()



        playerMusicLayout = binding.PlayerMusicLayout
        // Set the background color of SwipeRefreshLayout based on app theme
        setMusicLayoutBackgroundColor()

    }

    private fun unKnown(){
        if(intent.data?.scheme.contentEquals("content")){
            songPosition = 0
            val intentService = Intent(this, MusicService::class.java)
            bindService(intentService, this, BIND_AUTO_CREATE)
            startService(intentService)
            musicListPA = ArrayList()
//            musicListPA.add(getMusicDetails(intent.data!!))
            Glide.with(this)
                .load(getImgArt(musicListPA[songPosition].path))
                .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
                .into(binding.songImgPA)

            binding.songNamePA.text = musicListPA[songPosition].title
            setLayout()
        }
        else initializeLayout()


    }



    fun getCurrentSong(): Music {
        // Assuming you have a list of songs and a variable to store the current song position
        val currentSongPosition = songPosition
        return musicListPA[currentSongPosition]
    }
    private fun initializeBinding(){
        binding.backBtnPA.setOnClickListener { finish() }

        binding.musicMoreFun.setOnClickListener {
            // Create an instance of the BottomSheetFragment
                val moreMusicBottomSheetFragment = MoreMusicBottomSheet()
                // Show the BottomSheetFragment

                moreMusicBottomSheetFragment.show(supportFragmentManager, moreMusicBottomSheetFragment.tag)

        }
        binding.playPauseBtnPA.setOnClickListener {
            if (isPlaying) {
                pauseMusic()
                musicService!!.showNotification(R.drawable.round_play)
            }
            else {
                playMusic()
                musicService!!.showNotification(R.drawable.round_pause_24)
            }
        }
        binding.previousBtnPA.setOnClickListener {
            prevNextSong(increment = false)
        }
        binding.nextBtnPA.setOnClickListener {
            if (isShuffleEnabled) {
                if (musicListPA.isNotEmpty()) {
                    musicListPA.removeAt(songPosition) // Remove the currently playing song
                    songPosition = if (musicListPA.isNotEmpty()) {
                        0 // Always play the first song in the shuffled list
                    } else {
                        // Reset to the original list or stop playback
                        musicListPA.addAll(originalMusicListPA)
                        0
                    }
                }
            } else {
                prevNextSong(increment = true)
            }

            setLayout()
            createMediaPlayer()
        }


        // Inside onCreate or wherever you initialize your layout and bindings
        binding.shuffleBtnPA.setOnClickListener {
            isShuffleEnabled = !isShuffleEnabled
            // Change the color of the shuffle button based on the shuffle state
            if (isShuffleEnabled) {
                Toast.makeText(this, "Shuffle, play in shuffle order", Toast.LENGTH_SHORT).show()
                binding.shuffleBtnPA.setImageResource(R.drawable.media_playlist_consecutive_svgrepo_com__1_)
                binding.shuffleBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
                    R.color.cool_green))
                // Shuffle the music list
                originalMusicListPA = ArrayList(musicListPA) // Save original list
                musicListPA.shuffle()

                // Reset song position to start from the beginning
                songPosition = 0
                // Create and start playing music
                createMediaPlayer()
                // Update the layout with the current song details
                setLayout()
            } else {
                Toast.makeText(this, "Play all in the order", Toast.LENGTH_SHORT).show()
                binding.shuffleBtnPA.setImageResource(R.drawable.shuffle_icon)
                binding.shuffleBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
                    R.color.cool_pink))
                // Find the current song in the original list and update the song position
                val currentSong = musicListPA[songPosition]
                musicListPA = ArrayList(originalMusicListPA)
                songPosition = musicListPA.indexOf(currentSong)

                // Create and start playing music from the current position in the original list order
                setLayout()
                createMediaPlayer()
            }
            val intent = Intent(this, MusicService::class.java)
            bindService(intent, this, BIND_AUTO_CREATE)
            startService(intent)
        }

        binding.songNamePA.isSelected = true

        binding.seekBarPA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService!!.mediaPlayer!!.seekTo(progress)
                    musicService!!.showNotification(if (isPlaying) R.drawable.round_pause_24 else R.drawable.round_play)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        binding.repeatBtnPA.setOnClickListener {
            if (!repeat) {
                repeat = true
                Toast.makeText(this, "Repeat mode is on", Toast.LENGTH_SHORT).show()
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_green))
            } else {
                repeat = false
                Toast.makeText(this, "Repeat mode is off", Toast.LENGTH_SHORT).show()
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_pink))
            }
        }

        binding.favouriteBtnPA.setOnClickListener {
            fIndex = favouriteChecker(musicListPA[songPosition].id)
            val currentMusic = musicListPA[songPosition]

            if(isFavourite) {
                isFavourite = false
                binding.favouriteBtnPA.setImageResource(R.drawable.round_favorite_border_music)
                // Call deleteFromFavorites when a song is removed from favorites
                deleteFromFavorites(currentMusic)
            } else {
                isFavourite = true
                binding.favouriteBtnPA.setImageResource(R.drawable.round_favorite_music)
                // Call addToFavorites when a song is added to favorites
                addToFavorites(currentMusic)
                FavouriteActivity.favouriteSongs.add(currentMusic)
            }
        }



    }


    private fun addToFavorites(music: Music) {
        val musicFavEntity = MusicFavEntity(
            id = music.id,
            title = music.title,
            album = music.album,
            artist = music.artist,
            duration = music.duration,
            path = music.path,
            size = music.size.toString(),
            artUri = music.artUri,
            dateAdded = music.dateAdded
        )

        CoroutineScope(Dispatchers.IO).launch {
            musicDatabase.musicFavDao().insertMusic(musicFavEntity)
        }
    }

    private fun deleteFromFavorites(music: Music) {
        val musicFavEntity = MusicFavEntity(
           id = music.id,
            title = music.title,
            album = music.album,
            artist = music.artist,
            duration = music.duration,
            path = music.path,
            size = music.size.toString(),
            artUri = music.artUri,
            dateAdded = music.dateAdded
        )

        CoroutineScope(Dispatchers.IO).launch {
            musicDatabase.musicFavDao().deleteMusic(musicFavEntity)
        }
    }

    private fun   setMusicLayoutBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            playerMusicLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)

        } else {
            // Light mode is enabled, set background color to white
            playerMusicLayout.setBackgroundColor(resources.getColor(android.R.color.white))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        }
    }

    private fun initializeLayout() {
        songPosition = intent.getIntExtra("index", 0)
        when (intent.getStringExtra("class")) {
            "FavouriteAdapter" ->{
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(FavouriteActivity.favouriteSongs)
                originalMusicListPA = ArrayList(musicListPA)
                setLayout()
            }
          "MusicMP3Adapter" ->{
              val intent = Intent(this, MusicService::class.java)
              bindService(intent, this, BIND_AUTO_CREATE)
              startService(intent)
              musicListPA = ArrayList()
              musicListPA.addAll(MainActivity.MusicListMA)
              originalMusicListPA = ArrayList(musicListPA)
              setLayout()
          }
            "FavouriteBottomAdapter" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(FavouriteActivity.favouriteSongs)
                originalMusicListPA = ArrayList(musicListPA)
                setLayout()
            }
            "NowPlaying" ->{
                setLayout()
                binding.tvSeekBarStart.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekBarPA.progress = musicService!!.mediaPlayer!!.currentPosition
                binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
                if(isPlaying) binding.playPauseBtnPA.setIconResource(R.drawable.round_pause_24)
                else binding.playPauseBtnPA.setIconResource(R.drawable.round_play)
            }
            "MusicAdapter" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(MainActivity.MusicListMA)
                originalMusicListPA = ArrayList(musicListPA)
                setLayout()

            }
            "MusicBottomPlayAdapter" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(MainActivity.MusicListMA)
                originalMusicListPA = ArrayList(musicListPA)
                setLayout()

            }
            "FavouriteMultiSelect" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                originalMusicListPA = ArrayList(musicListPA)
                setLayout()
                createMediaPlayer()
            }
            "FavouriteAShuffle" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
               musicListPA = ArrayList()
                musicListPA.addAll(PlaylistDetails.videoList)
              originalMusicListPA = ArrayList(musicListPA)
                setLayout()
            }

            "PlaylistDetails" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = intent.getParcelableArrayListExtra("musicList") ?: ArrayList()
                originalMusicListPA = ArrayList(musicListPA)
                setLayout()
            }
            "bottomSheetPlay" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(PlaylistDetails.videoList)
                originalMusicListPA = ArrayList(musicListPA)
                setLayout()
            }
            "PlaylistDetailsShuffle" -> {

            }
            "MusicNav" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(MainActivity.MusicListMA)
                originalMusicListPA = ArrayList(musicListPA)
                musicListPA.shuffle()
                setLayout()

            }
            "MusicAdapterSearch" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(MainActivity.musicListSearch)
                originalMusicListPA = ArrayList(musicListPA)
                setLayout()
            }
            "FavouriteShuffle" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(FavouriteActivity.favouriteSongs)
                musicListPA.shuffle()
                createMediaPlayer()
                setLayout()
            }
        "MusicNav2" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(MainActivity.MusicListMA)
                originalMusicListPA = ArrayList(musicListPA)
                setLayout()
            }

            "PlaylistDetailsAdapter" ->{
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                originalMusicListPA = ArrayList(musicListPA)

                setLayout()
            }

        }
    }

    private fun setLayout() {
        fIndex = favouriteChecker(musicListPA[songPosition].id)

        Glide.with(applicationContext)
            .load(getImgArt(musicListPA[songPosition].path))
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
            .into(binding.songImgPA)
        binding.songNamePA.text = musicListPA[songPosition].title
        if (repeat) binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.cool_green
        ))else   binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.cool_pink))

        if (isShuffleEnabled) binding.shuffleBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.cool_green))
        if (!isShuffleEnabled)  binding.shuffleBtnPA.setImageResource(R.drawable.shuffle_icon)
        else binding.shuffleBtnPA.setImageResource(R.drawable.media_playlist_consecutive_svgrepo_com__1_)
//        if(min15 || min30 || min60) binding.timerBtnPA?.setColorFilter(ContextCompat.getColor(applicationContext,
//            R.color.cool_green
//        ))
        if(isFavourite) binding.favouriteBtnPA.setImageResource(R.drawable.round_favorite_music)
        else binding.favouriteBtnPA.setImageResource(R.drawable.round_favorite_border_music)

    }

    private fun createMediaPlayer() {
        try {
            if (!fileExists(musicListPA[songPosition].path)) {
                prevNextSong(true) // Skip to the next song
                return
            }
            if (musicService!!.mediaPlayer == null) musicService!!.mediaPlayer = MediaPlayer()
            musicService!!.mediaPlayer!!.reset()
            musicService!!.mediaPlayer!!.setDataSource(musicListPA[songPosition].path)
            musicService!!.mediaPlayer!!.prepare()
            musicService!!.mediaPlayer!!.start()
            isPlaying = true
            binding.playPauseBtnPA.setIconResource(R.drawable.round_pause_24)
            musicService!!.showNotification(R.drawable.round_pause_24)
            updateNextMusicTitle()
            binding.tvSeekBarStart.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
            binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
            binding.seekBarPA.progress = 0
            NowPlaying.binding.root.visibility = View.VISIBLE
            binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
            musicService!!.mediaPlayer!!.setOnCompletionListener(this)
            nowMusicPlayingId = musicListPA[songPosition].id
            loudnessEnhancer = LoudnessEnhancer(musicService!!.mediaPlayer!!.audioSessionId)
            loudnessEnhancer.enabled = true

        } catch (e: Exception) {
            return
        }
    }

    private fun fileExists(path: String): Boolean {
        return File(path).exists()
    }

    private fun playMusic() {
        isPlaying = true
      musicService!!.mediaPlayer!!.start()
        binding.playPauseBtnPA.setIconResource(R.drawable.round_pause_24)
       musicService!!.showNotification(R.drawable.round_pause_24)

    }

    private fun pauseMusic() {
        isPlaying = false
        binding.playPauseBtnPA.setIconResource(R.drawable.round_play)
        musicService!!.showNotification(R.drawable.round_play)
     musicService!!.mediaPlayer!!.pause()

    }
    private fun setSongPosition(increment: Boolean) {
        songPosition = if (increment) {
            if (isShuffleEnabled) {
                (songPosition + 1) % musicListPA.size
            } else {
                if (songPosition + 1 < musicListPA.size) songPosition + 1 else 0
            }
        } else {
            if (isShuffleEnabled) {
                if (songPosition - 1 < 0) musicListPA.size - 1 else songPosition - 1
            } else {
                if (songPosition - 1 < 0) musicListPA.size - 1 else songPosition - 1
            }
        }
    }

    private fun prevNextSong(increment: Boolean) {
        if (increment) {
            setSongPosition(increment = true)
            setLayout()
            createMediaPlayer()
        } else {
            setSongPosition(increment = false)
            setLayout()
            createMediaPlayer()
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if(musicService == null){
            val binder = service as MusicService.MyBinder
            musicService = binder.currentService()
//            musicService!!.audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
//            musicService!!.audioManager.requestAudioFocus(musicService, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        createMediaPlayer()
        musicService!!.
        seekBarSetup()



    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }


    override fun onCompletion(mp: MediaPlayer?) {
        if (musicListPA.isNotEmpty()) {
            // Check if the currently playing music has been deleted
            val deletedMusic = musicListPA[songPosition]
            if (deletedMusic.id != nowMusicPlayingId) {
                // The currently playing music has been deleted, adjust the song position

                createMediaPlayer()
                setLayout()
                return
            }
        }

        // Proceed with normal behavior
        setSongPosition(increment = true)
        createMediaPlayer()
        setLayout()

        // Refresh now playing image & text on song completion
        NowPlaying.binding.songNameNP.isSelected = true
        Glide.with(applicationContext)
            .load(getImgArt(musicListPA[songPosition].path))
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
            .into(NowPlaying.binding.songImgNP)
        NowPlaying.binding.songNameNP.text = musicListPA[songPosition].title
    }




    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 13 || resultCode == RESULT_OK)
            return
    }
    private fun loadAppOpenAd() {
        if (!isAdDisplayed) {
            val adRequest = AdRequest.Builder().build()
            AppOpenAd.load(
                this,
                "ca-app-pub-3504589383575544/3498264750",
                adRequest,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                appOpenAdLoadCallback
            )
        }
    }

    private val appOpenAdLoadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {
        override fun onAdLoaded(ad: AppOpenAd) {
            appOpenAd = ad
            appOpenAd!!.show(this@PlayerMusicActivity)
            isAdDisplayed = true // Mark ad as displayed
        }

        override fun onAdFailedToLoad(p0: LoadAdError) {
            // Handle failed ad loading
        }
    }



    override fun onDestroy() {
        super.onDestroy()
       loadAppOpenAd()

    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onSpeedSelected(speed: Float) {
        musicService?.mediaPlayer?.let { mediaPlayer ->
            if (mediaPlayer.isPlaying) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(speed)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mAdView = findViewById(R.id.adView)
        // banner ads
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

}



