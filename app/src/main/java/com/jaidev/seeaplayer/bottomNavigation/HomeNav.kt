
package com.jaidev.seeaplayer.bottomNavigation

import android.annotation.SuppressLint
import android.content.Intent
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
import com.jaidev.seeaplayer.FoldersAdapter
import com.jaidev.seeaplayer.LinkTubeActivity
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.VideoAdapter
import com.jaidev.seeaplayer.databinding.FragmentHomeNavBinding

class homeNav : Fragment() {
    lateinit var adapter: VideoAdapter
    private lateinit var foldersAdapter: FoldersAdapter
    private lateinit var binding: FragmentHomeNavBinding
    private var isSearchViewClicked = false
    private lateinit var searchView: SearchView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)


    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentHomeNavBinding.inflate(inflater, container, false)
        adapter = VideoAdapter(requireContext(), MainActivity.videoList)

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


        binding.totalFolder.text = "Total Folders : ${MainActivity.folderList.size}"

        binding.swipeRefreshFolder.setOnRefreshListener {
            // Perform the refresh action here
            refreshFolders()
        }

        binding.searchBackBtn.setOnClickListener {
            binding.searchRecyclerView.visibility = View.GONE
        }


        binding.chip5.setOnClickListener {
        }

        binding.chip2.setOnClickListener {
            val intent = Intent(requireContext(), LinkTubeActivity::class.java)
            startActivity(intent)
        }


        swipeRefreshLayout = binding.swipeRefreshFolder

        // Set the background color of SwipeRefreshLayout based on app theme
        setSwipeRefreshBackgroundColor()



        return binding.root
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

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshFolders() {
        binding.swipeRefreshFolder.isRefreshing = false
        foldersAdapter.notifyDataSetChanged()
    }
    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_view_menu, menu)
        val searchItem = menu.findItem(R.id.searchView)

        searchView = searchItem?.actionView as SearchView

        // Set an expand listener to track whether the search view is explicitly clicked
        searchView.setOnSearchClickListener {
            isSearchViewClicked = true
        }

        // Set a collapse listener to track when the search view is closed
        searchView.setOnCloseListener {
            isSearchViewClicked = false
            toggleSearchRecyclerViewVisibility(false)
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
                    toggleSearchRecyclerViewVisibility(true)
                } else {
                    toggleSearchRecyclerViewVisibility(false)
                }
                return true
            }
        })

        super.onCreateOptionsMenu(menu, inflater)
    }





    private fun toggleSearchRecyclerViewVisibility(show: Boolean) {

        // Check if the search view is explicitly clicked or if it is expanded
        if (isSearchViewClicked || searchView.isIconified) {
            binding.searchRecyclerView.visibility =
                if (show && MainActivity.searchList.isNotEmpty()) View.VISIBLE else View.GONE
        } else {
            // If the search view is not clicked and not expanded, hide the searchRecyclerView
            binding.searchRecyclerView.visibility = View.GONE
        }
    }

}
