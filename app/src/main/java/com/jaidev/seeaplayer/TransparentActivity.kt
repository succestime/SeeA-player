package com.jaidev.seeaplayer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jaidev.seeaplayer.dataClass.DownloadBottomSheetFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException

class TransparentActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transparent) // Assuming your layout file is activity_transparent.xml
        window.statusBarColor = Color.parseColor("#121212")
        progressBar = findViewById(R.id.progressBar)

        if (intent?.action == Intent.ACTION_SEND) {
            if ("text/plain" == intent.type) {
                handleSendText(intent) // Handle text being sent
            }
        }
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
            if (sharedText.isNotEmpty()) {
                val urls = extractUrls(sharedText)
                if (urls.isNotEmpty()) {
                    val videoUrl = urls[0] // Assuming the first URL is the video URL
                    // Remove any existing fragment before showing a new one
                    val existingFragment =
                        supportFragmentManager.findFragmentByTag("DownloadBottomSheetFragment")
                    if (existingFragment != null) {
                        supportFragmentManager.beginTransaction().remove(existingFragment).commit()
                    }
                    // Show the progress bar
                    progressBar.visibility = View.VISIBLE

                    fetchDownloadLink(videoUrl)
                }
            }
        }
    }

    private fun extractUrls(text: String): List<String> {
        val regex = Regex("(https?|ftp)://[^\\s/$.?#].[^\\s]*")
        return regex.findAll(text).map { it.value }.toList()
    }

    private fun fetchDownloadLink(url: String) {
        lifecycleScope.launch {
            val (title, downloadLink) = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use null to null
                        val html = response.body?.string()
                        Log.d("HTML Content", html ?: "No HTML content")
                        val document = html?.let { Jsoup.parse(it) }
                        val title = document?.title()
                        val imageUrl = document?.select("meta[property=og:image]")?.attr("content")
                        title to imageUrl
                        // Modify this line according to your logic for extracting the download link
                        val downloadLink = document?.select("a.download-link")?.attr("href")
                        title to downloadLink
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    null to null
                }
            }

            if (title != null && downloadLink != null) {
                Toast.makeText(
                    this@TransparentActivity,
                    "Download link: $downloadLink",
                    Toast.LENGTH_LONG
                ).show()
                val fragment = DownloadBottomSheetFragment.newInstance(title, downloadLink)
                fragment.show(supportFragmentManager, "DownloadBottomSheetFragment")
            } else {
                Toast.makeText(
                    this@TransparentActivity,
                    "Failed to fetch download link",
                    Toast.LENGTH_SHORT
                ).show()
            }
            progressBar.visibility = View.GONE
        }
    }


//    private fun fetchUrlDetails(url: String) {
//        lifecycleScope.launch {
//            val (title, imageUrl) = withContext(Dispatchers.IO) {
//                try {
//                    val client = OkHttpClient()
//                    val request = Request.Builder().url(url).build()
//                    client.newCall(request).execute().use { response ->
//                        if (!response.isSuccessful) return@use null to null
//                        val html = response.body?.string()
//                        val document = Jsoup.parse(html)
//                        val title = document.title()
//
//
//                    }
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                    null to null
//                }
//            }
//
//            if (title != null && imageUrl != null) {
//                // Create and show the DownloadBottomSheetFragment with the title and image URL
//                val fragment = DownloadBottomSheetFragment.newInstance(title, imageUrl)
//                fragment.show(supportFragmentManager, "DownloadBottomSheetFragment")
//            } else {
//                // Handle the error case here, e.g., show an error message
//            }
//            // Hide the progress bar
//            progressBar.visibility = View.GONE
//        }
//    }

}