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
    private lateinit var binding: ActivityFeedBackBinding
    private lateinit var swipeRefreshLayout: LinearLayout

    companion object {
        private const val EMAIL_REQUEST_CODE = 1
    }

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
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("seeaplayer1019@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback to SeeA Player: $subject")
                    putExtra(Intent.EXTRA_TEXT, """
                        Hello,
                        
                        Topic: $subject
                        
                        ${if (email.isNotEmpty()) "User's Email: $email\n" else ""}
                        
                        Feedback: $feedbackMsg
                        
                        ////////////////////***** SeeA Player Feedback *****////////////////////
                        
                        Regards,
                        SeeA Player
                    """.trimIndent())
                    setPackage("com.google.android.gm") // This restricts the intent to the Gmail app
                }

                try {
                    startActivityForResult(emailIntent, EMAIL_REQUEST_CODE)
                } catch (e: Exception) {
                    Toast.makeText(this, "Gmail app is not installed.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show()
            }
        }

        setActionBarGradient()
        swipeRefreshLayout = binding.feedBackActivity
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
            swipeRefreshLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
        } else {
            swipeRefreshLayout.setBackgroundColor(resources.getColor(android.R.color.white))
        }
    }

    private fun setActionBarGradient() {
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            supportActionBar?.apply {
                setBackgroundDrawable(ContextCompat.getDrawable(this@FeedBackActivity, R.drawable.background_actionbar_light))
            }
        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            supportActionBar?.apply {
                setBackgroundDrawable(ContextCompat.getDrawable(this@FeedBackActivity, R.drawable.background_actionbar))
            }
        } else {
            val isSystemDefaultDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
                android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
                else -> false
            }
            if (isSystemDefaultDarkMode) {
                supportActionBar?.apply {
                    setBackgroundDrawable(ContextCompat.getDrawable(this@FeedBackActivity, R.drawable.background_actionbar))
                }
            } else {
                supportActionBar?.apply {
                    setBackgroundDrawable(ContextCompat.getDrawable(this@FeedBackActivity, R.drawable.background_actionbar_light))
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EMAIL_REQUEST_CODE) {
            Toast.makeText(this, "Thank you for your feedback. Your feedback is successfully sent to SeeA Player.", Toast.LENGTH_LONG).show()
            finish() // Finish the activity
        }
    }
}
