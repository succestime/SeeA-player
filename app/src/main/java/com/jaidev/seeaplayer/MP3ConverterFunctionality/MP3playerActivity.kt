
package com.jaidev.seeaplayer.MP3ConverterFunctionality

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.arthenica.mobileffmpeg.FFmpeg
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jaidev.seeaplayer.Services.MP3Service
import com.jaidev.seeaplayer.allAdapters.MP3ConvertingAdapter.MP3QueueAdapter
import com.jaidev.seeaplayer.dataClass.MP3Data.MP3FileData
import com.jaidev.seeaplayer.dataClass.formatDuration
import com.jaidev.seeaplayer.dataClass.MP3Data.setMP3SongPosition
import com.jaidev.seeaplayer.databinding.ActivityMp3playerBinding
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.isPlaying
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.nowMusicPlayingId
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.songPosition
import java.io.File

class MP3playerActivity : AppCompatActivity(), ServiceConnection, MediaPlayer.OnCompletionListener {

    companion object{
        lateinit var mp3MusicPA : ArrayList<MP3FileData>
        var musicMP3Service: MP3Service? = null
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityMp3playerBinding
        @SuppressLint("StaticFieldLeak")
        lateinit var mp3Adapter: MP3QueueAdapter

    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag", "ObsoleteSdkInt", "NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMp3playerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        initializeLayout()

        // Initialize the adapter with the scroll to position and click callback
        mp3Adapter = MP3QueueAdapter(
            this,
            MainActivity.MP3MusicList,
            songPosition,
            { position -> scrollToPositionIfNotVisible(position) }, // Center the current item
            { position -> onSongSelected(position) } // Handle the song click
        )

        binding.MP3ListQueue.adapter = mp3Adapter
        binding.MP3ListQueue.layoutManager = LinearLayoutManager(this)

        // Scroll to the currently playing song if it's not visible
        scrollToPositionIfNotVisible(songPosition)

//        val callback = MP3QueueTouchHelperCallback(mp3Adapter)
//        val touchHelper = ItemTouchHelper(callback)
//        touchHelper.attachToRecyclerView(binding.MP3ListQueue)


        mp3Adapter.notifyDataSetChanged()

//        restoreScrollPosition()

        binding.shareMP3Layout.setOnClickListener {
          if (mp3MusicPA.isNotEmpty()) {
          val currentMP3File = mp3MusicPA[songPosition]
           shareMP3File(currentMP3File)
            }

         }
        binding.setAsRingtoneLayout.setOnClickListener {
            if (mp3MusicPA.isNotEmpty()) {

                showRingtoneBottomSheet()
            }
        }
        binding.roundForward10.setOnClickListener {
            musicMP3Service?.let {
                val currentPosition = it.mediaPlayer?.currentPosition ?: 0
                val newPosition = currentPosition + 10000 // Forward by 10 seconds (10,000 milliseconds)
                it.mediaPlayer?.seekTo(newPosition.coerceAtMost(it.mediaPlayer?.duration ?: 0)) // Ensure it doesn't exceed duration
                it.showNotification(R.drawable.round_pause_circle_outline_24)
            }
        }

        binding.roundReplay10.setOnClickListener {
            musicMP3Service?.let {
                val currentPosition = it.mediaPlayer?.currentPosition ?: 0
                val newPosition = currentPosition - 10000 // Rewind by 10 seconds (10,000 milliseconds)
                it.mediaPlayer?.seekTo(newPosition.coerceAtLeast(0)) // Ensure it doesn't go below 0
                it.showNotification(R.drawable.round_pause_circle_outline_24)
            }
        }


        binding.playPauseBtn.setOnClickListener {
            if (isPlaying) {
                pauseMusic()
            } else {
                playMusic()
            }
        }
        // Set up the navigation icon click listener to finish the activity
        binding.playlistToolbar.setNavigationOnClickListener {
            finish()
        }

        // Set up menu item click listener

        binding.nextBtn.setOnClickListener { prevNextSong(true) }
        binding.pervBtn.setOnClickListener { prevNextSong(false) }


        binding.seekBarMPA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicMP3Service!!.mediaPlayer!!.seekTo(progress)

                    musicMP3Service!!.showNotification(if (isPlaying) R.drawable.round_pause_circle_outline_24 else R.drawable.round_play_circle_outline_24)
                }
            }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    })
    }
    // Method to ensure the current playing song is visible on activity creation
    private fun scrollToPositionIfNotVisible(position: Int) {
        binding.MP3ListQueue.post {
            val layoutManager = binding.MP3ListQueue.layoutManager as LinearLayoutManager
            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
            val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

            if (position < firstVisiblePosition || position > lastVisiblePosition) {
                // Scroll smoothly to the position if it's not visible
                binding.MP3ListQueue.smoothScrollToPosition(position)
            } else {
                // Optionally adjust with offset if partially visible
                val offset = binding.MP3ListQueue.height / 2 - (binding.MP3ListQueue.getChildAt(0)?.height ?: 0) / 2
                layoutManager.scrollToPositionWithOffset(position, offset)
            }
        }
    }


    private fun onSongSelected(position: Int) {
        songPosition = position
        mp3Adapter.updateCurrentSongPosition(songPosition) // Update the adapter with the new song position
        setLayout() // Update the UI with the new song details
        createMediaPlayer() // Prepare and start playing the new song
    }


