package com.jaidev.seeaplayer

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.ActivityApplyThemeBinding

class ApplyThemeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityApplyThemeBinding
    private var selectedTheme: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplyThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSwipeRefreshBackgroundColor()
        supportActionBar?.hide()
        binding.backButton.setOnClickListener {
            onBackPressed()
        }
        updateUseNowButton()
        // Set up color views
        setUpColorView(binding.colorView1, binding.selectedIndicator, ThemeHelper.Light_Red_MODE)
        setUpColorView(binding.colorView2, binding.selectedIndicator2, ThemeHelper.Light_Orange_MODE)
        setUpColorView(binding.colorView3, binding.selectedIndicator3, ThemeHelper.Light_Yellow_MODE)
        setUpColorView(binding.colorView4, binding.selectedIndicator4, ThemeHelper.Saddle_Brown_MODE)
        setUpColorView(binding.colorView5, binding.selectedIndicator5, ThemeHelper.Lime_Green_MODE)
        setUpColorView(binding.colorView6, binding.selectedIndicator6, ThemeHelper.Light_Green_MODE)
        setUpColorView(binding.colorView7, binding.selectedIndicator7, ThemeHelper.Bright_Green_MODE)
        setUpColorView(binding.colorView8, binding.selectedIndicator8, ThemeHelper.Forest_Green_MODE)
        setUpColorView(binding.colorView9, binding.selectedIndicator9, ThemeHelper.Light_Blue_MODE)
        setUpColorView(binding.colorView10, binding.selectedIndicator10, ThemeHelper.Bright_Blue_MODE)
        setUpColorView(binding.colorView11, binding.selectedIndicator11, ThemeHelper.Purple_MODE)
        setUpColorView(binding.colorView12, binding.selectedIndicator12, ThemeHelper.Bright_Purple_MODE)
        setUpColorView(binding.colorView13, binding.selectedIndicator13, ThemeHelper.Dark_Purple_MODE)
        setUpColorView(binding.colorView14, binding.selectedIndicator14, ThemeHelper.Pink_MODE)
        setUpColorView(binding.colorView15, binding.selectedIndicator15, ThemeHelper.Bright_Pink_MODE)
        setUpColorView(binding.colorView16, binding.selectedIndicator16, ThemeHelper.Light_Gray_MODE)
        setUpColorView(binding.colorView17, binding.selectedIndicator17, ThemeHelper.DARK_Light_Red_MODE)
        setUpColorView(binding.colorView18, binding.selectedIndicator18, ThemeHelper.DARK_Light_Orange_MODE)
        setUpColorView(binding.colorView19, binding.selectedIndicator19, ThemeHelper.DARK_Light_Yellow_MODE)
        setUpColorView(binding.colorView20, binding.selectedIndicator20, ThemeHelper.DARK_Saddle_Brown_MODE)
        setUpColorView(binding.colorView21, binding.selectedIndicator21, ThemeHelper.DARK_Lime_Green_MODE)
        setUpColorView(binding.colorView22, binding.selectedIndicator22, ThemeHelper.DARK_Light_Green_MODE)
        setUpColorView(binding.colorView23, binding.selectedIndicator23, ThemeHelper.DARK_Bright_Green_MODE)
        setUpColorView(binding.colorView24, binding.selectedIndicator24, ThemeHelper.DARK_Forest_Green_MODE)
        setUpColorView(binding.colorView25, binding.selectedIndicator25, ThemeHelper.DARK_Light_Blue_MODE)
        setUpColorView(binding.colorView26, binding.selectedIndicator26, ThemeHelper.DARK_Bright_Blue_MODE)
        setUpColorView(binding.colorView27, binding.selectedIndicator27, ThemeHelper.DARK_Purple_MODE)
        setUpColorView(binding.colorView28, binding.selectedIndicator28, ThemeHelper.DARK_Bright_Purple_MODE)
        setUpColorView(binding.colorView29, binding.selectedIndicator29, ThemeHelper.DARK_Dark_Purple_MODE)
        setUpColorView(binding.colorView30, binding.selectedIndicator30, ThemeHelper.DARK_Pink_MODE)
        setUpColorView(binding.colorView31, binding.selectedIndicator31, ThemeHelper.DARK_Bright_Pink_MODE)

        // Get the selected theme from the intent and show the corresponding indicator
        selectedTheme = intent.getStringExtra(ThemeActivity.SELECTED_THEME_KEY)
        showSelectedIndicator(selectedTheme)

        // Set up use now button
        updateUseNowButton()
        binding.useNowButton.setOnClickListener {
            val savedTheme = ThemeHelper.getSavedTheme(this)
            if (selectedTheme == savedTheme) {
                binding.useNowButton.isClickable = false
            }else{
                selectedTheme?.let { theme ->
                    applyTheme(theme)
                }
            }
        }

    }

    private fun setUpColorView(colorView: View, selectedIndicator: View, theme: String) {
        colorView.setOnClickListener {
            // Hide all other indicators
            hideAllIndicators()
            // Show the selected indicator
            selectedIndicator.visibility = View.VISIBLE
            // Update selected theme
            selectedTheme = theme

            updateUseNowButton()

        }
        // Set visibility of new indicators
        val savedTheme = ThemeHelper.getSavedTheme(this)
        updateThemeIndicators(savedTheme)
    }

    private fun hideAllIndicators() {
        // Hide all selected indicators
        binding.selectedIndicator.visibility = View.INVISIBLE
        binding.selectedIndicator2.visibility = View.INVISIBLE
        binding.selectedIndicator3.visibility = View.INVISIBLE
        binding.selectedIndicator4.visibility = View.INVISIBLE
        binding.selectedIndicator5.visibility = View.INVISIBLE
        binding.selectedIndicator6.visibility = View.INVISIBLE
        binding.selectedIndicator7.visibility = View.INVISIBLE
        binding.selectedIndicator8.visibility = View.INVISIBLE
        binding.selectedIndicator9.visibility = View.INVISIBLE
        binding.selectedIndicator10.visibility = View.INVISIBLE
        binding.selectedIndicator11.visibility = View.INVISIBLE
        binding.selectedIndicator12.visibility = View.INVISIBLE
        binding.selectedIndicator13.visibility = View.INVISIBLE
        binding.selectedIndicator14.visibility = View.INVISIBLE
        binding.selectedIndicator15.visibility = View.INVISIBLE
        binding.selectedIndicator16.visibility = View.INVISIBLE
        binding.selectedIndicator17.visibility = View.INVISIBLE
        binding.selectedIndicator18.visibility = View.INVISIBLE
        binding.selectedIndicator19.visibility = View.INVISIBLE
        binding.selectedIndicator20.visibility = View.INVISIBLE
        binding.selectedIndicator21.visibility = View.INVISIBLE
        binding.selectedIndicator22.visibility = View.INVISIBLE
        binding.selectedIndicator23.visibility = View.INVISIBLE
        binding.selectedIndicator24.visibility = View.INVISIBLE
        binding.selectedIndicator25.visibility = View.INVISIBLE
        binding.selectedIndicator26.visibility = View.INVISIBLE
        binding.selectedIndicator27.visibility = View.INVISIBLE
        binding.selectedIndicator28.visibility = View.INVISIBLE
        binding.selectedIndicator29.visibility = View.INVISIBLE
        binding.selectedIndicator30.visibility = View.INVISIBLE
        binding.selectedIndicator31.visibility = View.INVISIBLE
    }

    private fun showSelectedIndicator(selectedTheme: String?) {
        when (selectedTheme) {
            ThemeHelper.Light_Red_MODE -> binding.selectedIndicator.visibility = View.VISIBLE
            ThemeHelper.Light_Orange_MODE -> binding.selectedIndicator2.visibility = View.VISIBLE
            ThemeHelper.Light_Yellow_MODE -> binding.selectedIndicator3.visibility = View.VISIBLE
            ThemeHelper.Saddle_Brown_MODE -> binding.selectedIndicator4.visibility = View.VISIBLE
            ThemeHelper.Lime_Green_MODE -> binding.selectedIndicator5.visibility = View.VISIBLE
            ThemeHelper.Light_Green_MODE -> binding.selectedIndicator6.visibility = View.VISIBLE
            ThemeHelper.Bright_Green_MODE -> binding.selectedIndicator7.visibility = View.VISIBLE
            ThemeHelper.Forest_Green_MODE -> binding.selectedIndicator8.visibility = View.VISIBLE
            ThemeHelper.Light_Blue_MODE -> binding.selectedIndicator9.visibility = View.VISIBLE
            ThemeHelper.Bright_Blue_MODE -> binding.selectedIndicator10.visibility = View.VISIBLE
            ThemeHelper.Purple_MODE -> binding.selectedIndicator11.visibility = View.VISIBLE
            ThemeHelper.Bright_Purple_MODE -> binding.selectedIndicator12.visibility = View.VISIBLE
            ThemeHelper.Dark_Purple_MODE -> binding.selectedIndicator13.visibility = View.VISIBLE
            ThemeHelper.Pink_MODE -> binding.selectedIndicator14.visibility = View.VISIBLE
            ThemeHelper.Bright_Pink_MODE -> binding.selectedIndicator15.visibility = View.VISIBLE
            ThemeHelper.Light_Gray_MODE -> binding.selectedIndicator16.visibility = View.VISIBLE
            ThemeHelper.DARK_Light_Red_MODE -> binding.selectedIndicator17.visibility = View.VISIBLE
            ThemeHelper.DARK_Light_Orange_MODE -> binding.selectedIndicator18.visibility = View.VISIBLE
            ThemeHelper.DARK_Light_Yellow_MODE -> binding.selectedIndicator19.visibility = View.VISIBLE
            ThemeHelper.DARK_Saddle_Brown_MODE -> binding.selectedIndicator20.visibility = View.VISIBLE
            ThemeHelper.DARK_Lime_Green_MODE -> binding.selectedIndicator21.visibility = View.VISIBLE
            ThemeHelper.DARK_Light_Green_MODE -> binding.selectedIndicator22.visibility = View.VISIBLE
            ThemeHelper.DARK_Bright_Green_MODE -> binding.selectedIndicator23.visibility = View.VISIBLE
            ThemeHelper.DARK_Forest_Green_MODE -> binding.selectedIndicator24.visibility = View.VISIBLE
            ThemeHelper.DARK_Light_Blue_MODE -> binding.selectedIndicator25.visibility = View.VISIBLE
            ThemeHelper.DARK_Bright_Blue_MODE -> binding.selectedIndicator26.visibility = View.VISIBLE
            ThemeHelper.DARK_Purple_MODE -> binding.selectedIndicator27.visibility = View.VISIBLE
            ThemeHelper.DARK_Bright_Purple_MODE -> binding.selectedIndicator28.visibility = View.VISIBLE
            ThemeHelper.DARK_Dark_Purple_MODE -> binding.selectedIndicator29.visibility = View.VISIBLE
            ThemeHelper.DARK_Pink_MODE -> binding.selectedIndicator30.visibility = View.VISIBLE
            ThemeHelper.DARK_Bright_Pink_MODE -> binding.selectedIndicator31.visibility = View.VISIBLE
        }
    }

    private fun applyTheme(theme: String) {

        binding.mainActivityProgressbar.visibility = View.VISIBLE
        ThemeHelper.saveTheme(this, theme)
        ThemeHelper.applyTheme(this, theme)
        val intent = Intent("THEME_CHANGED")
        sendBroadcast(intent)
        val intentChanged = Intent("THEME_CHANGEDED")
        sendBroadcast(intentChanged)
        val intentFolder = Intent("THEME_CHANGED_FOLDER")
        sendBroadcast(intentFolder)
        val intentThemeActivity = Intent("THEME_CHANGED_THEME_ACTIVITY")
        sendBroadcast(intentThemeActivity)
        onBackPressed()

        // Recreate the activity after a delay to ensure the progress bar is visible during the process
        binding.root.postDelayed({
            binding.mainActivityProgressbar.visibility = View.GONE
            recreate()
        }, 100) // Adjust delay time if needed
    }
    private fun updateThemeIndicators(theme: String) {
        val themeIndicators = mapOf(
            ThemeHelper.Light_Red_MODE to binding.newIndicator,
            ThemeHelper.Light_Orange_MODE to binding.newIndicator1,
            ThemeHelper.Light_Yellow_MODE to binding.newIndicator2,
            ThemeHelper.Saddle_Brown_MODE to binding.newIndicator3,
            ThemeHelper.Lime_Green_MODE to binding.newIndicator4,
            ThemeHelper.Light_Green_MODE to binding.newIndicator5,
            ThemeHelper.Bright_Green_MODE to binding.newIndicator6,
            ThemeHelper.Forest_Green_MODE to binding.newIndicator7,
            ThemeHelper.Light_Blue_MODE to binding.newIndicator8,
            ThemeHelper.Bright_Blue_MODE to binding.newIndicator9,
            ThemeHelper.Purple_MODE to binding.newIndicator10,
            ThemeHelper.Bright_Purple_MODE to binding.newIndicator11,
            ThemeHelper.Dark_Purple_MODE to binding.newIndicator12,
            ThemeHelper.Pink_MODE to binding.newIndicator13,
            ThemeHelper.Bright_Pink_MODE to binding.newIndicator14,
            ThemeHelper.Light_Gray_MODE to binding.newIndicator15,
            ThemeHelper.DARK_Light_Red_MODE to binding.newIndicator16,
            ThemeHelper.DARK_Light_Orange_MODE to binding.newIndicator17,
            ThemeHelper.DARK_Light_Yellow_MODE to binding.newIndicator18,
            ThemeHelper.DARK_Saddle_Brown_MODE to binding.newIndicator19,
            ThemeHelper.DARK_Lime_Green_MODE to binding.newIndicator20,
            ThemeHelper.DARK_Light_Green_MODE to binding.newIndicator21,
            ThemeHelper.DARK_Bright_Green_MODE to binding.newIndicator22,
            ThemeHelper.DARK_Forest_Green_MODE to binding.newIndicator23,
            ThemeHelper.DARK_Light_Blue_MODE to binding.newIndicator24,
            ThemeHelper.DARK_Bright_Blue_MODE to binding.newIndicator25,
            ThemeHelper.DARK_Purple_MODE to binding.newIndicator26,
            ThemeHelper.DARK_Bright_Purple_MODE to binding.newIndicator27,
            ThemeHelper.DARK_Dark_Purple_MODE to binding.newIndicator28,
            ThemeHelper.DARK_Pink_MODE to binding.newIndicator29,
            ThemeHelper.DARK_Bright_Pink_MODE to binding.newIndicator30
        )

        themeIndicators.values.forEach {
            it.visibility = View.INVISIBLE
        }

        themeIndicators[theme]?.visibility = View.VISIBLE
    }
    private fun updateUseNowButton() {
        val savedTheme = ThemeHelper.getSavedTheme(this)
        if (selectedTheme == savedTheme) {
            binding.useNowButton.setBackgroundResource(R.drawable.button_background_gray)
            binding.useNowButton.text = "Using"
            binding.useNowButton.setTextColor(ContextCompat.getColor(this, R.color.gray))

        } else {
            binding.useNowButton.setBackgroundResource(R.drawable.button_background)
            binding.useNowButton.text = "Use Now"
            binding.useNowButton.setTextColor(ContextCompat.getColor(this, R.color.cool_blue))

        }
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.from_left, R.anim.to_right)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
