package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.jaidev.seeaplayer.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private var checkedItem: Int = 0
    private val CHECKED_ITEM = "checked_item"
    private lateinit var binding: ActivitySplashBinding
    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply your chosen theme here
        when (getCheckedItem()) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
supportActionBar?.hide()

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
            finish()
        }, 2000)



    }

    private fun getCheckedItem(): Int {
        return this.getSharedPreferences("YourSharedPreferencesName", Context.MODE_PRIVATE)
            .getInt(CHECKED_ITEM, checkedItem)
    }




}