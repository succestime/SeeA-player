package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.MainActivity.Companion.folderList
import com.jaidev.seeaplayer.MainActivity.Companion.sortValue
import com.jaidev.seeaplayer.allAdapters.VideoAdapter
import com.jaidev.seeaplayer.dataClass.Folder
import com.jaidev.seeaplayer.dataClass.NaturalOrderComparator
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.databinding.ActivityFoldersBinding
import java.io.File

class FoldersActivity : AppCompatActivity(), VideoAdapter.VideoDeleteListener ,  VideoAdapter.OnFileCountChangeListener {
    private lateinit var binding: ActivityFoldersBinding
    private lateinit var adapter: VideoAdapter
    private var isSearchViewClicked = false
    private lateinit var searchView: SearchView
    private var currentLayoutManager: RecyclerView.LayoutManager? = null
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val PREF_LAYOUT_TYPE = "pref_layout_type"
    private val LAYOUT_TYPE_GRID = "grid"
    private val LAYOUT_TYPE_LIST = "list"
    private var hasRefreshed = false // Flag to track if a swipe-to-refresh has occurred

    companion object {
        var currentFolderVideos: ArrayList<VideoData> = arrayListOf()

    }

    @SuppressLint("SetTextI18n", "SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoldersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MobileAds.initialize(this) {}

        val position = intent.getIntExtra("position", 0)
        currentFolderVideos = getAllVideos(MainActivity.folderList[position].id)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        // Set the supportActionBar title
        val folderName = MainActivity.folderList[position].folderName.capitalize()
        supportActionBar?.title = if (folderName.isNullOrEmpty()) "Internal memory" else folderName

        binding.videoRVFA.setHasFixedSize(true)
        binding.videoRVFA.setItemViewCacheSize(15)
        adapter = VideoAdapter(this@FoldersActivity, currentFolderVideos, isFolder = true , this)
        binding.videoRVFA.adapter = adapter

        binding.totalVideo.text = "${currentFolderVideos.size} Videos"

        adapter = VideoAdapter(this@FoldersActivity, MainActivity.videoList, isFolder = true , this)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.setItemViewCacheSize(10)
        binding.recyclerView.layoutManager = LinearLayoutManager(this@FoldersActivity)
        binding.recyclerView.visibility = View.GONE
        binding.recyclerView.adapter = adapter

        val savedLayoutType = getSharedPreferences("LayoutPrefs", Context.MODE_PRIVATE)
            .getString(PREF_LAYOUT_TYPE, LAYOUT_TYPE_LIST)
        if (savedLayoutType == LAYOUT_TYPE_GRID) {
            setGridLayoutManager()
        } else {
            setListLayoutManager()
        }

        initializeBinding()
        toggleLayoutManager()
        setActionBarGradient()
        swipeRefreshLayout = binding.swipeRefreshFolder
        setSwipeRefreshBackgroundColor()




    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun initializeBinding() {
        val position = intent.getIntExtra("position", 0)
        binding.swipeRefreshFolder.setOnRefreshListener {
            if (adapter.isSelectionModeEnabled()) {
                hasRefreshed = false // Set the flag to true after a refresh

                binding.swipeRefreshFolder.isRefreshing = false
            } else {
                hasRefreshed = true // Set the flag to true after a refresh
                currentFolderVideos = getAllVideos(MainActivity.folderList[position].id)
                adapter = VideoAdapter(this@FoldersActivity, currentFolderVideos, isFolder = true, this)
                binding.videoRVFA.adapter = adapter
                binding.totalVideo.text = "${currentFolderVideos.size} Videos"
                binding.swipeRefreshFolder.isRefreshing = false
            }
        }
        binding.nowPlayingBtn.setOnClickListener {
            startPlayerActivity()
        }

        binding.gridBtn.setOnClickListener {
            adapter.enableGridMode(true)
            setGridLayoutManager()
            // Clear selection and reset action mode
        }

        binding.listBtn.setOnClickListener {
            adapter.enableGridMode(false)
            setListLayoutManager()
        }
    }

    private fun setGridLayoutManager() {
        val gridLayoutManager = GridLayoutManager(this, 2)
        binding.videoRVFA.layoutManager = gridLayoutManager
        binding.gridBtn.visibility = View.GONE
        binding.listBtn.visibility = View.VISIBLE
        saveLayoutType(LAYOUT_TYPE_GRID)
    }

    private fun setListLayoutManager() {
        val linearLayoutManager = LinearLayoutManager(this)
        binding.videoRVFA.layoutManager = linearLayoutManager
        binding.listBtn.visibility = View.GONE
        binding.gridBtn.visibility = View.VISIBLE
        saveLayoutType(LAYOUT_TYPE_LIST)
    }

    private fun toggleLayoutManager() {
        currentLayoutManager?.let {
            if (it is GridLayoutManager) {
                setListLayoutManager()
            } else if (it is LinearLayoutManager) {
                setGridLayoutManager()
            }
        }
    }

    private fun saveLayoutType(layoutType: String) {
        getSharedPreferences("LayoutPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_LAYOUT_TYPE, layoutType)
            .apply()
    }

    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            swipeRefreshLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)
        } else {
            swipeRefreshLayout.setBackgroundColor(resources.getColor(android.R.color.white))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onVideoDeleted() {
        MainActivity.videoList.clear()
        val position = intent.getIntExtra("position", 0)
        currentFolderVideos = getAllVideos(MainActivity.folderList[position].id)
        adapter.updateList(currentFolderVideos)
        binding.totalVideo.text = "${currentFolderVideos.size} Videos"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sort_view, menu)
        menuInflater.inflate(R.menu.search_music_view, menu)

        val searchItem = menu.findItem(R.id.searchMusicView)
        val searchView = searchItem?.actionView as SearchView
        val sortOrderMenuItem = menu.findItem(R.id.sortOrder)
        sortOrderMenuItem.setOnMenuItemClickListener { item ->
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
                    var value = sortValue
                    val dialog = MaterialAlertDialogBuilder(this)
                        .setTitle("Sort By")
                        .setPositiveButton("OK") { _, _ ->
                            val sortEditor = getSharedPreferences("Sorting", MODE_PRIVATE).edit()
                            sortEditor.putInt("sortValue", value)
                            sortEditor.apply()

                            finish()
                            startActivity(intent)
                        }
                        .setSingleChoiceItems(menuItems, sortValue) { _, pos ->
                            value = pos
                        }
                        .create()
                    dialog.show()
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.BLACK)
                    true
                }
                else -> false
            }
        }
        searchView.setOnCloseListener {
            isSearchViewClicked = false
            binding.recyclerView.visibility = View.GONE
            false
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                MainActivity.searchList = ArrayList()
                if (newText != null) {
                    val queryText = newText.lowercase()
                    for (video in MainActivity.videoList) {
                        if (video.title.lowercase().contains(queryText)) {
                            MainActivity.searchList.add(video)
                        }
                    }
                    MainActivity.search = true
                    adapter.updateList(searchList = MainActivity.searchList)
                }
                if (isSearchViewClicked || newText?.isNotEmpty() == true) {
                    binding.recyclerView.visibility = View.VISIBLE
                } else {
                    binding.recyclerView.visibility = View.GONE
                }
                return true
            }
        })

        val profileMenuItem = menu.findItem(R.id.profile)
        profileMenuItem.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.profile -> {
                    startActivity(Intent(this@FoldersActivity, More::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
                    true
                }
                else -> false
            }
        }
        return true
    }

    @SuppressLint("Range")
    fun getAllVideos(folderId: String): ArrayList<VideoData> {
        val sortEditor = getSharedPreferences("Sorting", MODE_PRIVATE)
        sortValue = sortEditor.getInt("sortValue", 0)
        val selection = "${MediaStore.Video.Media.BUCKET_ID} = ?"
        val tempList = ArrayList<VideoData>()
        val folderMap = mutableMapOf<String, String>()  // Map to hold folder ID and name association

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
            MainActivity.sortList[sortValue]
        )
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val titleC = it.getString(it.getColumnIndex(MediaStore.Video.Media.TITLE)) ?: ""
                    val idC = it.getString(it.getColumnIndex(MediaStore.Video.Media._ID)) ?: ""
                    val folderC = it.getString(it.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)) ?: ""
                    val folderIdC = it.getString(it.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)) ?: ""
                    val sizeC = it.getString(it.getColumnIndex(MediaStore.Video.Media.SIZE)) ?: ""
                    val pathC = it.getString(it.getColumnIndex(MediaStore.Video.Media.DATA)) ?: ""
                    val durationC = it.getString(it.getColumnIndex(MediaStore.Video.Media.DURATION))?.toLong() ?: 0L
                    val dateAddedMillis = it.getLong(it.getColumnIndex(MediaStore.Video.Media.DATE_ADDED))

                    try {
                        val file = File(pathC)
                        val artUriC = Uri.fromFile(file)
                        val currentTimestamp = System.currentTimeMillis()
                        val isNewVideo = currentTimestamp - dateAddedMillis <= DateUtils.DAY_IN_MILLIS

                        val video = VideoData(
                            title = titleC,
                            id = idC,
                            folderName = folderC,
                            duration = durationC,
                            size = sizeC,
                            path = pathC,
                            artUri = artUriC,
                            dateAdded = dateAddedMillis,
                            isNew = isNewVideo
                        )

                        if (file.exists()) tempList.add(video)

                        // Ensure folder is added to MainActivity.folderList
                        if (folderList.none { it.id == folderIdC }) {
                            folderList.add(Folder(id = folderIdC, folderName = folderMap[folderIdC] ?: folderC.ifEmpty { "Internal memory"
                             }))

                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } while (it.moveToNext())
            }
        }

        if (sortValue == 2 || sortValue == 3) {
            tempList.sortWith(Comparator { o1, o2 ->
                val result = NaturalOrderComparator().compare(o1.title, o2.title)
                if (sortValue == 3) -result else result
            })
        }
        // Sort the folderList using NaturalOrderComparator
        if (sortValue == 2 || sortValue == 3) { // Name(A to Z) or Name(Z to A)
            folderList.sortWith(Comparator { o1, o2 ->
                val result = NaturalOrderComparator().compare(o1.folderName, o2.folderName)
                if (sortValue == 3) -result else result
            })
        }

        return tempList
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setActionBarGradient() {
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@FoldersActivity,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@FoldersActivity,
                        R.drawable.background_actionbar
                    )
                )
            }
        } else {
            val isSystemDefaultDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
                android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
                else -> false
            }
            if (isSystemDefaultDarkMode) {
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@FoldersActivity,
                            R.drawable.background_actionbar
                        )
                    )
                }
            } else {
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@FoldersActivity,
                            R.drawable.background_actionbar_light
                        )
                    )
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged", "SuspiciousIndentation", "SetTextI18n")
    override fun onResume() {
        super.onResume()
        setActionBarGradient()
        if (PlayerActivity.position != -1) binding.nowPlayingBtn.visibility = View.VISIBLE
        if (MainActivity.adapterChanged) adapter.notifyDataSetChanged()
        MainActivity.adapterChanged = false
        binding.totalVideo.text = "${currentFolderVideos.size} Videos"
        // Enable or disable swipe-to-refresh based on selection mode and refresh flag
        binding.swipeRefreshFolder.isEnabled = !adapter.isSelectionModeEnabled() || hasRefreshed
    }

    private fun startPlayerActivity() {
        val intent = Intent(this@FoldersActivity, PlayerActivity::class.java)
        intent.putExtra("class", "NowPlaying")
        startActivity(intent)
    }

    @SuppressLint("SetTextI18n")
    override fun onFileCountChanged(newCount: Int) {
        binding.totalVideo.text = "$newCount Videos"

    }


}
