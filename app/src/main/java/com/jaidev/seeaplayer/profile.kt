package com.jaidev.seeaplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jaidev.seeaplayer.databinding.FragmentProfileBinding

class profile : Fragment() {
    private lateinit var binding:FragmentProfileBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        binding = FragmentProfileBinding.bind(view)


        return view
    }
}