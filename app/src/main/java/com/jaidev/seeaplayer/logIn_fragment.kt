package com.jaidev.seeaplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.jaidev.seeaplayer.databinding.FragmentLogInFragmentBinding

class logIn_fragment : Fragment() {

private lateinit var binding : FragmentLogInFragmentBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
          val view = inflater.inflate(R.layout.fragment_log_in_fragment, container, false)
binding = FragmentLogInFragmentBinding.bind(view)

        binding.Signin.setOnClickListener {
            it.findNavController().navigate(R.id.action_logIn_fragment_to_sinIn_Fragment)
        }
        return view
    }


}