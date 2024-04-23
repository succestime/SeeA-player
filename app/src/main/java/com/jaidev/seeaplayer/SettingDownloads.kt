package com.jaidev.seeaplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jaidev.seeaplayer.databinding.ActivitySettingDownloadsBinding

class SettingDownloads : AppCompatActivity() {
    private lateinit var binding : ActivitySettingDownloadsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Enable Up button (back button)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Order of Downloaded files"
    }


}