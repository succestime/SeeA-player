package com.jaidev.seeaplayer.browserActivity

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.caverock.androidsvg.SVG
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.ActivityImageViewerBinding
import java.io.File

class ImageViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageViewerBinding
    private var imagePath: String? = null
    private lateinit var swipeRefreshLayout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this,theme)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable up button in ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Retrieve the image path from Intent extras or data
        imagePath = intent.getStringExtra("imagePath") ?: getImagePathFromIntent(intent)

        // Set the ActionBar title to the image name
        imagePath?.let {
            val fileName = File(it).name
            supportActionBar?.title = fileName
        } ?: run {
            supportActionBar?.title = "Image Viewer"
        }

        // Load the image into the PhotoView
        if (!imagePath.isNullOrBlank()) {
            // Define target width and height
            val targetWidth = 1024 // Specify the desired target width
            val targetHeight = 1024 // Specify the desired target height
            // Decode the image file to get its dimensions
            val options = BitmapFactory.Options().apply {
                // Set inJustDecodeBounds to true to only decode the image dimensions without loading the full bitmap into memory
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)

            // Calculate the sample size to scale down the image
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, targetWidth, targetHeight)

            // Set the sample size in BitmapFactory options
            val bitmapOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }

            // Decode the image file with the scaled sample size
            val bitmap = BitmapFactory.decodeFile(imagePath, bitmapOptions)
            binding.photoView.setImageBitmap(bitmap)
        }

        loadImage()
        swipeRefreshLayout = binding.ImageViewerActivity

        // Set the background color of SwipeRefreshLayout based on app theme
        setSwipeRefreshBackgroundColor()
    }

    private fun getImagePathFromIntent(intent: Intent): String? {
        val dataUri = intent.data ?: return null
        return dataUri.path?.let { File(it).absolutePath }
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

    private fun loadImage() {
        if (!imagePath.isNullOrBlank()) {
            val file = File(imagePath!!)
            if (file.exists()) {
                if (imagePath!!.endsWith(".svg", ignoreCase = true)) {
                    // Load SVG image
                    loadSvgImage(file)
                } else {
                    // Load regular image
                    loadRegularImage(file)
                }
            } else {
                // Handle the case where the image file doesn't exist
                // You can show a placeholder image or a message to the user
                binding.photoView.setImageResource(R.drawable.image_browser)
            }
        }
    }

    private fun loadSvgImage(file: File) {
        // Load SVG image using AndroidSVG library
        try {
            val svg = SVG.getFromInputStream(file.inputStream())
            val drawable = PictureDrawable(svg.renderToPicture())
            binding.photoView.setImageDrawable(drawable)
        } catch (e: Exception) {
            // Handle any exceptions
            e.printStackTrace()
            // Show placeholder image on error
            binding.photoView.setImageResource(R.drawable.image_browser)
        }
    }

    private fun loadRegularImage(file: File) {
        // Load regular image using Glide
        Glide.with(this)
            .load(Uri.fromFile(file))
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.image_browser) // Placeholder image while loading
                    .error(R.drawable.image_browser) // Image to display on error
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both original & resized image
            )
            .into(binding.photoView)
    }

    private fun calculateSampleSize(imageWidth: Int, imageHeight: Int, targetWidth: Int, targetHeight: Int): Int {
        var inSampleSize = 1

        if (imageHeight > targetHeight || imageWidth > targetWidth) {
            val halfHeight = imageHeight / 2
            val halfWidth = imageWidth / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= targetHeight && (halfWidth / inSampleSize) >= targetWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
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
