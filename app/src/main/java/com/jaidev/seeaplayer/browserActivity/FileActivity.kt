package com.jaidev.seeaplayer.browserActivity

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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

class FileActivity : AppCompatActivity() , FileAdapter.OnItemClickListener  {
    private lateinit var fileListAdapter: FileAdapter
    private val fileItems: MutableList<FileItem> = mutableListOf()
    private lateinit var binding: ActivityFileBinding
    private var selectedBox: View? = null // Track currently selected box
    private var isEditTextVisible = false
    private var sortByNewestFirst = true // Default to sort by newest first
    lateinit var mAdView: AdView
    companion object {

        private val DOWNLOADS_DIRECTORY = Environment.DIRECTORY_DOWNLOADS

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
        binding.monthlyBox.setOnClickListener { toggleSelection(binding.monthlyBox) }
        binding.quarterlyBox.setOnClickListener { toggleSelection(binding.quarterlyBox) }
        binding.annualBox.setOnClickListener { toggleSelection(binding.annualBox) }
        binding.biennialBox.setOnClickListener { toggleSelection(binding.biennialBox) }

        // Set up RecyclerView and adapter
        fileListAdapter = FileAdapter(this, fileItems, this)

        binding.settingBrowser.setOnClickListener {
            showSettingDialog()

        }

        binding.recyclerFileView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@FileActivity)
            adapter = fileListAdapter
        }


        binding.imageButtonSearch.setOnClickListener {
            // Show editTextSearch
            binding.editTextSearch.visibility = View.VISIBLE
            binding.imageButtonSearch.visibility = View.GONE
            binding.settingBrowser.visibility = View.GONE
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
                }
            }
        })
        // Call method to retrieve downloaded files
        val fileList = retrieveDownloadedFiles()
        fileItems.addAll(fileList)
        fileListAdapter.notifyDataSetChanged()

        // Additional setup and initialization...


    thread {
            val websiteUrl = "https://www.google.com" // Replace with the target website URL

            val fileDetails = fetchFileDetailsFromWebsite(websiteUrl)

            // Check if file details are retrieved successfully
            if (fileDetails != null) {
                val (fileUrl, fileName) = fileDetails

                // Start the file download using the obtained file URL and filename
                FileDownloader.downloadFile(this@FileActivity, fileUrl, fileName)
            } else {
                // Handle error or display a message if file details retrieval fails
            }
        }

        binding.totalFile.text = "Total Downloaded files : ${fileList.size}"

        selectBox(binding.monthlyBox)
    }





    private fun showSettingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_setting_downloads, null)
        val dialogBinding = ActivitySettingDownloadsBinding.bind(dialogView)

        // Initialize switches based on stored settings
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        // Read the stored value for sortByNewestFirst (defaulting to true if not found)
        sortByNewestFirst = sharedPreferences.getBoolean("sortByNewestFirst", true)
        dialogBinding.switchNewFiles.isChecked = !sortByNewestFirst
        dialogBinding.switchOldFiles.isChecked = sortByNewestFirst

        val switchListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            when (buttonView.id) {
                R.id.switchNewFiles -> {
                    // Update the sorting order and switch states
                    sortByNewestFirst = !isChecked
                    dialogBinding.switchOldFiles.isChecked = sortByNewestFirst
                }
                R.id.switchOldFiles -> {
                    // Update the sorting order and switch states
                    sortByNewestFirst = isChecked
                    dialogBinding.switchNewFiles.isChecked = !isChecked
                }
            }
        }


        dialogBinding.switchNewFiles.setOnCheckedChangeListener(switchListener)
        dialogBinding.switchOldFiles.setOnCheckedChangeListener(switchListener)


        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Order of Downloaded files")
            .setPositiveButton("Apply") { dialog, _ ->
                sortByNewestFirst = dialogBinding.switchOldFiles.isChecked
                sortFilesByTimestamp()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
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

        }
    }


    private fun hideEditText() {
        binding.editTextSearch.visibility = View.GONE
        binding.imageButtonSearch.visibility = View.VISIBLE
        binding.settingBrowser.visibility = View.VISIBLE
        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editTextSearch.windowToken, 0)
        isEditTextVisible = false


    }


    override fun onItemClick(fileItem: FileItem) {
        if (fileItem.fileType == FileType.IMAGE) {
            openImageFile(fileItem)
        } else {
            openFile(fileItem)

        }
    }
    private fun openImageFile(fileItem: FileItem) {
        val intent = Intent(this, ImageViewerActivity::class.java).apply {
            putExtra("imagePath", fileItem.filePath)
        }
        startActivity(intent)
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun openFile(fileItem: FileItem) {
        val file = File(fileItem.filePath)
        val fileUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, getMimeType(fileItem.filePath))
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            // Add FLAG_ACTIVITY_NEW_TASK for starting activity outside of an existing task
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Handle opening URLs in a web browser
            if (getMimeType(fileItem.filePath) == "text/html") {
                data = fileUri // Set data to the URL
            } else {
                // Grant permission to install packages if the APK is from a content:// URI
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, packageName)
                }
            }
        }

        // Check if there's any app that can handle this intent
        val activities = packageManager.queryIntentActivities(intent, 0)
        val isIntentSafe = activities.isNotEmpty()

        if (isIntentSafe) {
            startActivity(intent)
        } else {
            // Handle case where no app can handle the file type
        }

        val mimeType = "image/jpeg"  // Example MIME type
        val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)

        // Start download based on MIME type
        if (mimeType.startsWith("image/")) {
            startDownload(this, "https://example.com/image.jpg", "image", fileExtension, mimeType)

        } else if (mimeType.startsWith("video/")) {
            startDownload(this, "https://example.com/video.mp4", "video", fileExtension, mimeType)
        }
    }


    private fun startDownload(context: Context, url: String, fileName: String, fileExtension: String?, mimeType: String?) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$fileName.$fileExtension")
            .setMimeType(mimeType ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension))

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }


    private fun getMimeType(filePath: String): String? {
        val extension = filePath.substringAfterLast('.', "")
        return when (extension.toLowerCase()) {
            "pdf" -> "application/pdf"
            "apk", "zip" -> "application/vnd.android.package-archive" // APK and ZIP both have the same MIME type
            "mp4", "3gp", "mkv", "webm" -> "video/*"
            "mp3", "wav", "ogg" -> "audio/*"
            "jpg", "jpeg", "png", "gif" , "svg"-> "image/*"
            else -> {
                // Check if it's a URL file
                if (filePath.startsWith("http://google.com") || filePath.startsWith("https://google.com")) {
                    "text/html"
                } else {
                    null
                }
            }
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
                }
                binding.annualBox -> {
                    // Filter and show only image files
                    val imageFiles = fileItems.filter { it.fileType == FileType.IMAGE }
                    fileListAdapter.filterList(imageFiles)
                }
                binding.biennialBox -> {
                    // Filter and show only APK files
                    val apkFiles = fileItems.filter { it.fileType == FileType.APK || it.fileType == FileType.WEBSITE || it.fileType == FileType.PDF }
                    fileListAdapter.filterList(apkFiles)
                }
                binding.monthlyBox -> {
                    fileListAdapter.filterList(fileItems)
                }

                else -> {
                    // Show all files when no specific box is selected (monthlyBox)
                    fileListAdapter.filterList(fileItems)
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

    private fun retrieveDownloadedFiles(): List<FileItem> {
        val downloadedFiles = ArrayList<FileItem>()
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
                        val websiteName = "example.com" // Name of the website where the video was downloaded from



                        // Use appropriate constructor parameters for FileItem
                        val fileItem = FileItem(fileName, file.absolutePath, fileSize ,
                            fileType, artUri , lastModifiedTimestamp = lastModifiedTimestamp , fileNewName , websiteName)
                        downloadedFiles.add(fileItem)
                    }
                }
            }
        }
