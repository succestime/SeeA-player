package com.jaidev.seeaplayer.bottomNavigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.databinding.FragmentMoreNavBinding

class moreNav : Fragment() {
private lateinit var binding : FragmentMoreNavBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_more_nav, container, false)
        binding = FragmentMoreNavBinding.bind(view)



        binding.goToSignin.setOnClickListener {
            it.findNavController().navigate(R.id.action_moreNav_to_sinIn_Fragment2)
        }
        return view
    }
}