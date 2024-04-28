package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.jaidev.seeaplayer.databinding.FragmentNavigationBinding


class NavigationFragment : Fragment() {
    private lateinit var drawerLayout: LinearLayout
private lateinit var binding : FragmentNavigationBinding
    @SuppressLint("ResourceType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_navigation, container, false)


        binding = FragmentNavigationBinding.bind(view)

        drawerLayout = binding.linearLayoutNav

        return view
    }



}