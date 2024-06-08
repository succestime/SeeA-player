package com.jaidev.seeaplayer.bottomNavigation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.FoldersAdapter
import com.jaidev.seeaplayer.allAdapters.VideoAdapter
import com.jaidev.seeaplayer.databinding.FragmentHomeNavBinding

class homeNav : Fragment() {
    lateinit var adapter: VideoAdapter
    private lateinit var foldersAdapter: FoldersAdapter
    private lateinit var binding: FragmentHomeNavBinding
    private var isSearchViewClicked = false
    private lateinit var searchView: SearchView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    lateinit var mAdView: AdView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)


    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentHomeNavBinding.inflate(inflater, container, false)
        adapter = VideoAdapter(requireContext(), MainActivity.videoList )



        binding.folderRV.setHasFixedSize(true)
        binding.folderRV.setItemViewCacheSize(10)
        binding.folderRV.layoutManager = LinearLayoutManager(requireContext())
        foldersAdapter = FoldersAdapter(requireContext(), MainActivity.folderList)
        binding.folderRV.adapter = foldersAdapter

        binding.searchRecyclerView.setHasFixedSize(true)
        binding.searchRecyclerView.setItemViewCacheSize(10)
        binding.searchRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.searchRecyclerView.visibility = View.GONE
        binding.searchRecyclerView.adapter = adapter




        swipeRefreshLayout = binding.swipeRefreshFolder

        binding.totalFolder.text = "Total Folders : ${MainActivity.folderList.size}"

        binding.swipeRefreshFolder.setOnRefreshListener {

            refreshFolderList()
        }
        // Set the background color of SwipeRefreshLayout based on app theme
        setSwipeRefreshBackgroundColor()

        if (MainActivity.folderList.isEmpty()) {
            binding.videoEmptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.videoEmptyStateLayout.visibility = View.GONE
        }

        MobileAds.initialize(requireContext()){}
//       mAdView = binding.adView

        return binding.root
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun refreshFolderList() {
        // Call the refresh function in MainActivity
      (activity as MainActivity).refreshFolderList()
        // Update the adapter with the new data
        foldersAdapter.notifyDataSetChanged()
        adapter.notifyDataSetChanged()

        binding.totalFolder.text = "Total Folders : ${MainActivity.folderList.size}"
        swipeRefreshLayout.isRefreshing = false

        if (MainActivity.folderList.isEmpty()) {
            binding.videoEmptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.videoEmptyStateLayout.visibility = View.GONE
        }
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


    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_view_menu, menu)
        val searchItem = menu.findItem(R.id.searchView)

        searchView = searchItem?.actionView as SearchView

        // Set a collapse listener to track when the search view is closed
        searchView.setOnCloseListener {
            isSearchViewClicked = false
            binding.searchRecyclerView.visibility = View.GONE
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

                        }
                    }
                    MainActivity.search = true
                    adapter.updateList(searchList = MainActivity.searchList)
                }

                // Check if the search view is clicked or if there is text in the search view
                if (isSearchViewClicked || newText?.isNotEmpty() == true) {
                  binding.searchRecyclerView.visibility = View.VISIBLE
                } else {
                    binding.searchRecyclerView.visibility = View.GONE
                }
                return true
            }
        })

        super.onCreateOptionsMenu(menu, inflater)
    }





}
