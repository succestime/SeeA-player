package com.jaidev.seeaplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.jaidev.seeaplayer.databinding.FragmentSinInBinding

class SinIn_Fragment : Fragment() {
private  lateinit var binding: FragmentSinInBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
       val view =  inflater.inflate(R.layout.fragment_sin_in_, container, false)
        binding = FragmentSinInBinding.bind(view)


        binding.login.setOnClickListener {
            it.findNavController().navigate(R.id.action_sinIn_Fragment_to_logIn_fragment)
        }
    return view
    }

}