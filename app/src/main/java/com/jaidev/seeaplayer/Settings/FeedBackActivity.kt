package com.jaidev.seeaplayer.Settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.databinding.ActivityFeedBackBinding

class FeedBackActivity : AppCompatActivity() {
    private lateinit var binding:ActivityFeedBackBinding
    private lateinit var swipeRefreshLayout: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedBackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Feedback"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.sendFA.setOnClickListener {
            val feedbackMsg = binding.feedbackMsgFA.text.toString()
            val subject = binding.topicFA.text.toString()
            val email = binding.emailFA.text.toString()

            if (feedbackMsg.isNotEmpty() && subject.isNotEmpty()) {
                val emailIntent = Intent(Intent.ACTION_SENDTO)
                emailIntent.data = Uri.parse("mailto:")
                emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("seeaplayer1019@gmail.com"))
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback to SeeA Player: $subject")
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Feedback message: $feedbackMsg\nUser's email: $email")
                emailIntent.putExtra(Intent.EXTRA_TEXT, """
                    Hello,
                    
                    Topic: $subject
                    
                    ${if (email.isNotEmpty()) "User's Email: $email\n" else ""}
                    
                    Feedback: $feedbackMsg
                    
                    ////////////////////***** SeeA Player Feedback *****////////////////////
                    
                    Regards,
                    SeeA Player
                """.trimIndent())

                try {
                    startActivity(Intent.createChooser(emailIntent, "Send feedback via..."))
                } catch (e: Exception) {
                    Toast.makeText(this, "No email clients installed.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show()
            }
        }

        setActionBarGradient()
        swipeRefreshLayout = binding.feedBackActivity

        // Set the background color of SwipeRefreshLayout based on app theme
        setSwipeRefreshBackgroundColor()
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            swipeRefreshLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
        } else {
            // Light mode is enabled, set background color to white
            swipeRefreshLayout.setBackgroundColor(resources.getColor(android.R.color.white))
        }
    }
    private fun setActionBarGradient() {
        // Check the current night mode
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            // Light mode is applied
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@FeedBackActivity,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            // Dark mode is applied
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@FeedBackActivity,
                        R.drawable.background_actionbar
                    )
                )
            }
        } else {
            // System Default mode is applied
            val isSystemDefaultDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
                android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
                else -> false
            }
            // Set the ActionBar color based on the System Default mode
            if (isSystemDefaultDarkMode) {
                // System Default mode is dark
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@FeedBackActivity,
                            R.drawable.background_actionbar
                        )
                    )
                }
            } else {
                // System Default mode is light
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@FeedBackActivity,
                            R.drawable.background_actionbar_light
                        )
                    )
                }
            }
        }
    }
}