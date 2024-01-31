package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.databinding.ActivityFoldersBinding
import java.io.File

class FoldersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFoldersBinding
    lateinit var adapter: VideoAdapter
    private var isSearchViewClicked = false
companion object {
    lateinit var currentFolderVideos: ArrayList<VideoData>
}

    @SuppressLint("SetTextI18n", "SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoldersBinding.inflate(layoutInflater)
        setTheme(R.style.coolBlueNav)
        setContentView(binding.root)
        val position = intent.getIntExtra("position", 0)

     currentFolderVideos = getAllVideos(MainActivity.folderList[position].id)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = MainActivity.folderList[position].folderName
        binding.videoRVFA.setHasFixedSize(true)
        binding.videoRVFA.setItemViewCacheSize(10)
        binding.videoRVFA.layoutManager = LinearLayoutManager(this@FoldersActivity)
        adapter = VideoAdapter(this@FoldersActivity,currentFolderVideos, isFolder = true,)
        binding.videoRVFA.adapter = adapter
        binding.totalVideo.text = "Total Video : ${currentFolderVideos.size}"

        adapter = VideoAdapter(this@FoldersActivity, MainActivity.videoList)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.setItemViewCacheSize(10)
        binding.recyclerView.layoutManager = LinearLayoutManager(this@FoldersActivity)
        binding.recyclerView.visibility = View.GONE
        binding.recyclerView.adapter = adapter

        binding.swipeRefreshFolder.setOnRefreshListener {
            // This block of code will be executed when the user swipes to refresh
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
        binding.searchBackBtn.setOnClickListener {
            binding.recyclerView.visibility = View.GONE
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sort_view, menu)
        menuInflater.inflate(R.menu.search_view_menu, menu)
        val searchItem = menu.findItem(R.id.searchView)
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
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    MainActivity.searchList = ArrayList()
                    for (video in MainActivity.videoList) {
                        if (video.title.lowercase().contains(newText.lowercase()))
                            MainActivity.searchList.add(video)
                    }
                    MainActivity.search = true
                    adapter.updateList(searchList = MainActivity.searchList)
                }
                binding.recyclerView.visibility =  if (MainActivity.searchList.isNotEmpty()) View.VISIBLE else View.GONE
                return true
            }
        })
        searchView.setOnCloseListener {
            isSearchViewClicked = false
            binding.recyclerView.visibility = View.GONE
            false
        }
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

                    try {
                        val file = File(pathC)
                        val artUriC = Uri.fromFile(file)
                        val video = VideoData(
                            title = titleC, id = idC, folderName = folderC, duration = durationC,
                            path = pathC, size = sizeC, artUri = artUriC
                        )
                        if (file.exists()) tempList.add(video)

                    } catch (_: Exception) {
                    }
                } while (cursor.moveToNext())
        cursor?.close()
        return tempList
    }

    @SuppressLint("NotifyDataSetChanged", "SuspiciousIndentation")
    override fun onResume() {
        super.onResume()
        if(PlayerActivity.position != -1) binding.nowPlayingBtn.visibility = View.VISIBLE
//           if (MainActivity.dataChanged) adapter.notifyDataSetChanged()
//               MainActivity.dataChanged = false



    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
//    @Deprecated("Deprecated in Java")
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        adapter.onResult(requestCode, resultCode)
//    }
}


