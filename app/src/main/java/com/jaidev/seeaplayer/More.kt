
package com.jaidev.seeaplayer

import android.content.Intent
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.jaidev.seeaplayer.databinding.ActivityMoreBinding

class More : AppCompatActivity() {

    private lateinit var binding: ActivityMoreBinding

    private lateinit var relativeLayout: RelativeLayout


    companion object {
        lateinit var auth: FirebaseAuth

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMoreBinding.inflate(layoutInflater)
        setContentView(binding.root)


        auth = FirebaseAuth.getInstance()

        binding.goToSignin.setOnClickListener {
            startActivity(Intent(this, FoldersActivity::class.java))
        }
        if (auth.currentUser == null) {
            startActivity(Intent(this, signin::class.java))
            finish()
        }

        binding.signOut.setOnClickListener {
            auth.signOut()
            binding.userDetails.text = updateData()
        }

        binding.linearLayout10.setOnClickListener {
            startActivity(Intent(this, SeeAOne::class.java))

        }
        supportActionBar?.apply {
            setBackgroundDrawable(
                ContextCompat.getDrawable(
                    this@More,
                    R.drawable.background_actionbar
                )
            )
        }
        binding.appThemelayout.setOnClickListener {

        }
        setActionBarGradient()
        relativeLayout = binding.relativeLayoutMore

        // Set the background color of SwipeRefreshLayout based on app theme
        setRelativeLayoutBackgroundColor()


    }
    private fun  setRelativeLayoutBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            relativeLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
        } else {
            // Light mode is enabled, set background color to white
            relativeLayout.setBackgroundColor(resources.getColor(android.R.color.white))
        }
    }

    override fun onResume() {
        super.onResume()
        binding.userDetails.text = updateData()

    }

    private fun updateData(): String {

        return "Name : ${auth.currentUser?.displayName}"

    }


    private fun setActionBarGradient() {
        // Check if light mode is applied
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
            // Set gradient background for action bar
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@More,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        }
    }
}
