package com.jaidev.seeaplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.jaidev.seeaplayer.databinding.ActivityMoreBinding

class More : AppCompatActivity() {

private lateinit var binding : ActivityMoreBinding
    private lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoreBinding.inflate(layoutInflater)

        setContentView(binding.root)

        supportActionBar?.apply {

            setBackgroundDrawable(ContextCompat.getDrawable(this@More, R.drawable.background_actionbar))
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        navController = findNavController(R.id.navHostFragmentContainerView)

        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}