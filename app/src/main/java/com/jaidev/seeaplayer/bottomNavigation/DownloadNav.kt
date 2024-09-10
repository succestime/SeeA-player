package com.jaidev.seeaplayer.bottomNavigation

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.jaidev.seeaplayer.MainActivity.Companion.isInternetAvailable
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.Subscription.SeeAOne
import com.jaidev.seeaplayer.allAdapters.ReDownloadAdapter
import com.jaidev.seeaplayer.databinding.FragmentDownloadNavBinding


class DownloadNav : Fragment() {
    private lateinit var adapter: ReDownloadAdapter
    private lateinit var binding: FragmentDownloadNavBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_download_nav, container, false)

        binding = FragmentDownloadNavBinding.bind(view)

        val supportFragmentManager = childFragmentManager
        adapter = ReDownloadAdapter(supportFragmentManager , lifecycle  )

        setupActionBar()

        binding.myTabLayout.addTab( binding.myTabLayout.newTab().setText("Recant Video"))
        binding.myTabLayout.addTab( binding.myTabLayout.newTab().setText("Recant Music"))
        binding.viewPagerDownload.adapter = adapter

        binding.myTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    binding.viewPagerDownload.currentItem = tab.position
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.viewPagerDownload.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.myTabLayout.selectTab(binding.myTabLayout.getTabAt(position))

            }
        })

        return view
    }
    private fun setupActionBar() {
        val inflater = LayoutInflater.from(requireContext())
        val customActionBarView = inflater.inflate(R.layout.custom_action_bar_layout, null)

        val titleTextView = customActionBarView.findViewById<TextView>(R.id.titleTextView)
        titleTextView.text = "Downloads"

        // Get the current theme
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(R.attr.titleTextColor, typedValue, true)
        val titleTextColor = typedValue.data

        // Set the title text color based on the current theme
        titleTextView.setTextColor(titleTextColor)
        val subscribeTextView = customActionBarView.findViewById<TextView>(R.id.subscribe)
        if (isInternetAvailable(requireContext())) {
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


}