
package com.jaidev.seeaplayer.recantFragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jaidev.seeaplayer.MainActivity.Companion.getAllRecantMusics
import com.jaidev.seeaplayer.MainActivity.Companion.musicRecantList
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.MusicAdapter
import com.jaidev.seeaplayer.allAdapters.RecantMusicAdapter
import com.jaidev.seeaplayer.databinding.FragmentDaysMusicBinding
import java.util.concurrent.TimeUnit

class DaysMusic : Fragment() ,   RecantMusicAdapter.OnFileCountChangeListener,  RecantMusicAdapter.MusicDeleteListener , MusicAdapter.MusicDeleteListener  {
    private lateinit var adapter: RecantMusicAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
companion object{
    @SuppressLint("StaticFieldLeak")
    private lateinit var binding: FragmentDaysMusicBinding

    fun updateEmptyViewVisibility() {
        if (musicRecantList.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.shuffleBtn.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.shuffleBtn.visibility = View.VISIBLE

        }
    }
}

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_days_music, container, false)
        binding = FragmentDaysMusicBinding.bind(view)
        try {
            binding.MusicRV.setHasFixedSize(true)
            binding.MusicRV.setItemViewCacheSize(50)
            binding.MusicRV.layoutManager = LinearLayoutManager(requireContext())
            adapter = RecantMusicAdapter(requireContext(), musicRecantList, isReMusic = true, this@DaysMusic, isMusic = false)
            binding.MusicRV.adapter = adapter
            binding.daysTotalMusics.text = "0 Musics"

            loadRecentMusics()

            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                binding.swipeRefreshMusic.isRefreshing = false // Hide the refresh indicator
            }, 2000) // 2000 milliseconds (2 seconds)


            swipeRefreshLayout = binding.swipeRefreshMusic
            swipeRefreshLayout.setOnRefreshListener {
                loadRecentMusics()
                binding.shuffleBtn.visibility = View.VISIBLE
                swipeRefreshLayout.isRefreshing = false
            }
            shuffleEmpty()
        } catch (_: Exception) {
        }
        return view

    }


    private fun shuffleEmpty(){
        binding.shuffleBtn.setOnClickListener {
            val intent = Intent(requireContext() , ReMusicPlayerActivity::class.java)
            intent.putExtra("index" , 0)
            intent.putExtra("class" , "DaysMusic")
            startActivity(intent)
        }
        updateEmptyViewVisibility()

    }

    @SuppressLint("SetTextI18n")
    private fun loadRecentMusics() {
        val recantMusics = getAllRecantMusics(requireContext())
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val reMusics = recantMusics.filter { it.timestamp >= sevenDaysAgo }
        recantMusics.sortedByDescending { it.timestamp }
        adapter.updateRecentMusics(reMusics)

        // Update the total music count text
        binding.daysTotalMusics.text = "${reMusics.size} Musics"

        updateEmptyViewVisibility()

    }



    @SuppressLint("SetTextI18n")
    override fun onFileCountChanged(newCount: Int) {
        binding.daysTotalMusics.text = "$newCount Musics"

    }
    override fun onDestroyView() {
        super.onDestroyView()
        // Disable selection mode in the adapter
        adapter.disableSelectionMode()
    }
    override fun onMusicDeleted() {
        updateEmptyViewVisibility()
        loadRecentMusics()
    }

}