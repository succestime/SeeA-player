package com.jaidev.seeaplayer.browserActivity

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.databinding.ActivityImageViewerBinding
import java.io.File

class ImageViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageViewerBinding
    private var imagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Enable up button in ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Image Viewer"

        // Retrieve the image path from Intent extras
        imagePath = intent.getStringExtra("imagePath")

        // Load the image into the PhotoView
        if (!imagePath.isNullOrBlank()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            binding.photoView.setImageBitmap(bitmap)
        }

        setActionBarGradient()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.image_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share_Image -> {
                shareImage()
                true
            }

            android.R.id.home -> {  // Handle up button click
                onBackPressed()     // Navigate back to parent activity
                true
            }
            R.id.open_Image -> {
                // Handle open image action
                showOpenWithPopupMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun shareImage() {
        // Check if imagePath is valid
        if (!imagePath.isNullOrBlank()) {
            // Create a Uri from the imagePath
            val imageUri = getImageUri(imagePath)

            // Create an intent with ACTION_SEND to share the image
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Start the activity to show the share dialog
            startActivity(Intent.createChooser(shareIntent, "Share Image"))
        }
    }


    private fun showOpenWithPopupMenu() {
        // Check if imagePath is valid
        if (!imagePath.isNullOrBlank()) {
            // Create a PopupMenu anchored to the "Open Image" menu item
            val openWithPopupMenu = PopupMenu(this, findViewById(R.id.open_Image))

            // Set the title of the popup menu
            openWithPopupMenu.menu.add("Open with")

            // Set up a click listener for the popup menu items
            openWithPopupMenu.setOnMenuItemClickListener { menuItem ->
                // Handle the menu item click event
                when (menuItem.title) {
                    "Open with" -> {
                        openImageWithDefaultApp()
                        true
                    }
                    else -> false
                }
            }

            // Show the popup menu
            openWithPopupMenu.show()
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
                        this@ImageViewerActivity,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else {
            // Dark mode is applied or the mode is set to follow system
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@ImageViewerActivity,
                        R.drawable.background_actionbar
                    )
                )
            }
        }
    }
    private fun openImageWithDefaultApp() {
        // Check if imagePath is valid
        if (!imagePath.isNullOrBlank()) {
            // Create an intent to view the image with the default app
            val imageUri = getImageUri(imagePath)
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(imageUri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Verify that there is an activity to handle the intent
            if (openIntent.resolveActivity(packageManager) != null) {
                // Show app chooser dialog to handle the intent
                val chooserIntent = Intent.createChooser(openIntent, "Open with")
                startActivity(chooserIntent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setActionBarGradient()
    }

    private fun getImageUri(imagePath: String?): Uri? {
        return imagePath?.let {
            val imageFile = File(it)
            FileProvider.getUriForFile(
                this@ImageViewerActivity,
                "${packageName}.provider",
                imageFile
            )
        }
    }

}
