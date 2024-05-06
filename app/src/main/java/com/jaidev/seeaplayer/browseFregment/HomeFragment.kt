
package com.jaidev.seeaplayer.browseFregment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.internal.ViewUtils.hideKeyboard
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.BookmarkAdapter
import com.jaidev.seeaplayer.allAdapters.SavedTitlesAdapter
import com.jaidev.seeaplayer.browserActivity.BookmarkActivity
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.browserActivity.changeTab
import com.jaidev.seeaplayer.browserActivity.checkForInternet
import com.jaidev.seeaplayer.dataClass.SearchTitle
import com.jaidev.seeaplayer.dataClass.SearchTitleStore
import com.jaidev.seeaplayer.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private var isBtnTextUrlFocused = false // Flag to track if btnTextUrl has been focused

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        binding = FragmentHomeBinding.bind(view)

        // Show the keyboard explicitly

        // Check if the keyboard is visible
        val rootView = view?.rootView
        rootView?.viewTreeObserver?.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom
            val isKeyboardVisible = keypadHeight > screenHeight * 0.15

            // Update the visibility of RecyclerView based on keyboard visibility
            binding.historyRecycler.visibility = if (isKeyboardVisible) View.VISIBLE else View.GONE
        }
        return view
    }

    @SuppressLint("RestrictedApi")
    override fun onResume() {
        super.onResume()

        val linkTubeRef = requireActivity() as LinkTubeActivity

        LinkTubeActivity.tabsBtn.text = LinkTubeActivity.tabsList.size.toString()
        LinkTubeActivity.tabsList[LinkTubeActivity.myPager.currentItem].name = "Home"

        linkTubeRef.binding.btnTextUrl.setText("")
        linkTubeRef.binding.webIcon.setImageResource(R.drawable.search_icon)
//        linkTubeRef.binding.refreshBrowserBtn.visibility = View.GONE
        linkTubeRef.binding.googleMicBtn.visibility = View.VISIBLE
        linkTubeRef.binding.bookMarkBtn.visibility = View.VISIBLE
        linkTubeRef.binding.crossBtn.visibility = View.GONE

        // TextWatcher for btnTextUrl
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    linkTubeRef.binding.googleMicBtn.visibility = View.VISIBLE
                    linkTubeRef.binding.bookMarkBtn.visibility = View.VISIBLE
                    linkTubeRef.binding.crossBtn.visibility = View.GONE
                } else {
                    linkTubeRef.binding.googleMicBtn.visibility = View.GONE
                    linkTubeRef.binding.bookMarkBtn.visibility = View.VISIBLE
                    linkTubeRef.binding.crossBtn.visibility = View.VISIBLE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        linkTubeRef.binding.btnTextUrl.addTextChangedListener(textWatcher)

        linkTubeRef.binding.crossBtn.setOnClickListener {
            linkTubeRef.binding.btnTextUrl.text.clear()
        }

        linkTubeRef.binding.btnTextUrl.setOnClickListener {
            linkTubeRef.binding.btnTextUrl.requestFocus()

        }

        linkTubeRef.binding.searchBrowser.setOnClickListener {
            // Request focus on btnTextUrl
            linkTubeRef.binding.btnTextUrl.requestFocus()

            // Check if btnTextUrl has text, if so, select all text
            if (linkTubeRef.binding.btnTextUrl.text.isNotEmpty()) {
                linkTubeRef.binding.btnTextUrl.selectAll()
            }

            // Show the keyboard explicitly
            val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(linkTubeRef.binding.btnTextUrl, InputMethodManager.SHOW_IMPLICIT)

            // Check if the keyboard is visible
            val rootView = view?.rootView
            rootView?.viewTreeObserver?.addOnGlobalLayoutListener {
                val rect = Rect()
                rootView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootView.height
                val keypadHeight = screenHeight - rect.bottom
                val isKeyboardVisible = keypadHeight > screenHeight * 0.15

                // Update the visibility of RecyclerView based on keyboard visibility
                binding.historyRecycler.visibility = if (isKeyboardVisible) View.VISIBLE else View.GONE
            }
        }


        // Set click listener for homeTextUrl
        binding.homeTextUrl.setOnClickListener {
            // Request focus on btnTextUrl only if it's not already focused
            if (!isBtnTextUrlFocused) {
                linkTubeRef.binding.btnTextUrl.requestFocus()
                isBtnTextUrlFocused = true // Update the flag
            }

            // Show the keyboard explicitly
            val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(linkTubeRef.binding.btnTextUrl, InputMethodManager.SHOW_IMPLICIT)

            // Check if the keyboard is visible
            val rootView = view?.rootView
            rootView?.viewTreeObserver?.addOnGlobalLayoutListener {
                val rect = Rect()
                rootView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootView.height
                val keypadHeight = screenHeight - rect.bottom
                val isKeyboardVisible = keypadHeight > screenHeight * 0.15

                // Update the visibility of RecyclerView based on keyboard visibility
                binding.historyRecycler.visibility = if (isKeyboardVisible) View.VISIBLE else View.GONE
            }
        }


        binding.historyRecycler.setHasFixedSize(true)
        binding.historyRecycler.setItemViewCacheSize(5)
        binding.historyRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecycler.adapter = SavedTitlesAdapter(requireContext())





        // Set editor action listener for btnTextUrl (IME_ACTION_DONE or IME_ACTION_GO)
        linkTubeRef.binding.btnTextUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                val query = linkTubeRef.binding.btnTextUrl.text.toString()
                if (checkForInternet(requireContext())) {
                    // Hide keyboard and RecyclerView
                    hideKeyboard(linkTubeRef.binding.btnTextUrl)
                    binding.historyRecycler.visibility = View.GONE
                    // Perform search
                    performSearch(query)
                } else {
                    Toast.makeText(requireContext(), "No Internet Connection \uD83C\uDF10", Toast.LENGTH_SHORT).show()
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        binding.voiceSearchButton.setOnClickListener {
            linkTubeRef.speak()
        }

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.setItemViewCacheSize(5)
        // Determine screen size
        val isTablet = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
        // Set appropriate GridLayoutManager
        val spanCount = if (isTablet) 5 else 3
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)

        binding.recyclerView.adapter = BookmarkAdapter(requireContext())

        if (LinkTubeActivity.bookmarkList.size < 1)
            binding.viewAllBtn.visibility = View.GONE

        binding.viewAllBtn.setOnClickListener {
            startActivity(Intent(requireContext(), BookmarkActivity::class.java))
        }


    }



    // Inside performSearch() method
    // Inside performSearch() method
    @SuppressLint("NotifyDataSetChanged")
    private fun performSearch(query: String) {
        if (checkForInternet(requireContext())) {
            // Check if the title is already saved
            val isTitleSaved = SearchTitleStore.getTitles(requireContext()).any { it.title == query }

            if (!isTitleSaved) {
                // Create a SearchTitle object with the query and save it
                val searchTitle = SearchTitle(query)
                SearchTitleStore.addTitle(requireContext(), searchTitle)
                // Add new item to the adapter
                val adapter = binding.historyRecycler.adapter as SavedTitlesAdapter
                adapter.addItem(query)
            }

            // Change tab and perform search
            changeTab(query, BrowseFragment(query))
        } else {
            Toast.makeText(requireContext(), "No Internet Connection \uD83C\uDF10", Toast.LENGTH_SHORT).show()
        }
    }




}
