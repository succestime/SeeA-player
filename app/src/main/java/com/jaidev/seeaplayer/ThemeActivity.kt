package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.ActivityThemeBinding

class ThemeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this, theme)
        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Theme Activity"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        themeApply()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun themeApply() {
        binding.lightTheme.setOnClickListener {
            applyTheme(ThemeHelper.LIGHT_MODE)
        }
        binding.darkTheme.setOnClickListener {
            applyTheme(ThemeHelper.DARK_MODE)
        }
binding.adaptiveTheme.setOnClickListener {
    applyTheme(ThemeHelper.ADAPTIVE_MODE)
}
        binding.view1.setOnClickListener {
            applyTheme(ThemeHelper.Light_Red_MODE)
        }
        binding.view2.setOnClickListener {
            applyTheme(ThemeHelper.Light_Orange_MODE)
        }
        binding.view3.setOnClickListener {
            applyTheme(ThemeHelper.Light_Yellow_MODE)
        }
        binding.view4.setOnClickListener {
            applyTheme(ThemeHelper.Saddle_Brown_MODE)
        }
        binding.view5.setOnClickListener {
            applyTheme(ThemeHelper.Lime_Green_MODE)
        }
        binding.view6.setOnClickListener {
            applyTheme(ThemeHelper.Light_Green_MODE)
        }
        binding.view7.setOnClickListener {
            applyTheme(ThemeHelper.Bright_Green_MODE)
        }
        binding.view8.setOnClickListener {
            applyTheme(ThemeHelper.Forest_Green_MODE)
        }
        binding.view9.setOnClickListener {
            applyTheme(ThemeHelper.Light_Blue_MODE)
        }
        binding.view10.setOnClickListener {
            applyTheme(ThemeHelper.Bright_Blue_MODE)
        }
        binding.view11.setOnClickListener {
            applyTheme(ThemeHelper.Purple_MODE)
        }
        binding.view12.setOnClickListener {
            applyTheme(ThemeHelper.Bright_Purple_MODE)
        }
        binding.view13.setOnClickListener {
            applyTheme(ThemeHelper.Dark_Purple_MODE)
        }
        binding.view14.setOnClickListener {
            applyTheme(ThemeHelper.Pink_MODE)
        }
        binding.view15.setOnClickListener {
            applyTheme(ThemeHelper.Bright_Pink_MODE)
        }
        binding.view16.setOnClickListener {
            applyTheme(ThemeHelper.Light_Gray_MODE)
        }





    }

    private fun applyTheme(theme: String) {
        ThemeHelper.saveTheme(this, theme)
        ThemeHelper.applyTheme(this, theme)
        val intent = Intent("THEME_CHANGED")
        sendBroadcast(intent)
          val intented = Intent("THEME_CHANGEDED")
        sendBroadcast(intented)
        val intentFolder= Intent("THEME_CHANGED_FOLDER")
        sendBroadcast(intentFolder)

        recreate()
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
