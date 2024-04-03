package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.DateUtils
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.databinding.ActivityFoldersBinding
import java.io.File

class FoldersActivity : AppCompatActivity(),VideoAdapter.VideoDeleteListener{
    private lateinit var binding: ActivityFoldersBinding
    private lateinit var adapter: VideoAdapter
    private var isSearchViewClicked = false
    private lateinit var searchView: SearchView

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    companion object {
        lateinit var currentFolderVideos: ArrayList<VideoData>
    }

    @SuppressLint("SetTextI18n", "SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoldersBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val position = intent.getIntExtra("position", 0)
        currentFolderVideos = getAllVideos(MainActivity.folderList[position].id)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = MainActivity.folderList[position].folderName
        binding.videoRVFA.setHasFixedSize(true)
        binding.videoRVFA.setItemViewCacheSize(10)
        binding.videoRVFA.layoutManager = LinearLayoutManager(this@FoldersActivity)
        adapter = VideoAdapter(this@FoldersActivity,currentFolderVideos, isFolder = true)
        binding.videoRVFA.adapter = adapter
        binding.totalVideo.text = "Total Video : ${currentFolderVideos.size}"


        adapter = VideoAdapter(this@FoldersActivity, MainActivity.videoList)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.setItemViewCacheSize(10)
        binding.recyclerView.layoutManager = LinearLayoutManager(this@FoldersActivity)
        binding.recyclerView.visibility = View.GONE
        binding.recyclerView.adapter = adapter

        binding.swipeRefreshFolder.setOnRefreshListener {

            currentFolderVideos = getAllVideos(MainActivity.folderList[position].id)
            adapter = VideoAdapter(this@FoldersActivity, currentFolderVideos, isFolder = true)
            binding.videoRVFA.adapter = adapter
            binding.totalVideo.text = "Total Video : ${currentFolderVideos.size}"
            binding.swipeRefreshFolder.isRefreshing = false // Hide the refresh indicator

        }

        binding.nowPlayingBtn.setOnClickListener {
            val intent = Intent(this@FoldersActivity, PlayerActivity::class.java)
            intent.putExtra("class", "NowPlaying")
            startActivity( intent )
        }

//        binding.searchBackBtn.setOnClickListener {
//            binding.recyclerView.visibility = View.GONE
//            binding.searchBackBtn.visibility = View.GONE
//        }
        setActionBarGradient()
        swipeRefreshLayout = binding.swipeRefreshFolder

        // Set the background color of SwipeRefreshLayout based on app theme
        setSwipeRefreshBackgroundColor()
    }

    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            swipeRefreshLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
        } else {
            // Light mode is enabled, set background color to white
            swipeRefreshLayout.setBackgroundColor(resources.getColor(android.R.color.white))
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onVideoDeleted() {

        val position = intent.getIntExtra("position", 0)
        currentFolderVideos = getAllVideos(MainActivity.folderList[position].id)
        adapter.updateList(currentFolderVideos)
        binding.totalVideo.text = "Total Video : ${currentFolderVideos.size}"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sort_view, menu)
        menuInflater.inflate(R.menu.search_music_view, menu)
        val searchItem = menu.findItem(R.id.searchMusicView)

        val searchView = searchItem?.actionView as SearchView
        val sortOrderMenuItem = menu.findItem(R.id.sortOrder)
        sortOrderMenuItem.setOnMenuItemClickListener { item ->
            // Handle the click event here
            when (item.itemId) {
                R.id.sortOrder -> {
                    val menuItems = arrayOf(
                        "Latest",
                        "Oldest",
                        "Name(A to Z)",
                        "Name(Z to A)",
                        "File Size(Smallest)",
                        "File Size(Largest)"
                    )
                    var value = MainActivity.sortValue
                    val dialog = MaterialAlertDialogBuilder(this)
                        .setTitle("Sort By")
                        .setPositiveButton("OK") { _, _ ->
                            val sortEditor = getSharedPreferences("Sorting", MODE_PRIVATE).edit()
                            sortEditor.putInt("sortValue", value)
                            sortEditor.apply()

                            //for restarting app
                            finish()
                            startActivity(intent)

                        }
                        .setSingleChoiceItems(menuItems, MainActivity.sortValue) { _, pos ->
                            value = pos
                        }
                        .create()
                    dialog.show()
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.RED)
                    true
                }


                else -> false
            }
        }
        searchView.setOnCloseListener {
            isSearchViewClicked = false
            binding.recyclerView.visibility = View.GONE
//            binding.searchBackBtn.visibility = View.GONE

            false
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                MainActivity.searchList = ArrayList()
                if (newText != null) {

                    val queryText = newText.lowercase()
                    for (video in MainActivity.videoList) {
                        // Filter videos based on the user's input
                        if (video.title.lowercase().contains(queryText)) {
                            MainActivity.searchList.add(video)
//                            binding.searchBackBtn.visibility = View.VISIBLE
                        }

                    }
                    MainActivity.search = true
                    adapter.updateList(searchList = MainActivity.searchList)

                }
                // Check if the search view is clicked or if there is text in the search view
                if (isSearchViewClicked || newText?.isNotEmpty() == true) {
                    binding.recyclerView.visibility = View.VISIBLE
//                    binding.searchBackBtn.visibility = View.VISIBLE

                } else {
                    binding.recyclerView.visibility = View.GONE
//                    binding.searchBackBtn.visibility = View.GONE

                }

                return true
            }
        })

        return true
    }

    @SuppressLint("Range")
    fun getAllVideos(folderId: String): ArrayList<VideoData> {
        val sortEditor = getSharedPreferences("Sorting", MODE_PRIVATE)
        MainActivity.sortValue = sortEditor.getInt("sortValue", 0)

        val tempList = ArrayList<VideoData>()
        val selection = MediaStore.Video.Media.BUCKET_ID + " like? "
        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID
        )
        val cursor = this.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, arrayOf(folderId),
            MainActivity.sortList[MainActivity.sortValue]
        )
        if (cursor != null)
            if (cursor.moveToNext())
                do {
                    val titleC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                    val folderC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                    val sizeC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))
                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                    val durationC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))
                            .toLong()
                    val dateAddedMillis = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED))


                    try {
                        val file = File(pathC)
                        val artUriC = Uri.fromFile(file)
                        val currentTimestamp = System.currentTimeMillis()
                        val isNewVideo = currentTimestamp - dateAddedMillis <= DateUtils.DAY_IN_MILLIS

                        val video = VideoData(
                            title = titleC, id = idC, folderName = folderC, duration = durationC,
                            path = pathC, size = sizeC, artUri = artUriC, dateAdded = dateAddedMillis, isNew =isNewVideo
                        )


                        if (file.exists()) tempList.add(video)

                    } catch (_: Exception) {
                    }
                } while (cursor.moveToNext())
        cursor?.close()
        return tempList
    }



    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    private fun setActionBarGradient() {
        // Check the current night mode
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            // Light mode is applied
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                       this@FoldersActivity,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else {
            // Dark mode is applied or the mode is set to follow system
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                       this@FoldersActivity,
                        R.drawable.background_actionbar
                    )
                )
            }
        }
    }
    @SuppressLint("NotifyDataSetChanged", "SuspiciousIndentation")
    override fun onResume() {
        super.onResume()
        setActionBarGradient()
        if(PlayerActivity.position != -1) binding.nowPlayingBtn.visibility = View.VISIBLE
        if (MainActivity.adapterChanged) adapter.notifyDataSetChanged()
        MainActivity.adapterChanged= false
    }
}
