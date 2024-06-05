package com.jaidev.seeaplayer.Settings

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.browserActivity.FileActivity
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.databinding.ActivityMoreSettingNavBinding

class MoreSettingNav : AppCompatActivity() {
private lateinit var binding:ActivityMoreSettingNavBinding
    private lateinit var swipeRefreshLayout: ConstraintLayout
    private var checkedItem: Int = 0
    private var selected: String = ""
    private val CHECKED_ITEM = "checked_item"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoreSettingNavBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setActionBarGradient()
        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable the Up button

        binding.appThemelayout.setOnClickListener {
            showDialog()
        }
        binding.Settingslayout.setOnClickListener {
            val menuItems = arrayOf(
                "Latest",
                "Oldest",
                "Name(A to Z)",
                "Name(Z to A)",
                "File Size(Smallest)",
                "File Size(Largest)"
            )
            var value = MainActivity.sortValue
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Sort By")
                .setPositiveButton("OK") { _, _ ->
                    val sortEditor = getSharedPreferences("Sorting", MODE_PRIVATE).edit()
                    sortEditor.putInt("sortValue", value)
                    sortEditor.apply()

                    //for restarting app
                    finish()
                    startActivity(intent)

                }
                .setSingleChoiceItems(menuItems, MainActivity.sortValue) { _, pos ->
                    value = pos
                }
                .create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.BLACK)
        }

binding.mySubscribeLayout.setOnClickListener {
    val intent = Intent(this@MoreSettingNav, LinkTubeActivity::class.java)
    startActivity(intent)
}

        binding.signOut.setOnClickListener {
            val intent = Intent(this@MoreSettingNav, FileActivity::class.java)
            startActivity(intent)
        }

        binding.feedBackLayout.setOnClickListener {
            val intent = Intent(this@MoreSettingNav, FeedBackActivity::class.java)
            startActivity(intent)
        }
        binding.AboutSeeAPlayer.setOnClickListener {
            val intent = Intent(this@MoreSettingNav, AboutApp::class.java)
            startActivity(intent)
        }
        swipeRefreshLayout = binding.relativeLayoutMore
        setSwipeRefreshBackgroundColor()
    }


    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            swipeRefreshLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)

        } else {
            // Light mode is enabled, set background color to white
            swipeRefreshLayout.setBackgroundColor(resources.getColor(android.R.color.white))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

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
                       this@MoreSettingNav,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            // Dark mode is applied
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@MoreSettingNav,
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
                            this@MoreSettingNav,
                            R.drawable.background_actionbar
                        )
                    )
                }
            } else {
                // System Default mode is light
               supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@MoreSettingNav,
                            R.drawable.background_actionbar_light
                        )
                    )
                }
            }
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    fun showDialog() {
        val themes = resources.getStringArray(R.array.theme)
        val builder = MaterialAlertDialogBuilder(this@MoreSettingNav)
        builder.setTitle("Select Theme")
        builder.setSingleChoiceItems(
            R.array.theme,
            getCheckedItem()
        ) { dialogInterface: DialogInterface, i: Int ->
            selected = themes[i]
            checkedItem = i
        }

        builder.setPositiveButton("OK") { dialogInterface: DialogInterface, i: Int ->
            if (selected == null) {
                selected = themes[i]
                checkedItem = i
            }

            when (selected) {
                "System Default" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

            }
            setCheckedItem(checkedItem)
        }

        builder.setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun getCheckedItem(): Int {
        return this.getSharedPreferences("YourSharedPreferencesName", Context.MODE_PRIVATE)
            .getInt(CHECKED_ITEM, checkedItem)
    }

    private fun setCheckedItem(i: Int) {
        this.getSharedPreferences("YourSharedPreferencesName", Context.MODE_PRIVATE)
            .edit()
            .putInt(CHECKED_ITEM, i)
            .apply()
    }
}