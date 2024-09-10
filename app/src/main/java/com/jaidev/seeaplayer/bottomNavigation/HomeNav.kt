package com.jaidev.seeaplayer.bottomNavigation

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.ads.MobileAds
import com.jaidev.seeaplayer.MP3ConverterFunctionality.MP3ConverterActivity
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.SearchActivity
import com.jaidev.seeaplayer.Subscription.SeeAOne
import com.jaidev.seeaplayer.VideoPlaylistFunctionality.PlaylistFolderActivity
import com.jaidev.seeaplayer.allAdapters.ChipAdapter
import com.jaidev.seeaplayer.allAdapters.FoldersAdapter
import com.jaidev.seeaplayer.allAdapters.VideoSearchAdapter
import com.jaidev.seeaplayer.browserActivity.PlayerFileActivity
import com.jaidev.seeaplayer.dataClass.ChipItem
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.databinding.FragmentHomeNavBinding

class HomeNav : Fragment() , VideoSearchAdapter.OnItemClickListener{
    private lateinit var foldersAdapter: FoldersAdapter
    private lateinit var searchAdapter: VideoSearchAdapter

    private lateinit var binding: FragmentHomeNavBinding
    private var isSearchViewClicked = false
    private lateinit var searchView: SearchView

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var searchItem: MenuItem
    private val iconModelArrayList = ArrayList<ChipItem>()
    private lateinit var playbackIconsAdapter: ChipAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)


    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentHomeNavBinding.inflate(inflater, container, false)
        val theme = ThemeHelper.getSavedTheme(requireContext())
        ThemeHelper.applyTheme(requireContext(),theme)


        setupActionBar()
        folderGetFunction()
        swipeRefreshLayout = binding.swipeRefreshFolder

        binding.swipeRefreshFolder.setOnRefreshListener {
            refreshFolderList()
        }
        setSwipeRefreshBackgroundColor()

        MobileAds.initialize(requireContext()){}
        horizontalIconList()
        return binding.root
    }

    private fun folderGetFunction(){
        binding.folderRV.setHasFixedSize(true)
        binding.folderRV.setItemViewCacheSize(10)
        binding.folderRV.layoutManager = LinearLayoutManager(requireContext())
        foldersAdapter = FoldersAdapter(requireContext(), MainActivity.folderList)
        binding.folderRV.adapter = foldersAdapter

        if (MainActivity.folderList.isEmpty()) {
            binding.videoEmptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.videoEmptyStateLayout.visibility = View.GONE
        }

        searchAdapter = VideoSearchAdapter(requireContext(), MainActivity.videoList, isSearchActivity = false , isFolder = true, isShort = true , this)
        binding.searchRecyclerView.setHasFixedSize(true)
        binding.searchRecyclerView.setItemViewCacheSize(10)
        binding.searchRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.searchRecyclerView.visibility = View.GONE
        binding.searchRecyclerView.adapter = searchAdapter
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun horizontalIconList() {

        iconModelArrayList.add(ChipItem("Playlist", R.drawable.round_playlist_music))
        iconModelArrayList.add(ChipItem("Share", R.drawable.share_svgrepo_com))
        iconModelArrayList.add(ChipItem("MP3 Converter", R.drawable.outline_headphones_24))

        playbackIconsAdapter = ChipAdapter(requireContext() , iconModelArrayList)
        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding.chipRecyclerView.layoutManager = layoutManager
        binding.chipRecyclerView.adapter = playbackIconsAdapter
        playbackIconsAdapter.notifyDataSetChanged()


        playbackIconsAdapter.setOnItemClickListener(object : ChipAdapter.OnItemClickListener {
            @SuppressLint("Range", "SourceLockedOrientationActivity")
            override fun onItemClick(position: Int) {
                when (position) {
                    0 -> {
                        startActivity(Intent(requireContext(), PlaylistFolderActivity::class.java))
                    }
                    1->{
                        startActivity(Intent(requireContext(), PlaylistFolderActivity::class.java))
                    }
                    2->{
                        startActivity(Intent(requireContext(), MP3ConverterActivity::class.java))
                    }

                    else -> {
                        // Handle any other positions if needed
                    }
                }
            }
        })
    }


        @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun refreshFolderList() {
        (activity as MainActivity).refreshFolderList()
        foldersAdapter.notifyDataSetChanged()
        searchAdapter.notifyDataSetChanged()
        swipeRefreshLayout.isRefreshing = false
        if (MainActivity.folderList.isEmpty()) {
            binding.videoEmptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.videoEmptyStateLayout.visibility = View.GONE
        }
    }


    private fun setupActionBar() {
        val inflater = LayoutInflater.from(requireContext())
        val customActionBarView = inflater.inflate(R.layout.custom_action_bar_layout, null)

        val titleTextView = customActionBarView.findViewById<TextView>(R.id.titleTextView)
        titleTextView.text = "Folders"
        // Get the current theme
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(R.attr.titleTextColor, typedValue, true)
        val titleTextColor = typedValue.data

        // Set the title text color based on the current theme
        titleTextView.setTextColor(titleTextColor)
        val subscribeTextView = customActionBarView.findViewById<TextView>(R.id.subscribe)
        if (MainActivity.isInternetAvailable(requireContext())) {
            subscribeTextView.visibility = View.VISIBLE
            subscribeTextView.setOnClickListener {
                startActivity(Intent(requireContext(), SeeAOne::class.java))
                (activity as AppCompatActivity).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
            }
        } else {
            subscribeTextView.visibility = View.GONE
        }


        (activity as AppCompatActivity).supportActionBar?.apply {
            setDisplayShowCustomEnabled(true)
            setDisplayShowTitleEnabled(false)
            customView = customActionBarView
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
        searchItem = menu.findItem(R.id.searchView)

        searchView = searchItem.actionView as SearchView

        // Set a collapse listener to track when the search view is closed
        searchView.setOnCloseListener {
            isSearchViewClicked = false
            binding.searchRecyclerView.visibility = View.GONE
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
                    searchAdapter.updateList(searchList = MainActivity.searchList)
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


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        refreshFolderList()
    }



    override fun onItemClick(fileItem: VideoData) {
        PlayerFileActivity.pipStatus = 1
        val intent = Intent(requireContext(), SearchActivity::class.java)
        intent.putExtra("videoData", fileItem)
        startActivity(intent)
        searchItem.collapseActionView()

    }



}