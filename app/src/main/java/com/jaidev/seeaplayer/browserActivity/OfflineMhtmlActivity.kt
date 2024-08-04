package com.jaidev.seeaplayer.browserActivity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.ActivityOfflineMhtmlBinding
import java.io.File

class OfflineMhtmlActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var linearLayout14: LinearLayout
    private lateinit var binding : ActivityOfflineMhtmlBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this,theme)
        binding = ActivityOfflineMhtmlBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        webView = findViewById(R.id.webViewTwo)
        // Get the file path from the intent
        val filePath = intent.getStringExtra("filePath")
        if (filePath != null) {
            loadMhtmlFile(filePath)

            // Set the file name as the text of the TextView
            val fileName = getFileName(filePath)
            findViewById<TextView>(R.id.tv_search_query).text = fileName

        } else {
            Log.e("OfflineMhtmlActivity", "File path is null")
        }

        binding.btnClose.setOnClickListener {
            finish()
        }



        linearLayout14 = binding.linearLayout14
        setSwipeRefreshBackgroundColor()
    }


    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            linearLayout14.setBackgroundColor(resources.getColor(R.color.black_statusBar))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)
        } else {
            linearLayout14.setBackgroundColor(resources.getColor(R.color.light_statusBar))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    private fun getFileName(filePath: String): String {
        val file = File(filePath)
        return file.name // This will return the name of the file without the path
    }
    @SuppressLint("SetJavaScriptEnabled")
    private fun loadMhtmlFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            webView.settings.apply {
                allowFileAccess = true
                javaScriptEnabled = true
                builtInZoomControls = true
                displayZoomControls = false // Set to true if you want to display zoom controls
            }
            webView.webViewClient = WebViewClient()
            webView.loadUrl("file:///$filePath")
        } else {
            Log.e("OfflineMhtmlActivity", "File does not exist")
        }
    }



    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

}