package com.jaidev.seeaplayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaidev.seeaplayer.allAdapters.PlaylistVideoShowAdapter
import com.jaidev.seeaplayer.dataClass.DatabaseClient
import com.jaidev.seeaplayer.dataClass.PlaylistVideoCrossRef
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.dataClass.VideoEntity
import com.jaidev.seeaplayer.databinding.ActivityPlaylistVideoBinding
import kotlinx.coroutines.launch

class PlaylistVideoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlaylistVideoBinding
    private lateinit var videoAdapter: PlaylistVideoShowAdapter
    private val db by lazy { DatabaseClient.getInstance(this) }
    private var playlistId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        playlistId = intent.getLongExtra("playlistId", -1)
        if (playlistId == -1L) finish() // Invalid playlist ID

        setupRecyclerView()
        loadVideosFromDatabase()

        binding.addVideosButton.setOnClickListener {
            val bottomSheet = AddVideosBottomSheetFragment()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }
        setSwipeRefreshBackgroundColor()
    }

    private fun setupRecyclerView() {
        videoAdapter = PlaylistVideoShowAdapter(this, mutableListOf())
        binding.videoOfPlaylistRV.layoutManager = LinearLayoutManager(this)
        binding.videoOfPlaylistRV.adapter = videoAdapter
    }

    fun addSelectedVideos(selectedVideos: List<VideoData>) {
        lifecycleScope.launch {
            selectedVideos.forEach { video ->
                db.playlistDao().insertVideo(VideoEntity(
                    id = video.id,
                    title = video.title,
                    duration = video.duration,
                    folderName = video.folderName,
                    size = video.size,
                    path = video.path,
                    artUri = video.artUri.toString(),
                    dateAdded = video.dateAdded,
                    isNew = video.isNew,
                    isPlayed = video.isPlayed
                ))
                db.playlistDao().insertPlaylistVideoCrossRef(PlaylistVideoCrossRef(playlistId, video.id))
            }
            loadVideosFromDatabase()
        }
    }

    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)
        } else {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.from_left, R.anim.to_right)
    }

    private fun checkIfRecyclerViewIsEmpty() {
        if (videoAdapter.itemCount == 0) {
            binding.AddVideoLayout.visibility = View.VISIBLE
        } else {
            binding.AddVideoLayout.visibility = View.GONE
        }
    }

    private fun loadVideosFromDatabase() {
        lifecycleScope.launch {
            val playlistWithVideos = db.playlistDao().getPlaylistWithVideos(playlistId)
            val videoDataList = playlistWithVideos.videos.map { videoEntity ->
                VideoData(
                    id = videoEntity.id,
                    title = videoEntity.title,
                    duration = videoEntity.duration,
                    folderName = videoEntity.folderName,
                    size = videoEntity.size,
                    path = videoEntity.path,
                    artUri = Uri.parse(videoEntity.artUri),
                    dateAdded = videoEntity.dateAdded,
                    isNew = videoEntity.isNew,
                    isPlayed = videoEntity.isPlayed
                )
            }
            videoAdapter.updateVideoList(videoDataList)
            checkIfRecyclerViewIsEmpty()
        }
    }
}
