package com.jaidev.seeaplayer.browserActivity

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.FileAdapter
import com.jaidev.seeaplayer.dataClass.FileDownloader
import com.jaidev.seeaplayer.dataClass.FileItem
import com.jaidev.seeaplayer.dataClass.FileType
import com.jaidev.seeaplayer.databinding.ActivityFileBinding
import com.jaidev.seeaplayer.databinding.ActivitySettingDownloadsBinding
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.util.Locale
import kotlin.concurrent.thread

class FileActivity : AppCompatActivity() , FileAdapter.OnItemClickListener,
    FileAdapter.OnFileCountChangeListener ,
    FileAdapter.OnFileDeleteListener
    , FileAdapter.OnSelectionModeChangeListener
{
    private lateinit var fileListAdapter: FileAdapter
    private val fileItems: MutableList<FileItem> = mutableListOf()
    private lateinit var binding: ActivityFileBinding
    private var selectedBox: View? = null // Track currently selected box
    private var isEditTextVisible = false
    private var sortByNewestFirst = true // Default to sort by newest first
    lateinit var mAdView: AdView
    private lateinit var swipeRefreshLayout: ConstraintLayout
    private val manageAllFilesPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                // Permission granted, load the files
                loadDownloadedFiles()
            }
        }
    companion object {
        private const val REQUEST_MANAGE_ALL_FILES_ACCESS_PERMISSION = 1001
    }
    @SuppressLint("NotifyDataSetChanged", "ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileBinding.inflate(layoutInflater)
        setContentView(binding.root)
       supportActionBar?.hide()
        setActionBarGradient()
        MobileAds.initialize(this){}
        mAdView = findViewById(R.id.adView)
        // banner ads
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        // Set click listeners for each box
        binding.monthlyBox.setOnClickListener { handleBoxClick(binding.monthlyBox) }
        binding.quarterlyBox.setOnClickListener { handleBoxClick(binding.quarterlyBox) }
        binding.annualBox.setOnClickListener { handleBoxClick(binding.annualBox) }
        binding.biennialBox.setOnClickListener { handleBoxClick(binding.biennialBox) }
        binding.pageBox.setOnClickListener { handleBoxClick(binding.pageBox) }

        // Set up RecyclerView and adapter
        fileListAdapter = FileAdapter(this, fileItems,  this, this, this,this)

        binding.recyclerFileView.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(10)
            layoutManager = LinearLayoutManager(this@FileActivity)
            adapter = fileListAdapter
        }

        thread()
        initializeBinding()
        // Call method to retrieve downloaded files
        val fileList = retrieveDownloadedFiles()
        fileItems.addAll(fileList)
        fileListAdapter.notifyDataSetChanged()

        // Additional setup and initialization...
        binding.totalFile.text = "Total Downloaded files : ${fileList.size}"
        selectBox(binding.monthlyBox)

        swipeRefreshLayout = binding.fileActivityLayout
        setSwipeRefreshBackgroundColor()
        updateEmptyStateVisibility()
        checkAndRequestFilePermission()
    }
    private fun checkAndRequestFilePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            showPermissionRequestDialog()
        } else {
            loadDownloadedFiles()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showPermissionRequestDialog() {
        val dialogView = layoutInflater.inflate(R.layout.fileactivity_permission_dialog, null)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.buttonOpenSettings).setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.data = Uri.parse("package:${applicationContext.packageName}")
            manageAllFilesPermissionLauncher.launch(intent)
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.buttonNotNow).setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun loadDownloadedFiles() {
        fileItems.clear()
        val fileList = retrieveDownloadedFiles()
        fileItems.addAll(fileList)
        fileListAdapter.notifyDataSetChanged()
        binding.totalFile.text = "Total Downloaded files: ${fileList.size}"
        selectBox(binding.monthlyBox)
    }
    @SuppressLint("ObsoleteSdkInt")
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
        val navigationBarDividerColor = ContextCompat.getColor(this, R.color.gray)

        // This sets the navigation bar divider color. API 28+ required.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = navigationBarDividerColor
        }
    }
    private fun handleBoxClick(box: View) {
        // Clear the selection in the adapter
        fileListAdapter.clearSelection()

        // Perform box selection logic
        toggleSelection(box)
        onSelectionModeChanged(false)
    }


    private fun thread(){
        thread {
            val websiteUrl = "https://www.google.com"

            val fileDetailsWithWebsiteName = fetchFileDetailsFromWebsite(websiteUrl)

            // Check if file details and website name are retrieved successfully
            if (fileDetailsWithWebsiteName != null) {
                val (fileUrl, fileName, websiteName) = fileDetailsWithWebsiteName

                // Start the file download using the obtained file URL, filename, and website name
                FileDownloader.downloadFile(this@FileActivity, fileUrl, fileName, websiteName)
            } else {
                // Handle error or display a message if file details retrieval fails
            }
        }

    }


    private fun openPlayerActivity(videoUri: String, videoTitle: String) {
        val intent = Intent(this, PlayerFileActivity::class.java).apply {
            putExtra("videoUri", videoUri)
            putExtra("videoTitle", videoTitle)
        }
        startActivity(intent)
    }



    @SuppressLint("ClickableViewAccessibility")
    private fun initializeBinding(){
        binding.settingBrowser.setOnClickListener {
            showSettingDialog()

        }

        binding.clearActivity.setOnClickListener {
            finish()
        }

        binding.imageButtonSearch.setOnClickListener {
            // Show editTextSearch
            binding.editTextSearch.visibility = View.VISIBLE
            binding.imageButtonSearch.visibility = View.GONE
            binding.settingBrowser.visibility = View.GONE
            binding.clearActivity.visibility = View.GONE
            binding.constraintLayout.visibility = View.GONE
            binding.editTextSearch.text?.clear()

            // Set focus to editTextSearch
            binding.editTextSearch.requestFocus()

            // Show the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.editTextSearch, InputMethodManager.SHOW_IMPLICIT)

            isEditTextVisible = true
        }
        binding.editTextSearch.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Handle touch events on EditText
                handleEditTextTouch(v, event)
            }
            false
        }
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not used in this case
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not used in this case
            }

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val searchText = s.toString().trim().toLowerCase(Locale.getDefault())
                    filterFileItems(searchText)
                    updateEmptyStateVisibility()
                }
            }
        })
    }


    private fun showSettingDialog() {
        // Inflate the dialog view from the XML layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_setting_downloads, null)
        val dialogBinding = ActivitySettingDownloadsBinding.bind(dialogView)

        // Access shared preferences to retrieve stored settings
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        // Read the stored value for sortByNewestFirst, defaulting to true if not found
        sortByNewestFirst = sharedPreferences.getBoolean("sortByNewestFirst", true)

        // Set the switch states based on the current sorting order
        dialogBinding.switchNewFiles.isChecked = sortByNewestFirst
        dialogBinding.switchOldFiles.isChecked = !sortByNewestFirst

        // Define the switch listener to handle changes in switch states
        val switchListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            when (buttonView.id) {
                R.id.switchNewFiles -> {
                    if (isChecked) {
                        dialogBinding.switchOldFiles.isChecked = false
                        sortByNewestFirst = true
                    } else {
                        dialogBinding.switchOldFiles.isChecked = true
                        sortByNewestFirst = false
                    }
                }
                R.id.switchOldFiles -> {
                    if (isChecked) {
                        dialogBinding.switchNewFiles.isChecked = false
                        sortByNewestFirst = false
                    } else {
                        dialogBinding.switchNewFiles.isChecked = true
                        sortByNewestFirst = true
                    }
                }
            }
        }

        // Attach the switch listener to both switches
        dialogBinding.switchNewFiles.setOnCheckedChangeListener(switchListener)
        dialogBinding.switchOldFiles.setOnCheckedChangeListener(switchListener)

        // Build and display the dialog
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Order of Downloaded Files")
            .setPositiveButton("Apply") { dialog, _ ->
                // Save the selected sorting order when the user clicks "Apply"
                sharedPreferences.edit().putBoolean("sortByNewestFirst", sortByNewestFirst).apply()
                sortFilesByTimestamp() // Apply the sorting and update the RecyclerView
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog without saving
            }
            .create()

        dialogBuilder.show()
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun sortFilesByTimestamp() {
        if (sortByNewestFirst) {
            fileItems.sortByDescending { it.lastModifiedTimestamp }
        } else {
            fileItems.sortBy { it.lastModifiedTimestamp }
        }
        fileListAdapter.notifyDataSetChanged()
    }

    private fun filterFileItems(searchText: String) {
        val filteredList = if (searchText.isEmpty()) {
            // If search text is empty, show all items
            fileItems.toList()
        } else {
            // Filter fileItems based on file name containing searchText
            fileItems.filter { fileItem ->
                fileItem.fileName.toLowerCase(Locale.getDefault()).contains(searchText)
            }
        }
        fileListAdapter.filterList(filteredList)
    }
    private fun handleEditTextTouch(v: View, event: MotionEvent) {
        val bounds: Rect = binding.editTextSearch.compoundDrawablesRelative[2].bounds
        val drawableStart = binding.editTextSearch.compoundDrawablesRelative[0] // Index 0 for drawable start
        val bound = drawableStart?.bounds

        if (bound != null) {
            val touchArea = Rect(
                v.paddingLeft,
                v.paddingTop,
                v.paddingLeft + bounds.width(),
                v.height - v.paddingBottom
            )

            if (touchArea.contains(event.x.toInt(), event.y.toInt())) {
                // Hide editTextSearch
                hideEditText()
                filterFileItems("") // Passing empty string to show all files
                updateEmptyStateVisibility()
                return
            }
        }

        val touchArea = Rect(
            v.width - bounds.width() - v.paddingEnd,
            v.paddingTop,
            v.width - v.paddingEnd,
            v.height - v.paddingBottom
        )

        if (touchArea.contains(event.x.toInt(), event.y.toInt())) {
            // Clear the text in the EditText
            binding.editTextSearch.text?.clear()
            filterFileItems("") // Passing empty string to show all files
            updateEmptyStateVisibility()
        }
    }


    private fun hideEditText() {
        binding.editTextSearch.visibility = View.GONE
        binding.imageButtonSearch.visibility = View.VISIBLE
        binding.settingBrowser.visibility = View.VISIBLE
        binding.clearActivity.visibility = View.VISIBLE

        // Set initial positions for the views outside the screen
        binding.constraintLayout.translationY = -binding.constraintLayout.height.toFloat()
        binding.horizontalLine.translationY = -binding.horizontalLine.height.toFloat()

        // Create ObjectAnimators to slide in from the top
        val constraintLayoutAnimator = ObjectAnimator.ofFloat(binding.constraintLayout, "translationY", 0f)
        val horizontalLineAnimator = ObjectAnimator.ofFloat(binding.horizontalLine, "translationY", 0f)

        // Create an AnimatorSet to play the animations together
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(constraintLayoutAnimator, horizontalLineAnimator)
        animatorSet.duration = 200 // Animation duration in milliseconds
        animatorSet.start()

        // Show the views
        binding.constraintLayout.visibility = View.VISIBLE
        binding.horizontalLine.visibility = View.VISIBLE

        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editTextSearch.windowToken, 0)
        isEditTextVisible = false
        updateEmptyStateVisibility()

    }


    override fun onItemClick(fileItem: FileItem) {
        // Check if the file name contains " - Google Search" and has no extension
        val fileName = fileItem.fileName
        val hasNoExtension = !fileName.contains(".")
        val isGoogleSearch = fileName.contains(" - Google Search")

        if (hasNoExtension && isGoogleSearch) {
            // Show a toast message that the file can't be opened
            Toast.makeText(this, "File can't be open", Toast.LENGTH_SHORT).show()
            return
        }
        when (fileItem.fileType) {
            FileType.VIDEO -> {
                val videoUri = Uri.fromFile(File(fileItem.filePath)).toString()
                openPlayerActivity(videoUri, fileItem.fileName)
            }
            FileType.AUDIO -> {
                openFile(fileItem)
            }
            FileType.IMAGE -> {
                openImageFile(fileItem)
            }
            FileType.MHTML-> {  // Modified case for .mhtml and unknown files
                openMhtmlFile(fileItem)
            }
            FileType.UNKNOWN -> {
                openFile(fileItem)
            }
            FileType.BIN -> {  // Add this case for .bin files
                openBinFile(fileItem)
            }
            else -> {
                openFile(fileItem)
            }
        }
    }

    private fun openBinFile(fileItem: FileItem) {
        try {
            val file = File(fileItem.filePath)
            val fileUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )
            val intent = Intent().apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                action = Intent.ACTION_VIEW
                setDataAndType(fileUri, getMimeType(fileItem.filePath))
            }

            // Check if there's any app that can handle this intent
            val activities = packageManager.queryIntentActivities(intent, 0)
            val isIntentSafe = activities.isNotEmpty()

            if (isIntentSafe) {
                startActivity(intent)
            } else {
                // Handle case where no app can handle the file type
                // You can implement your custom handling here, such as showing a toast message
                Toast.makeText(this, "No app found to open .bin file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // Log the error
            Log.e("FileOpening", "Error opening .bin file: ${e.message}")
            // Show a toast or handle the error as appropriate
            Toast.makeText(this, "Error opening .bin file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMhtmlFile(fileItem: FileItem) {
        try {
            val intent = Intent(this, OfflineMhtmlActivity::class.java).apply {
                putExtra("filePath", fileItem.filePath)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Log the error
            Log.e("FileOpening", "Error opening .mhtml file: ${e.message}")
            // Show a toast or handle the error as appropriate
        }
    }
    private fun openImageFile(fileItem: FileItem) {
        try {
            val intent = Intent(this, ImageViewerActivity::class.java).apply {
                putExtra("imagePath", fileItem.filePath)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Log the error
            Log.e("ImageLoading", "Error opening image file: ${e.message}")
            // Show a toast or handle the error as appropriate
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isEditTextVisible) {
            handleBoxClick(binding.monthlyBox)
            hideEditText()
            filterFileItems("")
          updateEmptyStateVisibility()
        } else {
            // Check if the currently selected box is not binding.monthlyBox
            if (selectedBox != binding.monthlyBox) {
                handleBoxClick(binding.monthlyBox)
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun openFile(fileItem: FileItem) {
        try {
            when (fileItem.fileType) {
                FileType.VIDEO -> {
                    val videoUri = Uri.fromFile(File(fileItem.filePath)).toString()
                    openPlayerActivity(videoUri, fileItem.fileName)
                }
                FileType.IMAGE -> {
                    openImageFile(fileItem)
                }

                else -> {
                    val file = File(fileItem.filePath)
                    val fileUri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.provider",
                        file
                    )
                    val intent = Intent().apply {
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        action = Intent.ACTION_VIEW
                        setDataAndType(fileUri, getMimeType(fileItem.filePath))
                    }

                    // Check if there's any app that can handle this intent
                    val activities = packageManager.queryIntentActivities(intent, 0)
                    val isIntentSafe = activities.isNotEmpty()

                    if (isIntentSafe) {
                        startActivity(intent)
                    } else {
                        // Handle case where no app can handle the file type
                        // You can implement your custom handling here, such as showing a toast message
                    }
                }
            }
        } catch (e: Exception) {
            // Log the error
            Log.e("FileOpening", "Error opening file: ${e.message}")
            // Show a toast or handle the error as appropriate
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getMimeType(filePath: String): String? {
        val extension = filePath.substringAfterLast('.', "")
        return when (extension.toLowerCase()) {
            "pdf", "pdfa", "pdfxml", "fdf", "xfdf", "pdfx", "pdp", "ppt", "pptx" -> "application/pdf"
            "jpg", "jpeg", "png", "gif", "svg", "webp" -> "image/*"
            "mp4", "3gp", "mkv", "webm" -> "video/*"
            "mp3", "wav", "ogg" -> "audio/*"
            "apk", "zip" -> "application/vnd.android.package-archive"
            "html", "htm" -> "text/html"
            "mhtml", "mht" -> "multipart/related"  // MIME type for .mhtml files
            "bin" -> "application/octet-stream"  // Add this line for .bin files
            else -> null
        }
    }


    private fun toggleSelection(box: View) {
        if (box != selectedBox) {
            // Deselect any previously selected box
            selectedBox?.let { clearSelection(it) }

            // Select the clicked box and change its background
            selectedBox = box
            box.setBackgroundResource(R.drawable.selected_background_tint_browser)
            // Filter and update RecyclerView based on the selected box
            when (box) {
                binding.quarterlyBox -> {
                    // Filter and show only video files
                    val videoFiles = fileItems.filter { it.fileType == FileType.VIDEO || it.fileType == FileType.AUDIO  }
                    fileListAdapter.filterList(videoFiles)
                    updateEmptyStateVisibility()
                }
                binding.annualBox -> {
                    // Filter and show only image files
                    val imageFiles = fileItems.filter { it.fileType == FileType.IMAGE }
                    fileListAdapter.filterList(imageFiles)
                    updateEmptyStateVisibility()
                }
                binding.biennialBox -> {
                    // Filter and show only APK files
                    val apkFiles = fileItems.filter { it.fileType == FileType.APK || it.fileType == FileType.WEBSITE || it.fileType == FileType.PDF  || it.fileType == FileType.UNKNOWN}
                    fileListAdapter.filterList(apkFiles)
                    updateEmptyStateVisibility()
                }
                binding.pageBox -> {
                    // Filter and show only APK files
                    val pageFiles = fileItems.filter { it.fileType == FileType.MHTML}
                    fileListAdapter.filterList(pageFiles)
                    updateEmptyStateVisibility()
                }
                binding.monthlyBox -> {
                    fileListAdapter.filterList(fileItems)
                    updateEmptyStateVisibility()
                }
                else -> {
                    // Show all files when no specific box is selected (monthlyBox)
                    fileListAdapter.filterList(fileItems)
                    updateEmptyStateVisibility()
                }
            }
        }
        }

        // If the clicked box is already selected, do nothing (maintain selection)



    private fun clearSelection(box: View) {
        box.setBackgroundResource(R.drawable.square_box_bg_browser) // Revert to default background
        selectedBox = null

        // Show all files again when deselecting a box
        fileListAdapter.filterList(fileItems)
    }

    private fun selectBox(box: View) {
        selectedBox = box
        box.setBackgroundResource(R.drawable.selected_background_tint_browser)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun retrieveDownloadedFiles(): List<FileItem> {
        val downloadedFiles = ArrayList<FileItem>()
        val sharedPreferences = getSharedPreferences("FileMetadata", Context.MODE_PRIVATE)

        // Get the path to the Downloads directory
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir != null && downloadsDir.exists() && downloadsDir.isDirectory) {
            // List all files in the Downloads directory
            val files = downloadsDir.listFiles()
            if (files != null) {
                // Sort files by last modified time (newest first)
                files.sortByDescending { it.lastModified() }
                for (file in files) {
                    if (file.isFile) {
                        // Create a FileItem object for each downloaded file
                        val fileName = file.name
                        val fileSize = file.length()
                        val fileExtension = getFileExtension(fileName)
                        val fileType = getFileType(fileExtension)
                        val artUri = file.toUri()
                        val lastModifiedTimestamp =file.lastModified() // Get last modified timestamp
                        val fileNewName = file.name
                        val websiteName = sharedPreferences.getString(file.absolutePath, "unknown")



                        // Use appropriate constructor parameters for FileItem
                        val fileItem = FileItem(fileName, file.absolutePath, fileSize ,
                            fileType, artUri , lastModifiedTimestamp = lastModifiedTimestamp , fileNewName , websiteName)
                        downloadedFiles.add(fileItem)
                    }
                }
            }
        }
        sortFilesByTimestamp()

// Filter image files and check if the list is empty
        val imageFiles = downloadedFiles.filter { it.fileType == FileType.IMAGE }
        if (imageFiles.isEmpty()) {
            fileListAdapter.notifyDataSetChanged()
            binding.annualBox.visibility = View.GONE
        }
        else {
            fileListAdapter.notifyDataSetChanged()
            binding.annualBox.visibility = View.VISIBLE
        }

        // Filter image files and check if the list is empty
        val webViewFiles = downloadedFiles.filter { it.fileType == FileType.MHTML }
        if (webViewFiles.isEmpty()) {

            fileListAdapter.notifyDataSetChanged()
            binding.pageBox.visibility = View.GONE
        }
        else {

            fileListAdapter.notifyDataSetChanged()
            binding.pageBox.visibility = View.VISIBLE
        }

        // Filter image files and check if the list is empty
        val otherFiles = downloadedFiles.filter { it.fileType == FileType.APK || it.fileType == FileType.WEBSITE || it.fileType == FileType.PDF  || it.fileType == FileType.UNKNOWN }
        if (otherFiles.isEmpty()) {

            // Notify the adapter of the change
            fileListAdapter.notifyDataSetChanged()
            binding.biennialBox.visibility = View.GONE
        }
        else {

            fileListAdapter.notifyDataSetChanged()
            binding.biennialBox.visibility = View.VISIBLE
        }

        // Filter image files and check if the list is empty
        val videoFiles = downloadedFiles.filter { it.fileType == FileType.AUDIO || it.fileType == FileType.VIDEO }
        if (videoFiles.isEmpty()) {

            fileListAdapter.notifyDataSetChanged()
            binding.quarterlyBox.visibility = View.GONE
        }
        else {

            fileListAdapter.notifyDataSetChanged()
            binding.quarterlyBox.visibility = View.VISIBLE
        }

        return downloadedFiles
    }

    private fun getFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex != -1 && lastDotIndex < fileName.length - 1) {
            fileName.substring(lastDotIndex + 1).toLowerCase()
        } else {
            ""
        }
    }
    private fun getFileType(fileExtension: String): FileType {
        return when (fileExtension) {
            "pdf", "pdfa", "pdfxml", "fdf", "xfdf", "pdfx", "pdp", "ppt", "pptx" -> FileType.PDF
            "jpg", "jpeg", "png", "gif", "svg", "webp" -> FileType.IMAGE
            "mp4", "3gp", "mkv", "webm" -> FileType.VIDEO
            "mp3", "wav", "ogg" -> FileType.AUDIO
            "apk", "zip" -> FileType.APK
            "html", "htm" -> FileType.WEBSITE
            "mhtml", "mht" -> FileType.MHTML  // Add this line
            "bin" -> FileType.BIN
            else -> FileType.UNKNOWN
        }
    }

    private fun fetchFileDetailsFromWebsite(url: String): Triple<String, String, String>? {
        try {
            // Fetch the webpage content using Jsoup
            val doc: Document = Jsoup.connect(url).get()

            // Extract the file URL and filename from the webpage content based on file type
            val fileUrl: String
            val fileName: String

            // Fetch the website name
            val websiteName: String = doc.title()

            // Check if the webpage contains a PDF file link
            if (doc.select("a[href$=.pdf], a[href$=.pdfa], a[href$=.pdfxml], a[href$=.fdf], a[href$=.xfdf], a[href$=.pdfx], a[href$=.pdp] , a[href$=.PPT], a[href$=.pptx]").isNotEmpty()) {
                fileUrl = doc.select("a[href$=.pdf], a[href$=.pdfa], a[href$=.pdfxml], a[href$=.fdf], a[href$=.xfdf], a[href$=.pdfx], a[href$=.pdp], a[href$=.PPT], a[href$=.pptx] ").attr("href")
                fileName = fileUrl.substringAfterLast('/')
                return Triple(fileUrl, fileName, websiteName)
            }

            // Check if the webpage contains a video file link (e.g., MP4)
            if (doc.select("a[href$=.mp4]").isNotEmpty()) {
                fileUrl = doc.select("a[href$=.mp4]").attr("href")
                fileName = fileUrl.substringAfterLast('/')
                return Triple(fileUrl, fileName, websiteName)
            }

            // Check if the webpage contains a video file link (e.g., MP4)
            if (doc.select("a[href$=.mhtml], a[href$=.mht]").isNotEmpty()) {
                fileUrl = doc.select("a[href$=.mhtml], a[href$=.mht]").attr("href")
                fileName = fileUrl.substringAfterLast('/')
                return Triple(fileUrl, fileName, websiteName)
            }

            // Check if the webpage contains an image file link (e.g., JPG, PNG)
            if (doc.select("a[href$=.jpg], a[href$=.jpeg], a[href$=.png] , a[href$=.svg] , a[href$=.webp]" ).isNotEmpty()) {
                fileUrl = doc.select("a[href$=.jpg], a[href$=.jpeg], a[href$=.png]  , a[href$=.svg]  a[href$=.webp] , " ).attr("href")
                fileName = fileUrl.substringAfterLast('/')
                return Triple(fileUrl, fileName, websiteName)
            }
            if (doc.select("a[href$=.mp3], a[href$=.wav], a[href$=.ogg]").isNotEmpty()) {
                fileUrl = doc.select("a[href$=.mp3], a[href$=.wav], a[href$=.ogg]").attr("href")
                fileName = fileUrl.substringAfterLast('/')
                return Triple(fileUrl, fileName, websiteName)
            }

            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
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
                        this@FileActivity,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else {
            // Dark mode is applied or the mode is set to follow system
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@FileActivity,
                        R.drawable.background_actionbar
                    )
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setActionBarGradient()
        updateEmptyStateVisibility()
        sortFilesByTimestamp()

        MobileAds.initialize(this){}
        mAdView = findViewById(R.id.adView)
        // banner ads
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    @SuppressLint("SetTextI18n")
    override fun onFileCountChanged(newCount: Int) {

            binding.totalFile.text = "Total Downloaded files: $newCount"

    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onFileDeleted(fileItem: FileItem) {
        fileItems.remove(fileItem) // Remove the file from the main list

        // Notify the adapter that the data set has changed
        fileListAdapter.notifyDataSetChanged()

        // Update the total file count text
        binding.totalFile.text = "Total Downloaded files: ${fileItems.size}"

    }
 fun updateEmptyStateVisibility() {
        if (fileListAdapter.itemCount == 0) {
            binding.fileEmptyStateLayout.visibility = View.VISIBLE

            // Set button colors to gray
            binding.imageButtonSearch.setColorFilter(Color.GRAY)
            binding.settingBrowser.setColorFilter(Color.GRAY)

            // Disable click listeners
            binding.imageButtonSearch.isClickable = false
            binding.settingBrowser.isClickable = false
        } else {
            binding.fileEmptyStateLayout.visibility = View.GONE

            // Reset button colors
            binding.imageButtonSearch.setColorFilter(null)
            binding.settingBrowser.setColorFilter(null)

            // Enable click listeners
            binding.imageButtonSearch.isClickable = true
            binding.settingBrowser.isClickable = true
        }
    }

    override fun onSelectionModeChanged(isSelectionModeEnabled: Boolean) {
        if (isSelectionModeEnabled) {
            binding.linearLayout7.visibility = View.GONE
            binding.adsLayout.visibility = View.GONE
        } else {
            binding.linearLayout7.visibility = View.VISIBLE
            binding.adsLayout.visibility = View.VISIBLE
        }
    }


}

