
package com.jaidev.seeaplayer.recantFragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
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
import com.jaidev.seeaplayer.dataClass.MusicFavouriteData.MusicFavDatabase
import com.jaidev.seeaplayer.dataClass.RecantMusic
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.dataClass.reFormatDuration
import com.jaidev.seeaplayer.dataClass.setSongPosition
import com.jaidev.seeaplayer.databinding.ActivityReMusicPlayerBinding
import com.jaidev.seeaplayer.musicActivity.ReMoreMusicBottomSheet
import com.jaidev.seeaplayer.musicActivity.SpeedMusicBottomSheet
import java.io.File

class ReMusicPlayerActivity : AppCompatActivity()
    , ServiceConnection, MediaPlayer.OnCompletionListener , SpeedMusicBottomSheet.SpeedSelectionListener
{
    lateinit var mAdView: AdView
    private lateinit var reMusicPlayerLayout: ConstraintLayout
    private var appOpenAd : AppOpenAd? = null
    private lateinit var musicDatabase: MusicFavDatabase

    companion object {
        // of PlayerActivity of this reMusicActivity
        var reMusicListPA: ArrayList<RecantMusic> = ArrayList()
        var songPosition: Int = 0
        var isPlaying: Boolean = false
        var musicService : MusicService? = null
        var nowMusicPlayingId : String = ""

        private var isServiceBound = null
        var position: Int = -1
        var min10: Boolean = false
        var min15: Boolean = false
        var min20: Boolean = false
        var min30: Boolean = false
        var min60: Boolean = false
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityReMusicPlayerBinding
        var repeat: Boolean = false
        private var isAdDisplayed = false
        var isShuffleEnabled = false
        private lateinit var originalMusicListPA: ArrayList<RecantMusic> // Original playlist order

        fun updateNextMusicTitle() {
            val nextSongPosition = if (songPosition + 1 < MainActivity.musicRecantList.size) songPosition + 1 else 0 // Assuming looping back to the first song after reaching the end
            val nextMusicTitle = MainActivity.musicRecantList[nextSongPosition].title
            binding.nextMusicTitle.text = nextMusicTitle

        }


        fun setLayout(context: Context) {
            if (!fileExists(reMusicListPA[songPosition].path)) {
                // Check if there are other songs to play
                if (reMusicListPA.size > 1) {
                    setSongPosition(increment = true)
                    setLayout(context)
                    createMediaPlayer(context)
                }else{
                    musicService!!.mediaPlayer!!.stop()
                    ReNowPlaying.binding.root.visibility = View.GONE

                }
                return
            }
            Glide.with(context)
                .load(getImgArt(reMusicListPA[songPosition].path))
                .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
                .into(binding.songImgPA)

            binding.songNamePA.text = reMusicListPA[songPosition].title

            if (repeat) binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(context, R.color.cool_green))
            else binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(context, R.color.cool_pink))

            if (isShuffleEnabled) binding.shuffleBtnPA.setColorFilter(ContextCompat.getColor(context, R.color.cool_green))
            else binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(context, R.color.cool_pink))

            if (!isShuffleEnabled) binding.shuffleBtnPA.setImageResource(R.drawable.shuffle_icon)
            else binding.shuffleBtnPA.setImageResource(R.drawable.media_playlist_consecutive_svgrepo_com__1_)



        }
        fun createMediaPlayer(context: Context) {
            try {
                if (!fileExists(reMusicListPA[songPosition].path)) {
                    // Check if there are other songs to play
                    if (reMusicListPA.size > 1) {
                        setSongPosition(increment = true)
                        setLayout(context)
                        createMediaPlayer(context)
                    }else{
                        musicService!!.mediaPlayer!!.stop()
                        ReNowPlaying.binding.root.visibility = View.GONE

                    }
                    return
                }
                if (musicService!!.mediaPlayer == null) musicService!!.mediaPlayer = MediaPlayer()
                musicService!!.mediaPlayer!!.reset()
                musicService!!.mediaPlayer!!.setDataSource(reMusicListPA[songPosition].path)
                musicService!!.mediaPlayer!!.prepare()
                musicService!!.mediaPlayer!!.start()
                isPlaying = true
                binding.tvSeekBarStart.text = reFormatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekBarEnd.text = reFormatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekBarPA.progress = 0
                binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
                binding.playPauseBtnPA.setIconResource(R.drawable.round_pause_24)
                updateNextMusicTitle()
                musicService!!.mediaPlayer!!.setOnCompletionListener(this@Companion)
            }catch (e : Exception){return}
        }





        private fun fileExists(path: String): Boolean {
            val file = File(path)
            return file.exists()
        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReMusicPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        updateNextMusicTitle()
        initializeLayout()
        initializeBinding()

        MobileAds.initialize(this){}
        mAdView = findViewById(R.id.adView)
        // banner ads
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        reMusicPlayerLayout = binding.ReMusicPlayerLayout
        setMusicLayoutBackgroundColor()

    }

    private fun initializeBinding(){
        binding.backBtnPA.setOnClickListener { finish() }

        binding.playPauseBtnPA.setOnClickListener {
            if (isPlaying) pauseMusic()
            else playMusic()
        }
        binding.musicMoreFun.setOnClickListener {
            // Create an instance of the BottomSheetFragment
            val moreMusicBottomSheetFragment = ReMoreMusicBottomSheet()
            // Show the BottomSheetFragment

            moreMusicBottomSheetFragment.show(supportFragmentManager, moreMusicBottomSheetFragment.tag)

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
                originalMusicListPA = ArrayList(reMusicListPA) // Save original list
                reMusicListPA.shuffle()

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
                val currentSong = reMusicListPA[songPosition]
                reMusicListPA = ArrayList(originalMusicListPA)
                songPosition = reMusicListPA.indexOf(currentSong)

                // Create and start playing music from the current position in the original list order
                setLayout()
                createMediaPlayer()
            }
            val intent = Intent(this, MusicService::class.java)
            bindService(intent, this, BIND_AUTO_CREATE)
            startService(intent)
        }

        binding.songNamePA.isSelected = true



        binding.previousBtnPA.setOnClickListener {
            prevNextSong(increment = false)
        }
        binding.nextBtnPA.setOnClickListener {
            prevNextSong(increment = true)
        }
        binding.seekBarPA.setOnSeekBarChangeListener(object  : OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) musicService!!.mediaPlayer!!.seekTo(progress)
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


    }


    private fun   setMusicLayoutBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            reMusicPlayerLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)

        } else {
            // Light mode is enabled, set background color to white
            reMusicPlayerLayout.setBackgroundColor(resources.getColor(android.R.color.white))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR


        }
    }



    private fun setLayout(){
        if (!fileExists(reMusicListPA[songPosition].path)) {
            // Check if there are other songs to play
            if (reMusicListPA.size > 1) {
                setSongPosition(increment = true)
                setLayout()
                createMediaPlayer()
            }else{
                musicService!!.mediaPlayer!!.stop()
                ReNowPlaying.binding.root.visibility = View.GONE

            }
            return
        }
        Glide.with(applicationContext)
            .load(getImgArt(reMusicListPA[songPosition].path))
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
            .into(binding.songImgPA)

        binding.songNamePA.text = reMusicListPA[songPosition].title

        if (repeat) binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.cool_green
        ))else   binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.cool_pink))

        if (isShuffleEnabled) binding.shuffleBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.cool_green))else   binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.cool_pink))
        if (!isShuffleEnabled)  binding.shuffleBtnPA.setImageResource(R.drawable.shuffle_icon)
        else binding.shuffleBtnPA.setImageResource(R.drawable.media_playlist_consecutive_svgrepo_com__1_)

    }
    private fun fileExists(path: String): Boolean {
        return File(path).exists()
    }
    private fun createMediaPlayer(){
        try {
            if (!fileExists(reMusicListPA[songPosition].path)) {
                // Check if there are other songs to play
                if (reMusicListPA.size > 1) {
                    setSongPosition(increment = true)
                    setLayout()
                    createMediaPlayer()
                }else{
                    musicService!!.mediaPlayer!!.stop()
                    ReNowPlaying.binding.root.visibility = View.GONE

                }
                return
            }
            if (musicService!!.mediaPlayer == null) musicService!!.mediaPlayer = MediaPlayer()
            musicService!!.mediaPlayer!!.reset()
            musicService!!.mediaPlayer!!.setDataSource(reMusicListPA[songPosition].path)
            musicService!!.mediaPlayer!!.prepare()
            musicService!!.mediaPlayer!!.start()
            isPlaying = true

            binding.tvSeekBarStart.text = reFormatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
            binding.tvSeekBarEnd.text = reFormatDuration(musicService!!.mediaPlayer!!.duration.toLong())
            binding.seekBarPA.progress = 0
            binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
            binding.playPauseBtnPA.setIconResource(R.drawable.round_pause_24)
            updateNextMusicTitle()
            musicService!!.mediaPlayer!!.setOnCompletionListener(this)
        }catch (e : Exception){return}
    }

    fun getCurrentSong(): RecantMusic {
        // Assuming you have a list of songs and a variable to store the current song position
        val currentSongPosition = songPosition
        return reMusicListPA[currentSongPosition]
    }
    private fun initializeLayout(){
        songPosition =  intent.getIntExtra("index" , 0)
        when(intent.getStringExtra("class")){
            "RecantMusicAdapter" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                reMusicListPA = ArrayList()
                reMusicListPA.addAll(MainActivity.musicRecantList)
                originalMusicListPA = ArrayList(reMusicListPA) // Save original list
                setLayout()
            }
            "MusicBottomPlayAdapter" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                reMusicListPA = ArrayList()
                reMusicListPA.addAll(MainActivity.musicRecantList)
                originalMusicListPA = ArrayList(reMusicListPA) // Save original list
                setLayout()
            }

            "ReNowPlaying" -> {
                setLayout()
                binding.tvSeekBarStart.text = reFormatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekBarEnd.text = reFormatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
                binding.seekBarPA.progress = musicService!!.mediaPlayer!!.currentPosition
                if(isPlaying) binding.playPauseBtnPA.setIconResource(R.drawable.round_pause_24)
                else binding.playPauseBtnPA.setIconResource(R.drawable.round_play)
            }
            "DaysMusic" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                reMusicListPA = ArrayList()
                reMusicListPA.addAll(MainActivity.musicRecantList)
                originalMusicListPA = ArrayList(reMusicListPA) // Save original list
                reMusicListPA.shuffle()
                setLayout()
            }
        }
    }
    private fun playMusic() {
        isPlaying = true
        binding.playPauseBtnPA.setIconResource(R.drawable.round_pause_24)
        musicService!!.mediaPlayer!!.start()

    }

    private fun pauseMusic() {
        isPlaying = false
        binding.playPauseBtnPA.setIconResource(R.drawable.round_play)

        musicService!!.mediaPlayer!!.pause()


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
    private fun setSongPosition(increment: Boolean) {

        if(!repeat){
            if (increment) {
                if (reMusicListPA.size - 1 == songPosition)
                    songPosition = 0
                else ++songPosition
            } else {
                if (0 == songPosition)
                    songPosition = reMusicListPA.size - 1
                else --songPosition
            }

        }
    }


    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (service is MusicService.MyBinder) {
            val binder = service as MusicService.MyBinder
            musicService = binder.currentService()
//            isServiceBound = true
        }
        createMediaPlayer()
        musicService!!.reSeekSetup()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        isServiceBound = null
    }


    override fun onResume() {
        super.onResume()
        mAdView = findViewById(R.id.adView)
        // banner ads
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }
    override fun onCompletion(p0: MediaPlayer?) {
        setSongPosition(increment = true)
        createMediaPlayer()
        try {
            setLayout()
        }catch (e: Exception){return}
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 13 || resultCode == RESULT_OK)
            return
    }

    fun loadAppOpenAd() {
        if (!isAdDisplayed) {
            val adRequest = AdRequest.Builder().build()
            AppOpenAd.load(
                this,
                "ca-app-pub-3504589383575544/8514536729",
                adRequest,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                appOpenAdLoadCallback
            )
        }
    }

    private val appOpenAdLoadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {
        override fun onAdLoaded(ad: AppOpenAd) {
            appOpenAd = ad
            appOpenAd!!.show(this@ReMusicPlayerActivity)
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
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.anim_no_change ,R.anim.slide_out_bottom
        )
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

}

private fun MediaPlayer.setOnCompletionListener(companion: ReMusicPlayerActivity.Companion) {


}