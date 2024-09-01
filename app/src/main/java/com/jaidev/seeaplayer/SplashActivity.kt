package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_F_cool_blue)

        // Create a SpannableString to set different styles for different parts of the text
        val spannableString = SpannableString("SeeA Player")

        // Set bold style for "SeeA"
        spannableString.setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set normal style for "Player"
        val normalStart = 5 // Start position of normal text
        val normalEnd = spannableString.length // End position of normal text
        spannableString.setSpan(StyleSpan(android.graphics.Typeface.NORMAL), normalStart, normalEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set the modified SpannableString to the TextView
        binding.textView.text = spannableString

        // Delay for 2 seconds and then start the main activity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            val theme = ThemeHelper.getSavedTheme(this)
            ThemeHelper.applyTheme(this,theme)
            finish()

        }, 200)



    }





}