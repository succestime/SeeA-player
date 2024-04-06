
package com.jaidev.seeaplayer.browseFregment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
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

        linkTubeRef.binding.btnTextUrl.setText("")
        binding.searchView.setQuery("" , false)
        linkTubeRef.binding.webIcon.setImageResource(R.drawable.search_icon)

        linkTubeRef.binding.refreshBrowserBtn.visibility = View.GONE
        linkTubeRef.binding.googleMicBtn.visibility = View.VISIBLE
        // TextWatcher for btnTextUrl
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    linkTubeRef.binding.googleMicBtn.visibility = View.VISIBLE
                    linkTubeRef.binding.crossBtn.visibility = View.GONE
                } else {
                    linkTubeRef.binding.googleMicBtn.visibility = View.GONE
                    linkTubeRef.binding.crossBtn.visibility = View.VISIBLE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        linkTubeRef.binding.btnTextUrl.addTextChangedListener(textWatcher)

        // OnClickListener for crossBtn to clear text
        linkTubeRef.binding.crossBtn.setOnClickListener {
            linkTubeRef.binding.btnTextUrl.text.clear()
        }
        // Listen for the action performed on the EditText
        linkTubeRef.binding.btnTextUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                // Perform action when the Enter key or Go button is pressed
                val query =  linkTubeRef.binding.btnTextUrl.text.toString()
                if (checkForInternet(requireContext())) {
                    changeTab(query, BrowseFragment(query))
                } else {
                    Toast.makeText(requireContext(), "No Internet Connection \uD83C\uDF10", Toast.LENGTH_SHORT).show()
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(result: String?): Boolean {
                if (checkForInternet(requireContext()))
                    changeTab(result!!, BrowseFragment(result))
                else
                    Toast.makeText(requireContext(), "No Internet Connection \uD83C\uDF10", Toast.LENGTH_SHORT).show()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = false

        })
//        linkTubeRef.binding.goBtn.setOnClickListener {
//            if(checkForInternet(requireContext()))
//                changeTab(linkTubeRef.binding.btnTextUrl.text.toString(),
//                    BrowseFragment(linkTubeRef.binding.btnTextUrl.text.toString())
//                )
//            else
//                Toast.makeText(requireContext(), "No Internet Connection \uD83C\uDF10", Toast.LENGTH_SHORT).show()
//        }

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
