//package com.jaidev.seeaplayer.bottomNavigation
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.appcompat.app.AppCompatActivity
//import androidx.fragment.app.Fragment
//import androidx.navigation.findNavController
//import com.google.firebase.auth.FirebaseAuth
//import com.jaidev.seeaplayer.R
//import com.jaidev.seeaplayer.databinding.FragmentMoreNavBinding
//
//class moreNav : Fragment() {
//private lateinit var binding : FragmentMoreNavBinding
//
//companion object{
//    lateinit var auth : FirebaseAuth
//}
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//
//        val view = inflater.inflate(R.layout.fragment_more_nav, container, false)
//        binding = FragmentMoreNavBinding.bind(view)
//        (activity as AppCompatActivity).supportActionBar?.title = "SeeA Player"
//
//        auth = FirebaseAuth.getInstance()
//
//        binding.goToSignin.setOnClickListener {
//            it.findNavController().navigate(R.id.action_moreNav_to_sinIn_Fragment2)
//
//        }
//        binding.signOut.setOnClickListener{
//            auth.signOut()
//            binding.userDetails.text = updateData()
//        }
//
//
//
//        return view
//    }
//
//    override fun onResume() {
//        super.onResume()
//        binding.userDetails.text = updateData()
//    }
//    private fun updateData(): String {
//
//        return "Email : ${auth.currentUser?.email}"
//
//    }
//
//}