//    override fun onPause() {
//        super.onPause()
//        saveScrollPosition()
//    }
//
//    override fun onStop() {
//        super.onStop()
//        saveScrollPosition()
//    }
//
//    private fun saveScrollPosition() {
//        // Save the current scroll position of the RecyclerView
//        val layoutManager = binding.MP3ListQueue.layoutManager as LinearLayoutManager
//        savedScrollPosition = layoutManager.findFirstVisibleItemPosition()
//        getPreferences(Context.MODE_PRIVATE).edit()
//            .putInt(SCROLL_POSITION_KEY, savedScrollPosition)
//            .apply()
//    }
//
//    private fun restoreScrollPosition() {
//        // Restore the saved scroll position of the RecyclerView
//        val layoutManager = binding.MP3ListQueue.layoutManager as LinearLayoutManager
//        savedScrollPosition = getPreferences(Context.MODE_PRIVATE).getInt(SCROLL_POSITION_KEY, 0)
//        layoutManager.scrollToPosition(savedScrollPosition)
//    }


    private fun prevNextSong(increment: Boolean) {
        if (increment) {
            setMP3SongPosition(increment = true)
            mp3Adapter.updateCurrentSongPosition(songPosition) // Update adapter
            setLayout()
            createMediaPlayer()
        } else {
            setMP3SongPosition(increment = false)
            mp3Adapter.updateCurrentSongPosition(songPosition) // Update adapter
            setLayout()
            createMediaPlayer()
        }
    }
    private fun playMusic() {
        isPlaying = true
        musicMP3Service!!.mediaPlayer!!.start()
    binding.playPauseBtn.setImageResource(R.drawable.round_pause_circle_outline_24)
        musicMP3Service!!.showNotification(R.drawable.round_pause_circle_outline_24)

    }

    private fun pauseMusic() {
        isPlaying = false
        musicMP3Service!!.mediaPlayer!!.pause()
    binding.playPauseBtn.setImageResource(R.drawable.round_play_circle_outline_24)
        musicMP3Service!!.showNotification(R.drawable.round_play_circle_outline_24)

    }
    private fun initializeLayout(){
        songPosition = intent.getIntExtra("index" , 0)
        when (intent.getStringExtra("class")) {
            "MP3AudioList" -> {
                val intent = Intent(this, MP3Service::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                mp3MusicPA = ArrayList()
                mp3MusicPA.addAll(MainActivity.MP3MusicList)
                setLayout()


            }
            "MP3NowPlaying" -> {
                setLayout()
                binding.remainingTimeLabelStart.text = formatDuration(musicMP3Service!!.mediaPlayer!!.currentPosition.toLong())
                binding.elapsedTimeLabelEnd.text = formatDuration(musicMP3Service!!.mediaPlayer!!.duration.toLong())
                binding.seekBarMPA.progress = musicMP3Service!!.mediaPlayer!!.currentPosition
                binding.seekBarMPA.max = musicMP3Service!!.mediaPlayer!!.duration
                if(isPlaying) binding.playPauseBtn.setImageResource(R.drawable.round_pause_circle_outline_24)
                else   binding.playPauseBtn.setImageResource(R.drawable.round_play_circle_outline_24)
            }
        }
    }


    private fun setLayout(){
        Glide.with(applicationContext)
            .load(mp3MusicPA[songPosition].artUri)
            .into(binding.playerActivityMP3Image)
        binding.playerActivityMP3Image.foreground = ContextCompat.getDrawable(this@MP3playerActivity,
            R.drawable.gray_overlay
        )

        binding.SongTitle.text = mp3MusicPA[songPosition].title


    }
    @SuppressLint("NotifyDataSetChanged")
private fun createMediaPlayer(){
    try {
        if (  musicMP3Service!!.mediaPlayer == null)   musicMP3Service!!.mediaPlayer = MediaPlayer()
        musicMP3Service!!.mediaPlayer!!.reset()
        musicMP3Service!!.mediaPlayer!!.setDataSource(mp3MusicPA[songPosition].path)
        musicMP3Service!!.mediaPlayer!!.prepare()
        musicMP3Service!!.mediaPlayer!!.start()
        isPlaying = true
        binding.playPauseBtn.setImageResource(R.drawable.round_pause_circle_outline_24)
      binding.remainingTimeLabelStart.text = formatDuration(musicMP3Service!!.mediaPlayer!!.currentPosition.toLong())
        binding.elapsedTimeLabelEnd.text = formatDuration(musicMP3Service!!.mediaPlayer!!.duration.toLong())
        binding.seekBarMPA.progress = 0
        binding.seekBarMPA.max = musicMP3Service!!.mediaPlayer!!.duration
        musicMP3Service!!.showNotification(R.drawable.round_pause_circle_outline_24)
        musicMP3Service!!.mediaPlayer!!.setOnCompletionListener(this)
    nowMusicPlayingId = mp3MusicPA[songPosition].id

    }catch (e: Exception){
        return
    }
}

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {

            val binder = service as MP3Service.MyBinder
            musicMP3Service = binder.currentService()
        musicMP3Service!!.seekBarSetup()
        mp3Adapter.updateCurrentSongPosition(songPosition) // Update adapter

        createMediaPlayer()

    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicMP3Service = null
    }
    @SuppressLint("ObsoleteSdkInt")
    private fun showRingtoneBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.ringtone_music_bottom_sheet, null)
        bottomSheetDialog.setContentView(view)


        // Store the reference to the TextView
      val  mp3TitleTextView = view.findViewById<TextView>(R.id.MP3Title)
        // Assuming mediaPlayerService has a method to get the current track title or MP3FileData
        val currentTrackTitle = mp3MusicPA[songPosition].title
        mp3TitleTextView?.text = currentTrackTitle ?: "Unknown Track"

        // Set up the bottom sheet options (e.g., Phone Ringtone, Notification Ringtone)
        view.findViewById<LinearLayout>(R.id.setAsPhoneRingtoneButton).setOnClickListener {
            val builderTone = AlertDialog.Builder(this)
            val dialogViewTone =
                LayoutInflater.from(this).inflate(R.layout.favurite_ringtone, null)
            builderTone.setView(dialogViewTone)

            val dialogTone = builderTone.create()

            val notButton: Button = dialogViewTone.findViewById(R.id.not_button)
            val yesButton: Button = dialogViewTone.findViewById(R.id.yes_button)

            notButton.setOnClickListener {
                dialogTone.dismiss()
            }

            // Inside yesButton.setOnClickListener
            yesButton.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.System.canWrite(this@MP3playerActivity)) {
                        // Request permission to write system settings
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    } else {
                        setAsRingtone(mp3MusicPA[songPosition].path)
                    }
                } else {
                    // For devices below Android 6.0
                    setAsRingtone(mp3MusicPA[songPosition].path)
                }
                dialogTone.dismiss()
                bottomSheetDialog.dismiss()
            }
            dialogTone.show()
            bottomSheetDialog.dismiss()
        }


        view.findViewById<LinearLayout>(R.id.setAsNotificationRingtoneButton).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(this@MP3playerActivity)) {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } else {
                    setAsNotificationTone(mp3MusicPA[songPosition].path)
                }
            } else {
                setAsNotificationTone(mp3MusicPA[songPosition].path)
            }
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }



    // Method to set the MP3 file as a ringtone
    private fun setAsRingtone(mp3FilePath: String) {
        val file = File(mp3FilePath)

        // Define the target location for the file in the Ringtones directory
        val ringtoneDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES), "SeeA MP3 Audio")
        if (!ringtoneDir.exists()) {
            ringtoneDir.mkdirs() // Create the directory if it doesn't exist
        }

        try {
            // Copy the file to the public Ringtones directory
            val newFile = File(ringtoneDir, file.name)
            file.copyTo(newFile, overwrite = true)

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, newFile.absolutePath)
                put(MediaStore.MediaColumns.TITLE, mp3MusicPA[songPosition].title)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                put(MediaStore.Audio.Media.IS_ALARM, false)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }

            val uri = MediaStore.Audio.Media.getContentUriForPath(newFile.absolutePath)
            val newUri = contentResolver.insert(uri!!, contentValues)

            RingtoneManager.setActualDefaultRingtoneUri(
                this,
                RingtoneManager.TYPE_RINGTONE,
                newUri
            )

            Toast.makeText(this, "Ringtone set to ${mp3MusicPA[songPosition].title}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to set ringtone: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setAsNotificationTone(mp3FilePath: String) {
        val file = File(mp3FilePath)

        // Define the target location for the file in the Notifications directory
        val notificationDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS), "SeeA MP3 Audio")
        if (!notificationDir.exists()) {
            notificationDir.mkdirs() // Create the directory if it doesn't exist
        }

        val trimmedFile = File(notificationDir, "trimmed_${file.name}")
        try {
            // Use FFmpeg to trim the audio to 2 seconds
            val command = arrayOf("-i", file.absolutePath, "-t", "00:00:03", "-c", "copy", trimmedFile.absolutePath)
            val rc = FFmpeg.execute(command)

            if (rc != 0) {
                throw Exception("FFmpeg failed with return code $rc")
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, trimmedFile.absolutePath)
                put(MediaStore.MediaColumns.TITLE, mp3MusicPA[songPosition].title)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.IS_RINGTONE, false)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, true)
                put(MediaStore.Audio.Media.IS_ALARM, false)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }

            val uri = MediaStore.Audio.Media.getContentUriForPath(trimmedFile.absolutePath)
            val newUri = contentResolver.insert(uri!!, contentValues)

            RingtoneManager.setActualDefaultRingtoneUri(
                this,
                RingtoneManager.TYPE_NOTIFICATION,
                newUri
            )

            Toast.makeText(this, "Notification tone set to ${mp3MusicPA[songPosition].title}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to set notification tone: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareMP3File(mp3FileData: MP3FileData) {
        val mp3File = File(mp3FileData.path)
        if (!mp3File.exists()) {
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mpeg"
            putExtra(Intent.EXTRA_STREAM, getUriForFile(mp3File))
            putExtra(Intent.EXTRA_SUBJECT, "Sharing ${mp3FileData.title}")
            putExtra(Intent.EXTRA_TEXT, "Listen to this audio file: ${mp3FileData.title}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share MP3 via"))
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun getUriForFile(file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build .VERSION_CODES.N) {
            FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                file
            )
        } else {
            Uri.fromFile(file)
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        setMP3SongPosition(increment = true)
        createMediaPlayer()
        setLayout()
        mp3Adapter.updateCurrentSongPosition(songPosition) // Update adapter

        // Refresh now playing image & text on song completion
        MP3NowPlaying.binding.songNameNP.isSelected = true
        Glide.with(applicationContext)
            .load(mp3MusicPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
            .into(MP3NowPlaying.binding.songImgNP)
        MP3NowPlaying.binding.songNameNP.text = mp3MusicPA[songPosition].title
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
            super.onBackPressed()
            overridePendingTransition(
                R.anim.anim_no_change, R.anim.slide_out_bottom
            )
        }


}
