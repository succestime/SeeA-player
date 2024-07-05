

package com.jaidev.seeaplayer.recantFragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.jaidev.seeaplayer.MainActivity.Companion.videoRecantList
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.RecentVideoAdapter
import com.jaidev.seeaplayer.dataClass.RecantVideo
import com.jaidev.seeaplayer.databinding.FragmentDaysDownloadBinding
import java.util.concurrent.TimeUnit
// Import statements go here

class DaysDownload : Fragment(),   RecentVideoAdapter.OnFileCountChangeListener {
    private lateinit var binding: FragmentDaysDownloadBinding
    lateinit var adapter: RecentVideoAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    companion object {
        lateinit var currentFolderVideos: ArrayList<RecantVideo>
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_days_download, container, false)
        binding = FragmentDaysDownloadBinding.bind(view)

        binding.DownloadRV.setHasFixedSize(true)
        binding.DownloadRV.setItemViewCacheSize(13)
        binding.DownloadRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = RecentVideoAdapter(requireContext(), videoRecantList, isRecantVideo = true , this)
        binding.DownloadRV.adapter = adapter
        binding.daysTotalVideos.text = "0 Videos "

        if (!requestRuntimePermission()) {
            // Permission not granted yet
            return view
        }


        loadRecentVideos()

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            binding.swipeRefreshVideo.isRefreshing = false // Hide the refresh indicator
        }, 2000) // 2000 milliseconds (2 seconds)

// Set up SwipeRefreshLayout
        swipeRefreshLayout = binding.swipeRefreshVideo

        swipeRefreshLayout.setOnRefreshListener {
            loadRecentVideos()
            swipeRefreshLayout.isRefreshing = false
        }
        // Set the background color of SwipeRefreshLayout based on app theme
        return view
    }

    private fun requestRuntimePermission(): Boolean {
        // Check for permission based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_VIDEO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
                    13
                )
                return false
            }
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    13
                )
                return false
            }
        } else {
            // For Android versions >= Q (API 29)
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    14
                )
                return false
            }
        }
        return true
    }

    // Handle permission request results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 13) {
            // Handle WRITE_EXTERNAL_STORAGE or READ_MEDIA_VIDEO permission request result
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, load recent videos
                loadRecentVideos()
            } else {
                // Permission denied, show a message or retry request
                Snackbar.make(binding.root, "Storage Permission Needed!!", Snackbar.LENGTH_LONG)
                    .show()
            }
        } else if (requestCode == 14) {
            // Handle READ_EXTERNAL_STORAGE permission request result
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, load recent videos
                loadRecentVideos()
            } else {
                // Permission denied, show a message or retry request
                Snackbar.make(binding.root, "Storage Permission Needed!!", Snackbar.LENGTH_LONG)
                    .show()
            }
        }
    }





    @SuppressLint("SetTextI18n")
    private fun loadRecentVideos() {
        val recantVideos = getAllRecantVideos(requireContext())
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val sortedRecentVideos = recantVideos.filter { it.timestamp >= sevenDaysAgo }
        recantVideos.sortedByDescending { it.timestamp }

        adapter.updateRecentVideos(sortedRecentVideos)
        binding.daysTotalVideos.text = "${sortedRecentVideos.size} Videos"

        if (videoRecantList.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
        }

    }



    private fun getAllRecantVideos(context: Context): ArrayList<RecantVideo> {
        val recantVList = ArrayList<RecantVideo>()
        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val titleC = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val idC = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val durationC = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val timestampC = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val pathC = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val sizeC = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            while (it.moveToNext()) {
                val title = it.getString(titleC)
                val id = it.getString(idC)
                val size = it.getString(sizeC)
                val duration = it.getLong(durationC)
                val timestamp = it.getLong(timestampC) * 1000
                val path = it.getString(pathC)
                val artUri = Uri.parse("content://media/external/video/media/$id")

                val video = RecantVideo(title, timestamp, id, duration, path, artUri, size)
                recantVList.add(video)
            }
        }

        return recantVList
    }

    @SuppressLint("SetTextI18n")
    override fun onFileCountChanged(newCount: Int) {
        binding.daysTotalVideos.text = "$newCount Videos"
    }

}


