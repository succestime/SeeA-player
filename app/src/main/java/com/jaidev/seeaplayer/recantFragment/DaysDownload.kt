package com.jaidev.seeaplayer.recantFragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaidev.seeaplayer.MainActivity.Companion.videoRecantList
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.RecentVideoAdapter
import com.jaidev.seeaplayer.dataClass.RecantVideo
import com.jaidev.seeaplayer.databinding.FragmentDaysDownloadBinding
import java.util.concurrent.TimeUnit
// Import statements go here

class DaysDownload : Fragment() {
    private lateinit var binding: FragmentDaysDownloadBinding
    lateinit var adapter: RecentVideoAdapter


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
        adapter = RecentVideoAdapter(requireContext(), videoRecantList, isRecantVideo = true)
        binding.DownloadRV.adapter = adapter
        binding.daysTotalVideos.text = "Total Videos : 0"
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    loadRecentVideos()
                } else {
                    // Handle permission denial
                }
            }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadRecentVideos()
        } else {
//            if (shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//            }
            requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }


        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            binding.swipeRefreshVideo.isRefreshing = false // Hide the refresh indicator
        }, 2000) // 2000 milliseconds (2 seconds)


        return view
    }


    @SuppressLint("SetTextI18n")
    private fun loadRecentVideos(){
        val recantVideos = getAllRecantVideos(requireContext())
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val sortedRecentVideos = recantVideos.filter { it.timestamp >= sevenDaysAgo }
        recantVideos.sortedByDescending { it.timestamp }

        adapter.updateRecentVideos(sortedRecentVideos)
        binding.daysTotalVideos.text = "Total Musics : ${sortedRecentVideos.size}"

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
            val sizeC =it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            while (it.moveToNext()) {
                val title = it.getString(titleC)
                val id = it.getString(idC)
                val size = it.getString(sizeC)
                val duration = it.getLong(durationC)
                val timestamp = it.getLong(timestampC) * 1000
                val path = it.getString(pathC)
                val artUri = Uri.parse("content://media/external/video/media/$id")

                val video = RecantVideo(title, timestamp, id, duration, path, artUri , size )
                recantVList.add(video)
            }
        }

        return recantVList
    }

//    @SuppressLint("NotifyDataSetChanged")
//    override fun onResume() {
//        super.onResume()
//        if (MainActivity.dataChanged) adapter.notifyDataSetChanged()
//        MainActivity.dataChanged = false
//    }

}
