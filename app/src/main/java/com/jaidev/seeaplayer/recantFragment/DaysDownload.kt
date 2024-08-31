

package com.jaidev.seeaplayer.recantFragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jaidev.seeaplayer.MainActivity.Companion.getAllRecantVideos
import com.jaidev.seeaplayer.MainActivity.Companion.videoRecantList
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.RecentVideoAdapter
import com.jaidev.seeaplayer.databinding.FragmentDaysDownloadBinding
import java.util.concurrent.TimeUnit
// Import statements go here

class DaysDownload : Fragment(),   RecentVideoAdapter.OnFileCountChangeListener {
    lateinit var adapter: RecentVideoAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var binding: FragmentDaysDownloadBinding
        fun updateEmptyViewVisibility() {
            if (videoRecantList.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
            }
        }

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

    @SuppressLint("SetTextI18n")
    private fun loadRecentVideos() {
        val recantVideos = getAllRecantVideos(requireContext())
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val sortedRecentVideos = recantVideos.filter { it.timestamp >= sevenDaysAgo }
        recantVideos.sortedByDescending { it.timestamp }

        adapter.updateRecentVideos(sortedRecentVideos)
        binding.daysTotalVideos.text = "${sortedRecentVideos.size} Videos"

        updateEmptyViewVisibility()

    }


    override fun onDestroyView() {
        super.onDestroyView()
        // Disable selection mode in the adapter
        adapter.disableSelectionMode()
    }
    @SuppressLint("SetTextI18n")
    override fun onFileCountChanged(newCount: Int) {
        binding.daysTotalVideos.text = "$newCount Videos"
    }

}


