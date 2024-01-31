package com.jaidev.seeaplayer.browseFregment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.jaidev.seeaplayer.BookmarkActivity
import com.jaidev.seeaplayer.BookmarkAdapter
import com.jaidev.seeaplayer.LinkTubeActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.changeTab
import com.jaidev.seeaplayer.checkForInternet
import com.jaidev.seeaplayer.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {


    private lateinit var binding: FragmentHomeBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        binding = FragmentHomeBinding.bind(view)

        return view
    }


    override fun onResume() {
        super.onResume()

         val linkTubeRef = requireActivity() as LinkTubeActivity

        LinkTubeActivity.tabsBtn.text = LinkTubeActivity.tabsList.size.toString()
        LinkTubeActivity.tabsList[LinkTubeActivity.myPager.currentItem].name = "Home"

        linkTubeRef.binding.topSearchBar.setText("")
        binding.searchView.setQuery("" , false)
        linkTubeRef.binding.webIcon.setImageResource(R.drawable.search_icon)

        linkTubeRef.binding.refreshBrowserBtn.visibility = View.GONE

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(result: String?): Boolean {
             if (checkForInternet(requireContext()))
                 changeTab(result!!, BrowseFragment(result))
                else
                    Snackbar.make(binding.root , "Internet Not Connected\uD83D\uDE03" , 3000).show()
             return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = false

        })
        linkTubeRef.binding.goBtn.setOnClickListener {
            if(checkForInternet(requireContext()))
                changeTab(linkTubeRef.binding.topSearchBar.text.toString(),
                    BrowseFragment(linkTubeRef.binding.topSearchBar.text.toString())
                )
            else
                Snackbar.make(binding.root, "Internet Not Connected\uD83D\uDE03", 3000).show()
        }

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.setItemViewCacheSize(5)
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext() , 5)
        binding.recyclerView.adapter = BookmarkAdapter(requireContext())

        if (LinkTubeActivity.bookmarkList.size < 1)
            binding.viewAllBtn.visibility = View.GONE
        binding.viewAllBtn.setOnClickListener {
            startActivity(Intent(requireContext() , BookmarkActivity::class.java))
        }


    }
}

