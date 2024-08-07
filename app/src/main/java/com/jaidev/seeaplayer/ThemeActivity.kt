package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.ActivityThemeBinding

class ThemeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeBinding

    companion object {
        const val SELECTED_THEME_KEY = "SELECTED_THEME_KEY"
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this, theme)
        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyThemeChange()
        setSwipeRefreshBackgroundColor()
        supportActionBar?.title = "App Theme"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        registerReceiver(themeChangeReceiver, IntentFilter("THEME_CHANGED_THEME_ACTIVITY"))

        themeApply()
    }

    private val themeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            applyThemeChange()
            recreate()
        }
    }

    private fun applyThemeChange() {
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this, theme)
        updateThemeIndicators(theme)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(themeChangeReceiver)
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
            openApplyThemeActivity(ThemeHelper.Light_Red_MODE)
        }
        binding.view2.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Light_Orange_MODE)
        }
        binding.view3.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Light_Yellow_MODE)
        }
        binding.view4.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Saddle_Brown_MODE)
        }
        binding.view5.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Lime_Green_MODE)
        }
        binding.view6.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Light_Green_MODE)
        }
        binding.view7.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Bright_Green_MODE)
        }
        binding.view8.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Forest_Green_MODE)
        }
        binding.view9.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Light_Blue_MODE)
        }
        binding.view10.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Bright_Blue_MODE)
        }
        binding.view11.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Purple_MODE)
        }
        binding.view12.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Bright_Purple_MODE)
        }
        binding.view13.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Dark_Purple_MODE)
        }
        binding.view14.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Pink_MODE)
        }
        binding.view15.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Bright_Pink_MODE)
        }
        binding.view16.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.Light_Gray_MODE)
        }
        binding.view17.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Light_Red_MODE)
        }
        binding.view18.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Light_Orange_MODE)
        }
        binding.view19.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Light_Yellow_MODE)
        }
        binding.view20.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Saddle_Brown_MODE)
        }
        binding.view21.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Lime_Green_MODE)
        }
        binding.view22.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Light_Green_MODE)
        }
        binding.view23.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Bright_Green_MODE)
        }
        binding.view24.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Forest_Green_MODE)
        }
        binding.view25.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Light_Blue_MODE)
        }
        binding.view26.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Bright_Blue_MODE)
        }
        binding.view27.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Purple_MODE)
        }
        binding.view28.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Bright_Purple_MODE)
        }
        binding.view29.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Dark_Purple_MODE)
        }
        binding.view30.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Pink_MODE)
        }
        binding.view31.setOnClickListener {
            openApplyThemeActivity(ThemeHelper.DARK_Bright_Pink_MODE)
        }

        // Set visibility of new indicators
        val savedTheme = ThemeHelper.getSavedTheme(this)
        updateThemeIndicators(savedTheme)
    }

    private fun applyTheme(theme: String) {
        ThemeHelper.saveTheme(this, theme)
        ThemeHelper.applyTheme(this, theme)
        val intent = Intent("THEME_CHANGED")
        sendBroadcast(intent)
        updateThemeIndicators(theme)
        recreate()
    }

    private fun updateThemeIndicators(theme: String) {
        val themeIndicators = mapOf(
            ThemeHelper.LIGHT_MODE to binding.newIndicator,
            ThemeHelper.DARK_MODE to binding.newIndicator1,
            ThemeHelper.ADAPTIVE_MODE to binding.newIndicator2,
            ThemeHelper.Light_Red_MODE to binding.newIndicator3,
            ThemeHelper.Light_Orange_MODE to binding.newIndicator4,
            ThemeHelper.Light_Yellow_MODE to binding.newIndicator5,
            ThemeHelper.Saddle_Brown_MODE to binding.newIndicator6,
            ThemeHelper.Lime_Green_MODE to binding.newIndicator7,
            ThemeHelper.Light_Green_MODE to binding.newIndicator8,
            ThemeHelper.Bright_Green_MODE to binding.newIndicator9,
            ThemeHelper.Forest_Green_MODE to binding.newIndicator10,
            ThemeHelper.Light_Blue_MODE to binding.newIndicator11,
            ThemeHelper.Bright_Blue_MODE to binding.newIndicator12,
            ThemeHelper.Purple_MODE to binding.newIndicator13,
            ThemeHelper.Bright_Purple_MODE to binding.newIndicator14,
            ThemeHelper.Dark_Purple_MODE to binding.newIndicator15,
            ThemeHelper.Pink_MODE to binding.newIndicator16,
            ThemeHelper.Bright_Pink_MODE to binding.newIndicator17,
            ThemeHelper.Light_Gray_MODE to binding.newIndicator18,
            ThemeHelper.DARK_Light_Red_MODE to binding.newIndicator19,
            ThemeHelper.DARK_Light_Orange_MODE to binding.newIndicator20,
            ThemeHelper.DARK_Light_Yellow_MODE to binding.newIndicator21,
            ThemeHelper.DARK_Saddle_Brown_MODE to binding.newIndicator22,
            ThemeHelper.DARK_Lime_Green_MODE to binding.newIndicator23,
            ThemeHelper.DARK_Light_Green_MODE to binding.newIndicator24,
            ThemeHelper.DARK_Bright_Green_MODE to binding.newIndicator25,
            ThemeHelper.DARK_Forest_Green_MODE to binding.newIndicator26,
            ThemeHelper.DARK_Light_Blue_MODE to binding.newIndicator27,
            ThemeHelper.DARK_Bright_Blue_MODE to binding.newIndicator28,
            ThemeHelper.DARK_Purple_MODE to binding.newIndicator29,
            ThemeHelper.DARK_Bright_Purple_MODE to binding.newIndicator30,
            ThemeHelper.DARK_Dark_Purple_MODE to binding.newIndicator31,
            ThemeHelper.DARK_Pink_MODE to binding.newIndicator32,
            ThemeHelper.DARK_Bright_Pink_MODE to binding.newIndicator33
        )

        themeIndicators.values.forEach {
            it.visibility = View.INVISIBLE
        }

        themeIndicators[theme]?.visibility = View.VISIBLE
    }

    private fun openApplyThemeActivity(selectedTheme: String) {
        val intent = Intent(this, ApplyThemeActivity::class.java).apply {
            putExtra(SELECTED_THEME_KEY, selectedTheme)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
    }

    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)
        } else {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.from_left, R.anim.to_right)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
