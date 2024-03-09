package com.jaidev.seeaplayer.bottomNavigation

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaidev.seeaplayer.FoldersAdapter
import com.jaidev.seeaplayer.LinkTubeActivity
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.VideoAdapter
import com.jaidev.seeaplayer.databinding.FragmentHomeNavBinding

class homeNav : Fragment() {
    lateinit var adapter: VideoAdapter
    private lateinit var currentFragment: Fragment
    private lateinit var foldersAdapter: FoldersAdapter
    private lateinit var binding: FragmentHomeNavBinding
    private var isSearchViewClicked = false
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        requireActivity().setTheme(R.style.coolBlueNav)

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

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            binding.swipeRefreshFolder.isRefreshing = false // Hide the refresh indicator
        }, 2000) // 2000 milliseconds (2 seconds)


        binding.searchBackBtn.setOnClickListener {
            binding.searchRecyclerView.visibility = View.GONE
        }

        binding.chip5.setOnClickListener {
            val alertDialogBuilder = AlertDialog.Builder(requireContext())
            val view = layoutInflater.inflate(R.layout.delete_alertdialog, null)

            val deleteText = view.findViewById<TextView>(R.id.deleteText)
            val cancelText = view.findViewById<TextView>(R.id.cancelText)
            val iconImageView = view.findViewById<ImageView>(R.id.videoImage)

            // Set the delete text color to red
            deleteText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))

            // Set the cancel text color to black
            cancelText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))

            alertDialogBuilder.setView(view)

            val alertDialog = alertDialogBuilder.create()

            deleteText.setOnClickListener {

            }

            cancelText.setOnClickListener {
                // Handle cancel action here
                alertDialog.dismiss()
            }
            alertDialog.show()
        }
        binding.chip2.setOnClickListener {
            val intent = Intent(requireContext(), LinkTubeActivity::class.java)
            startActivity(intent)
        }
//        binding.chip.setOnClickListener {
//           it.findNavController().navigate(R.id.action_homeNav_to_daysDownload)
//        }
//        binding.chip5.setOnClickListener {
//           it.findNavController().navigate(R.id.action_homeNav_to_musicNav)
//        }
        return binding.root
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
