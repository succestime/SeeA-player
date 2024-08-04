
package com.jaidev.seeaplayer.dataClass

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.jaidev.seeaplayer.R

object ThemeHelper {
    const val LIGHT_MODE = "Light"
    const val DARK_MODE = "Dark"
    const val ADAPTIVE_MODE = "Adaptive"
    const val Light_Red_MODE = "Light_Red"
     const val Light_Orange_MODE = "Light_Orange"
      const val Light_Yellow_MODE = "Light_Yellow"
     const val Saddle_Brown_MODE = "Saddle_Brown"
     const val Lime_Green_MODE = "Lime_Green"
      const val Light_Green_MODE = "Light_Green"
     const val Bright_Green_MODE = "Bright_Green"
     const val Forest_Green_MODE = "Forest_Green"
     const val Light_Blue_MODE = "Light_Blue"
     const val Bright_Blue_MODE = "Bright_Blue"
      const val Purple_MODE = "Purple"
     const val Bright_Purple_MODE = "Bright_Purple"
     const val Dark_Purple_MODE = "Dark_Purple"
     const val Pink_MODE = "Pink"
     const val Bright_Pink_MODE = "Bright_Pink"
     const val Light_Gray_MODE = "Light_Gray"


    fun applyTheme(context: Context,theme: String) {
        when (theme) {
            LIGHT_MODE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            DARK_MODE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ADAPTIVE_MODE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        val themeResId = when (theme) {
            Light_Red_MODE -> R.style.coolBlueNav1
            Light_Orange_MODE -> R.style.coolBlueNav2
            Light_Yellow_MODE -> R.style.coolBlueNav3
            Saddle_Brown_MODE -> R.style.coolBlueNav4
            Lime_Green_MODE -> R.style.coolBlueNav5
            Light_Green_MODE -> R.style.coolBlueNav6
            Bright_Green_MODE -> R.style.coolBlueNav7
            Forest_Green_MODE -> R.style.coolBlueNav8
            Light_Blue_MODE -> R.style.coolBlueNav9
            Bright_Blue_MODE -> R.style.coolBlueNav10
            Purple_MODE -> R.style.coolBlueNav11
            Bright_Purple_MODE -> R.style.coolBlueNav12
            Dark_Purple_MODE -> R.style.coolBlueNav13
            Pink_MODE -> R.style.coolBlueNav14
            Bright_Pink_MODE -> R.style.coolBlueNav15
            Light_Gray_MODE -> R.style.coolBlueNav16

            else -> R.style.coolBlueNav
        }

        context.setTheme(themeResId)


    }

    fun getSavedTheme(context: Context): String {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString("app_theme", LIGHT_MODE) ?: LIGHT_MODE
    }

    fun saveTheme(context: Context, theme: String) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putString("app_theme", theme).apply()
    }

}
