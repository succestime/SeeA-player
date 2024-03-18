package com.jaidev.seeaplayer.bottomNavigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.ReDownloadAdapter
import com.jaidev.seeaplayer.databinding.FragmentDownloadNavBinding



class downloadNav : Fragment() {
   private lateinit var adapter: ReDownloadAdapter
    private lateinit var binding: FragmentDownloadNavBinding



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        requireContext().theme.applyStyle(More.themesList[More.themeIndex], true)
        val view = inflater.inflate(R.layout.fragment_download_nav, container, false)
//
//       supportActionBar?.setDisplayHomeAsUpEnabled(false)
        binding = FragmentDownloadNavBinding.bind(view)

        val supportFragmentManager = childFragmentManager
        adapter = ReDownloadAdapter(supportFragmentManager , lifecycle  )


     binding.myTabLayout.addTab( binding.myTabLayout.newTab().setText("Recant Download"))
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

        binding.viewPagerDownload.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.myTabLayout.selectTab(binding.myTabLayout.getTabAt(position))
            }
        })
        return view
    }

}