// Sort files based on the default sorting order
        sortFilesByTimestamp()

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
            "pdf" ,"pdfa", "pdfxml", "fdf", "xfdf", "pdfx", "pdp"-> FileType.PDF
            "jpg", "jpeg", "png", "gif" , "svg"-> FileType.IMAGE
            "mp4", "3gp", "mkv", "webm" -> FileType.VIDEO
            "mp3", "wav", "ogg"  -> FileType.AUDIO
            "apk","zip" -> FileType.APK // Handle APK file type
            "html", "htm" -> FileType.WEBSITE // Handle WEBSITE file type (HTML)
            // Add more file types as needed
            else -> FileType.UNKNOWN
        }
    }

    private fun fetchFileDetailsFromWebsite(url: String): Pair<String, String>? {
        try {
            // Fetch the webpage content using Jsoup
            val doc: Document = Jsoup.connect(url).get()

            // Extract the file URL and filename from the webpage content based on file type
            val fileUrl: String
            val fileName: String

            // Check if the webpage contains a PDF file link
            if (doc.select("a[href$=.pdf], a[href$=.pdfa], a[href$=.pdfxml], a[href$=.fdf], " +
                        "a[href$=.xfdf], a[href$=.pdfx], a[href$=.pdp]").isNotEmpty()) {
                fileUrl = doc.select("a[href$=.pdf], a[href$=.pdfa], a[href$=.pdfxml], a[href$=.fdf], " +
                        "a[href$=.xfdf], a[href$=.pdfx], a[href$=.pdp]").attr("href")
                fileName = fileUrl.substringAfterLast('/')
                return Pair(fileUrl, fileName)
            }

            // Check if the webpage contains a video file link (e.g., MP4)
            if (doc.select("a[href$=.mp4]").isNotEmpty()) {
                fileUrl = doc.select("a[href$=.mp4]").attr("href")
                fileName = fileUrl.substringAfterLast('/')
                return Pair(fileUrl, fileName)
            }

            // Check if the webpage contains an image file link (e.g., JPG, PNG)
            if (doc.select("a[href$=.jpg], a[href$=.jpeg], a[href$=.png] , a[href$=.svg]").isNotEmpty()) {
                fileUrl = doc.select("a[href$=.jpg], a[href$=.jpeg], a[href$=.png]  , a[href$=.svg]" ).attr("href")
                fileName = fileUrl.substringAfterLast('/')
                return Pair(fileUrl, fileName)
            }
            if (doc.select("a[href$=.mp3], a[href$=.wav], a[href$=.ogg]").isNotEmpty()) {
                fileUrl = doc.select("a[href$=.mp3], a[href$=.wav], a[href$=.ogg]").attr("href")
                fileName = fileUrl.substringAfterLast('/')
                return Pair(fileUrl, fileName)
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

        sortFilesByTimestamp()
    }

}

