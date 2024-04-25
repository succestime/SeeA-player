package com.jaidev.seeaplayer.allAdapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jaidev.seeaplayer.recantFragment.DaysMusic
import com.jaidev.seeaplayer.recantFragment.DaysDownload

class ReDownloadAdapter(fragmentManager: FragmentManager , lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager,lifecycle) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return if (position == 0)
            DaysDownload()
        else
            DaysMusic()
    }
}