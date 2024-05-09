
package com.jaidev.seeaplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.jaidev.seeaplayer.databinding.ActivityMoreBinding

class More : AppCompatActivity() {

    private lateinit var binding: ActivityMoreBinding
private lateinit var navController: NavController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable the Up button

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        navController = findNavController(R.id.navHostFragmentContainerView)
        return navController.navigateUp() ||super.onSupportNavigateUp()
    }


}
