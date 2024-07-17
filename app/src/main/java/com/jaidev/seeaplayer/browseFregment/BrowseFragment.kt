

package com.jaidev.seeaplayer.browseFregment


import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.ContextMenu
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.internal.ViewUtils
import com.google.android.material.snackbar.Snackbar
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.HistoryAdapter
import com.jaidev.seeaplayer.allAdapters.SavedTitlesAdapter
import com.jaidev.seeaplayer.allAdapters.SearchItemAdapter
import com.jaidev.seeaplayer.allAdapters.TabAdapter
import com.jaidev.seeaplayer.allAdapters.TabQuickButtonAdapter
import com.jaidev.seeaplayer.browserActivity.FileActivity
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity.Companion.myPager
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity.Companion.tabsList
import com.jaidev.seeaplayer.browserActivity.changeTab
import com.jaidev.seeaplayer.dataClass.FileType
import com.jaidev.seeaplayer.dataClass.HistoryItem
import com.jaidev.seeaplayer.dataClass.HistoryManager
import com.jaidev.seeaplayer.dataClass.SearchTitle
import com.jaidev.seeaplayer.dataClass.SearchTitleStore
import com.jaidev.seeaplayer.databinding.FragmentBrowseBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder


class BrowseFragment(private var urlNew : String) : Fragment(), DownloadListener, HistoryAdapter.ItemClickListener {
    lateinit var binding: FragmentBrowseBinding

    var webIcon: Bitmap? = null
    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var fileListAdapter: SearchItemAdapter
    private var isLoadingPage = false // Add this variabl
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var loadRunnable: Runnable


    companion object {
        private const val CHANNEL_ID = "FileDownloadChannel"
    }

    interface DownloadListener {
        fun onDownloadStarted(
            fileName: String,
            fileSize: String,
            fileType: FileType,
            fileIconResId: Int
        )


    }


    @SuppressLint("SetJavaScriptEnabled", "ObsoleteSdkInt", "NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_browse, container, false)

        try {
        binding = FragmentBrowseBinding.bind(view)
        registerForContextMenu(binding.webView)

            loadRunnable = Runnable {
                if (isLoadingPage) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Page load timed out", Toast.LENGTH_SHORT).show()
                }
            }

        binding.webView.apply {
            when {
                URLUtil.isValidUrl(urlNew) -> loadUrl(urlNew)
                urlNew.contains(".com", ignoreCase = true) -> loadUrl(urlNew)
                urlNew.startsWith("https://", ignoreCase = true) -> loadUrl(urlNew)
                urlNew.startsWith("http://", ignoreCase = true) -> loadUrl(urlNew)
                urlNew.startsWith("intent://", ignoreCase = true) -> loadUrl(urlNew)
                else -> loadUrl("https://www.google.com/search?q=$urlNew")
            }

            settings.javaScriptEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false

            CookieManager.getInstance().setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            }


        }

        val rootView = view.rootView

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom
            val isKeyboardVisible = keypadHeight > screenHeight * 0.15
            if (!isKeyboardVisible) {
                binding.recyclerviewLayout.visibility = View.GONE

            }
        }

        setupNoInternetView()
        checkAndDisableSwipeRefreshBasedOnUrl() // Check URL here
            TabQuickButtonAdapter.updateTabs()

            return view

        } catch (e: Exception) {
            showToast(requireContext(), "Something went wrong, try to refresh")
            e.printStackTrace()
            return view
        }
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }


    private fun decodeUrl(url: String): String {
        return try {
            URLDecoder.decode(url, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            url // Return the original URL if decoding fails
        }
    }
    private fun setupNoInternetView() {
        binding.customErrorLayout.findViewById<TextView>(R.id.try_again_button).setOnClickListener {
            retryLoadingPage()
        }

        // Handle turn on mobile data button
        binding.customErrorLayout.findViewById<TextView>(R.id.turn_on_mobile_data_button).setOnClickListener {
            val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Unable to open mobile data settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNoInternetView() {
        binding.webView.visibility = View.GONE
        if (binding.customErrorLayout.parent == null) {
            (binding.root as ViewGroup).addView(binding.customErrorLayout)
        }

        binding.customErrorLayout.visibility = View.VISIBLE
    }

    private fun hideNoInternetView() {
        binding.webView.visibility = View.VISIBLE
        binding.customErrorLayout.visibility = View.GONE

    }
    // Check if internet is back on Try Again click
    private fun retryLoadingPage() {
        if (checkForInternet(requireContext())) {
            binding.webView.reload()
        } else {
            showNoInternetView()
        }
    }


    private fun checkForInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }


    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility", "ObsoleteSdkInt",
        "RestrictedApi"
    )
    override fun onResume() {
        super.onResume()
        try {
        registerDownloadReceiver()
        tabsList[myPager.currentItem].name =
            binding.webView.url.toString()
        LinkTubeActivity.tabsBtn.text = tabsList.size.toString()
        LinkTubeActivity.tabs2Btn.text = tabsList.size.toString()
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.domStorageEnabled = true // Enable DOM storage
            settings.mediaPlaybackRequiresUserGesture = false // Auto-play media
            // Enable cookies if needed
            CookieManager.getInstance().setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            }
        }
        binding.webView.setDownloadListener { url, _, _, mimeType, _ ->
            // Determine the file extension based on MIME type
            val fileExtension = when {
                mimeType.equals("image/svg+xml", ignoreCase = true) -> "svg"
                else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            }

            // Extract the filename from the URL
            val fileName = URLUtil.guessFileName(url, null, fileExtension)

            // Check the file type and initiate appropriate download action
            if (!mimeType.isNullOrBlank() && !fileExtension.isNullOrBlank()) {
                when {
                    mimeType.equals("application/pdf", ignoreCase = true) -> {
                        startDownload(url, "document.pdf", fileExtension)
                    }
                    mimeType.startsWith("video/") -> {
                        // Assuming the filename is 'video.mp4' for demonstration
                        startDownload(url, "video.mp4", fileExtension)
                    }
                    mimeType.startsWith("image/") -> {
                        // Use the correct file extension for images
                        startDownload(url, fileName, fileExtension)
                    }
                    else -> {
                        // Handle other file types here if needed
                        startDownload(url, fileName, fileExtension)
                    }
                }
            } else {
                // Handle invalid MIME type or file extension
                Log.e("DownloadListener", "Invalid MIME type or file extension")
            }
        }

        val linkRef = requireActivity() as LinkTubeActivity

        var frag: BrowseFragment? = null
        try {
            frag = tabsList[linkRef.binding.myPager.currentItem].fragment as BrowseFragment
        }catch (_:Exception){}

        linkRef.binding.crossBtn.setOnClickListener {
            linkRef.binding.btnTextUrl.text.clear()

        }


        linkRef.binding.btnTextUrl.setOnClickListener {
            if (binding.recyclerviewLayout.visibility == View.VISIBLE) {
                binding.recyclerviewLayout.visibility = View.GONE
            } else {

                fillTitleInTextUrl()
                linkRef.binding.btnTextUrl.text.clear()
                binding.recyclerviewLayout.visibility = View.VISIBLE
            }
        }

// Before loading the webpage, check for network connectivity
        if (checkForInternet(requireContext())) {
            // Proceed with loading the webpage
        } else {
            // Inform the user about the lack of internet connectivity
            Toast.makeText(requireContext(), "No Internet Connection", Toast.LENGTH_SHORT).show()
        }

        // Implement a function to check for internet connectivity
        fun checkForInternet(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }

        binding.historyRecycler.setHasFixedSize(true)
        binding.historyRecycler.setItemViewCacheSize(5)
        binding.historyRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecycler.adapter = SavedTitlesAdapter(requireContext())


// Set editor action listener for btnTextUrl (IME_ACTION_DONE or IME_ACTION_GO)
        linkRef.binding.btnTextUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                val query = linkRef.binding.btnTextUrl.text.toString()
                if (checkForInternet(requireContext())) {
                    // Hide keyboard and RecyclerView
                    ViewUtils.hideKeyboard(linkRef.binding.btnTextUrl)
                    binding.recyclerviewLayout.visibility = View.GONE
                    // Perform search
                    performSearch(query)
                } else {
                    Toast.makeText(requireContext(), "No Internet Connection \uD83C\uDF10", Toast.LENGTH_SHORT).show()
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        // Check if it's a tablet
        val isTablet = resources.configuration.smallestScreenWidthDp >= 600


        // Show or hide the buttons based on the device type
        if (isTablet) {
            linkRef.binding.backBrowserBtn.visibility = View.VISIBLE
            linkRef.binding.forwardBrowserBtn.visibility = View.VISIBLE
        } else {
            linkRef.binding.backBrowserBtn.visibility = View.GONE
            linkRef.binding.forwardBrowserBtn.visibility = View.GONE
        }

        linkRef.binding.backBrowserBtn.setOnClickListener {
            linkRef.onBackPressed()
        }
        linkRef.binding.bottomBackBrowser.setOnClickListener {
            linkRef.onBackPressed()
        }
        linkRef.binding.bottomForwardBrowser.setOnClickListener {
            frag?.apply {
                if (binding.webView.canGoForward())
                    binding.webView.goForward()

            }
        }
        linkRef.binding.forwardBrowserBtn.setOnClickListener {
            frag?.apply {
                if (binding.webView.canGoForward())
                    binding.webView.goForward()

            }
        }

        linkRef.binding.btnTextUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()

                val adapter = binding.historyRecycler.adapter as? SavedTitlesAdapter

                // Check if the query contains the specific URL format
                val urlPrefix = "https://www.google.com/search?q="
                val filteredQuery = if (query.startsWith(urlPrefix)) {
                    query.removePrefix(urlPrefix)
                } else {
                    query
                }

                // Filter the adapter with the extracted or original query
                adapter?.filter(filteredQuery)

                // Show the RecyclerView layout if there is a filtered query or the original query changes
                if (query.isNotEmpty()) {
                    binding.recyclerviewLayout.visibility = View.VISIBLE
                } else {
                    binding.recyclerviewLayout.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })


        binding.swipeRefreshBrowser.setOnRefreshListener {
            if (checkForInternet(requireContext())) {
                hideNoInternetView() // Hide the error layout if the internet is connected
                if (binding.swipeRefreshBrowser.isEnabled) {
                    binding.webView.reload()
                }
            } else {
                showNoInternetView() // Show the error layout if there's no internet
            }
            binding.swipeRefreshBrowser.isRefreshing = false
        }


        binding.webView.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> binding.swipeRefreshBrowser.isEnabled = false
                MotionEvent.ACTION_UP -> binding.swipeRefreshBrowser.isEnabled = true
            }
            view.onTouchEvent(motionEvent)
            false
        }

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.domStorageEnabled = true // Enable DOM storage
            settings.mediaPlaybackRequiresUserGesture = false // Auto-play media

            CookieManager.getInstance().setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            }
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
//                    super.onReceivedError(view, errorCode, description, failingUrl)
//                    // Hide WebView
//                    binding.webView.visibility = View.GONE // Hide the WebView
//                    // Show custom error layout
////
                // binding.customErrorLayout.visibility = android.view.View.VISIBLE
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    // Hide WebView
//                    binding.webView.visibility = View.GONE // Hide the WebView
                    // Show custom error layout
//                    binding.customErrorLayout.visibility = android.view.View.VISIBLE
                }
                override fun doUpdateVisitedHistory(
                    view: WebView?,
                    url: String?,
                    isReload: Boolean
                ) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    linkRef.binding.btnTextUrl.text =
                        SpannableStringBuilder(url?.let { decodeUrl(it) })
                    tabsList[myPager.currentItem].name = url.toString()
                }


                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    isLoadingPage = false
                    // Update swipe refresh state when the main content of the page is visible
                    updateSwipeRefreshState(url)
                    val websiteTitle = HistoryManager.extractWebsiteTitle(url ?: "")
                    val favicon = view?.favicon
                    val timestamp = System.currentTimeMillis()
                    val historyItem = HistoryItem(url ?: "", websiteTitle, timestamp, favicon)
                    // Add history item to the HistoryManager
                    HistoryManager.addHistoryItem(historyItem, requireContext())
                    // Update searchHintTitleRecyclerView here
                    updateSearchHintTitleRecyclerView()
                    binding.progressBar.visibility = View.GONE

                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    isLoadingPage = true
                    linkRef.binding.progressBar.progress = 0
                    linkRef.binding.progressBar.visibility = View.VISIBLE
                    linkRef.binding.btnTextUrl.text = SpannableStringBuilder(url?.let { decodeUrl(it) })
//                    if (checkForInternet(requireContext())) {
//                        binding.webView.visibility = View.VISIBLE
//                        hideNoInternetView()
//                    } else {
//                        binding.webView.visibility = View.GONE
//                        showNoInternetView()
//                    }
                    // Start the timeout countdown
                    binding.progressBar.visibility = View.VISIBLE

                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isLoadingPage = false
                    linkRef.binding.progressBar.visibility = View.GONE
                    binding.webView.zoomOut()
                    updateSwipeRefreshState(url)
                    updateSearchHintTitleRecyclerView()
                    val websiteTitle = HistoryManager.extractWebsiteTitle(url ?: "")
                    val favicon = view?.favicon
                    val timestamp = System.currentTimeMillis()
                    val historyItem = HistoryItem(url ?: "", websiteTitle, timestamp, favicon)
                    HistoryManager.addHistoryItem(historyItem, requireContext())

//                    if (checkForInternet(requireContext())) {
//                        binding.webView.visibility = View.VISIBLE
//                        hideNoInternetView()
//                    } else {
//                        binding.webView.visibility = View.GONE
//                        showNoInternetView()
//                    }
                    binding.progressBar.visibility = View.GONE

// Capture Bitmap of WebView content
                    binding.webView.isDrawingCacheEnabled = true
                    binding.webView.buildDrawingCache(true)
                    val bitmap = Bitmap.createBitmap(binding.webView.drawingCache)
                    binding.webView.isDrawingCacheEnabled = false

                    // Store the Bitmap in tabsList
                    tabsList[linkRef.binding.myPager.currentItem].previewBitmap = bitmap

                }


            }
            webChromeClient = object : WebChromeClient() {

                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    super.onReceivedIcon(view, icon)
                    try {
                        linkRef.binding.webIcon.setImageBitmap(icon)

                        webIcon = icon
                        // Update the icon for the current tab
                        val currentTab = tabsList[myPager.currentItem]
                        currentTab.icon = icon
                        TabQuickButtonAdapter.updateTabs()
                        // Notify the TabAdapter of the change
                        (myPager.adapter as? TabAdapter)?.notifyItemChanged(myPager.currentItem)

                        val websiteTitle = HistoryManager.extractWebsiteTitle(url ?: "")
                        val favicon = view?.favicon
                        val timestamp = System.currentTimeMillis()
                        val historyItem = HistoryItem(url ?: "", websiteTitle, timestamp, favicon)
                        HistoryManager.addHistoryItem(historyItem, requireContext())

                        LinkTubeActivity.bookmarkIndex = linkRef.isBookmarked(view?.url!!)
                        if (LinkTubeActivity.bookmarkIndex != -1) {
                            val array = ByteArrayOutputStream()
                            icon!!.compress(Bitmap.CompressFormat.PNG, 100, array)
                            LinkTubeActivity.bookmarkList[LinkTubeActivity.bookmarkIndex].image =
                                array.toByteArray()
                        }
                    } catch (_: Exception) {
                    }
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    super.onShowCustomView(view, callback)
                    binding.webView.visibility = View.GONE
                    binding.customView.visibility = View.VISIBLE
                    binding.customView.addView(view)

                }

                override fun onHideCustomView() {
                    super.onHideCustomView()
                    binding.webView.visibility = View.VISIBLE
                    binding.customView.visibility = View.GONE

                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    binding.progressBar.progress = newProgress
                    if (newProgress == 100) {
                        binding.progressBar.visibility = View.GONE
                    } else {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    linkRef.binding.progressBar.progress = newProgress
                    updateSwipeRefreshState(linkRef.binding.btnTextUrl.text.toString())
                }
            }
            binding.webView.setOnTouchListener { _, motionEvent ->
                linkRef.binding.root.onTouchEvent(motionEvent)
                return@setOnTouchListener false
            }

            loadRunnable = Runnable {
                if (isLoadingPage) {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
        } catch (e: Exception) {
            showToast(requireContext(), "Something went wrong, try to refresh")
            e.printStackTrace()
        }
    }


    private fun updateSwipeRefreshState(url: String?) {
        val keywords = listOf("/reels/", "/comments/", "/story.", "/reel/", "/shorts/", "/watch?")
        binding.swipeRefreshBrowser.isEnabled = !keywords.any { url?.contains(it, ignoreCase = true) == true }
    }
    private fun checkAndDisableSwipeRefreshBasedOnUrl() {
        val linkRef = requireActivity() as LinkTubeActivity
        val currentUrl = linkRef.binding.btnTextUrl.text.toString()

        // Call updateSwipeRefreshState with the current URL
        updateSwipeRefreshState(currentUrl)
    }


    private fun performSearch(query: String) {
        val linkRef = requireActivity() as LinkTubeActivity

        if (checkForInternet(requireContext())) {
            // Check if the title is already saved
            val isTitleSaved = SearchTitleStore.getTitles(requireContext()).any { it.title == query }

            if (!isTitleSaved) {
                // Create a SearchTitle object with the query and save it
                val searchTitle = SearchTitle(query)
                SearchTitleStore.addTitle(requireContext(), searchTitle)

                val adapter = binding.historyRecycler.adapter as SavedTitlesAdapter
                adapter.addItem(query)
            }
            linkRef.binding.btnTextUrl.setText(decodeUrl(query)) // Set the decoded URL
            changeTab(query, BrowseFragment(query))
        } else {
            Toast.makeText(requireContext(), "No Internet Connection \uD83C\uDF10", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSearchHintTitleRecyclerView() {
        // Get the updated history list
        val historyList = HistoryManager.getHistoryList(requireContext()).toMutableList()
        fileListAdapter = if (historyList.isNotEmpty()) {
            SearchItemAdapter(requireContext(), historyList.first())
        } else {
            SearchItemAdapter(requireContext(), null)
        }
        binding.searchHintTitleRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fileListAdapter
        }
    }

    private fun fillTitleInTextUrl() {
        val linkTubeRef = context as LinkTubeActivity
        val editText = linkTubeRef.binding.btnTextUrl
        editText.setSelection(editText.text.length)



    }


    @SuppressLint("Range")
    private fun startDownload(url: String, fileName: String?, fileExtension: String?) {
        val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)

        val actualFileName = fileName ?: URLUtil.guessFileName(url, null, fileExtension ?: "image/*")

        val request = DownloadManager.Request(downloadUri)
            .setTitle(actualFileName)
            .setDescription("Downloading")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, actualFileName)

        val downloadId = downloadManager.enqueue(request)

        // Notify FileActivity about download start
        val fileType = getFileType(fileExtension)
        val fileIconResId = getIconResId(fileExtension)

        (requireActivity() as? DownloadListener)?.onDownloadStarted(actualFileName, "", fileType, fileIconResId)

        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val notificationBuilder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setContentTitle(actualFileName)
            .setContentText("Downloading $actualFileName")
            .setSmallIcon(R.drawable.download_icon)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, 0, false)

        val notification = notificationBuilder.build()
        notificationManager.notify(downloadId.toInt(), notification)

        loadRunnable = object : Runnable {
            override fun run() {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    if (bytesTotal > 0) {
                        val progress = (bytesDownloaded * 100L / bytesTotal).toInt()
                        notificationBuilder.setProgress(100, progress, false)
                        notificationBuilder.setContentText("Downloading $actualFileName: $progress%")
                        notificationManager.notify(downloadId.toInt(), notificationBuilder.build())

                        binding.progressBar.progress = progress
                    }

                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        handler.removeCallbacks(this)
                        binding.progressBar.visibility = View.GONE

                        notificationBuilder.setContentText("Download complete")
                            .setProgress(0, 0, false)
                            .setOngoing(false)

                        // Create intent to open FileActivity
                        val intent = Intent(requireContext(), FileActivity::class.java).apply {
                            putExtra("fileName", actualFileName)
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            requireContext(),
                            0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        notificationBuilder.setContentIntent(pendingIntent)
                        notificationManager.notify(downloadId.toInt(), notificationBuilder.build())


                    } else if (status == DownloadManager.STATUS_FAILED) {
                        handler.removeCallbacks(this)
                        binding.progressBar.visibility = View.GONE

                        notificationBuilder.setContentText("Download failed")
                            .setProgress(0, 0, false)
                            .setOngoing(false)
                        notificationManager.notify(downloadId.toInt(), notificationBuilder.build())
                    }
                }
                cursor?.close()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(loadRunnable)
    }


    private fun getFileType(fileExtension: String?): FileType {
        return when (fileExtension?.toLowerCase()) {
            "pdf" -> FileType.PDF
            "jpg" -> FileType.IMAGE
            "jpeg" -> FileType.IMAGE
            "png" -> FileType.IMAGE
            "svg" -> FileType.IMAGE
            "mp4" -> FileType.VIDEO
            "mp3" -> FileType.AUDIO
            "html" -> FileType.WEBSITE
            "apk" -> FileType.APK
           else -> FileType.UNKNOWN
        }
    }


    private fun getIconResId(fileExtension: String?): Int {
        return when (getFileType(fileExtension)) {
            FileType.PDF -> R.drawable.internat_browser
            FileType.VIDEO -> R.drawable.video_browser
            FileType.IMAGE -> R.drawable.image_browser
            FileType.AUDIO -> R.drawable.music_download_browser
            FileType.APK -> R.drawable.pdf_image
            FileType.WEBSITE -> R.drawable.pdf_image
            FileType.MHTML -> R.drawable.pdf_image
            else -> R.drawable.image_browser
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "File Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }



    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerDownloadReceiver() {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        requireActivity().registerReceiver(downloadReceiver, filter)
    }

    private fun unregisterDownloadReceiver() {
        requireActivity().unregisterReceiver(downloadReceiver)
    }

    private val downloadReceiver = object : BroadcastReceiver() {
        @SuppressLint("Range")
        override fun onReceive(context: Context?, intent: Intent?) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent?.action) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(downloadId.toInt())
                handler.removeCallbacks(loadRunnable)
                binding.progressBar.visibility = View.GONE // Hide the progress bar

                // Query the DownloadManager for the completed download
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor != null && cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                        val fileName = uriString?.let { Uri.parse(it).lastPathSegment }

                        if (fileName != null) {
                            // Create and show a notification indicating download completion
                            val fileExtension = fileName.substringAfterLast('.', "")
                            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                                .setSmallIcon(R.drawable.download_icon) // Use a different icon for completion
                                .setContentTitle("$fileName")
                                .setContentText("Download complete of $fileName")
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setAutoCancel(true)

                            val intent = Intent(context, FileActivity::class.java).apply {
                                putExtra("fileName", fileName)
                            }
                            val pendingIntent = PendingIntent.getActivity(
                                context,
                                0,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            notificationBuilder.setContentIntent(pendingIntent)

                            notificationManager.notify(downloadId.toInt(), notificationBuilder.build())
                        }
                    }
                }
                cursor?.close()
            }
        }
    }



    override fun onPause() {
        super.onPause()
        unregisterDownloadReceiver()
        (requireActivity() as LinkTubeActivity).saveBookmarks()

    }




    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val result = binding.webView.hitTestResult
        when (result.type) {
            WebView.HitTestResult.IMAGE_TYPE -> {
                menu.add("Open image in new tab")
                menu.add("Preview Image")
                menu.add("Download Image")
                menu.add("Share Image")
                menu.add("Close")
            }

            WebView.HitTestResult.SRC_ANCHOR_TYPE, WebView.HitTestResult.ANCHOR_TYPE -> {
                menu.add("Open in New Tab")
                menu.add("Open Tab in Background")
                menu.add("Share")
                menu.add("Close")
            }

            WebView.HitTestResult.EDIT_TEXT_TYPE, WebView.HitTestResult.UNKNOWN_TYPE -> {}
            else -> {
                menu.add("Open in New Tab")
                menu.add("Open Tab in Background")
                menu.add("Share")
                menu.add("Close")
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {

        val message = Handler().obtainMessage()
        binding.webView.requestFocusNodeHref(message)
        val url = message.data.getString("url")
        val imgUrl = message.data.getString("src")

        when (item.title) {
            "Open in New Tab" -> {
                changeTab(url.toString(), BrowseFragment(url.toString()))
            }

            "Open Tab in Background" -> {
                changeTab(url.toString(), BrowseFragment(url.toString()), isBackground = true)
            }
            "Open image in new tab" -> {
                val imageUrl = imgUrl ?: url
                if (imageUrl != null) {
                    changeTab(imageUrl, BrowseFragment(imageUrl), isBackground = true)
                    Toast.makeText(requireContext(), "Tab opened in background", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Image URL not found", Toast.LENGTH_SHORT).show()
                }
            }

            "Download Image" -> {
                val imageUrl = imgUrl ?: url
                if (imageUrl != null) {
                    // Start a background task to check and download the image
                    validateAndDownloadImage(imageUrl)
                } else {
                    Toast.makeText(requireContext(), "Image URL is null", Toast.LENGTH_SHORT).show()
                }
            }

            "Preview Image" -> {
                if (imgUrl != null) {
                    if (imgUrl.contains("base64")) {
                        val pureBytes = imgUrl.substring(imgUrl.indexOf(",") + 1)
                        val decodedBytes = Base64.decode(pureBytes, Base64.DEFAULT)
                        val finalImg = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        // Create a FrameLayout to center the ImageView
                        val frameLayout = FrameLayout(requireContext())
                        val imgView = ShapeableImageView(requireContext())
                        imgView.setImageBitmap(finalImg)

                        // Set layout parameters to center the image
                        val layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        layoutParams.gravity = Gravity.CENTER
                        imgView.layoutParams = layoutParams

                        frameLayout.addView(imgView)

                        val imgDialog = MaterialAlertDialogBuilder(requireContext())
                            .create()
                        imgDialog.show()
                        imgView.layoutParams.width = Resources.getSystem().displayMetrics.widthPixels
                        imgView.layoutParams.height = (Resources.getSystem().displayMetrics.heightPixels * .50).toInt()
                        imgView.requestLayout()
                        imgDialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
                    } else {
                        changeTab(imgUrl, BrowseFragment(imgUrl))
                    }
                }
            }

            "Share Image" -> {
                val tempUrl = url ?: imgUrl
                if(tempUrl != null){
                    if(tempUrl.contains("base64")){

                        val pureBytes = tempUrl.substring(tempUrl.indexOf(",") + 1)
                        val decodedBytes = Base64.decode(pureBytes, Base64.DEFAULT)
                        val finalImg = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        val path = MediaStore.Images.Media.
                        insertImage(requireActivity().contentResolver,
                            finalImg, "Image", null)

                        ShareCompat.IntentBuilder(requireContext()).setChooserTitle("Sharing Url!")
                            .setType("image/*")
                            .setStream(Uri.parse(path))
                            .startChooser()
                    }
                    else{
                        ShareCompat.IntentBuilder(requireContext()).setChooserTitle("Sharing Url!")
                            .setType("text/plain").setText(tempUrl)
                            .startChooser()
                    }
                }
                else Snackbar.make(binding.root, "Not a Valid Link!", 3000).show()
            }
            "Close" -> {}
        }

        return super.onContextItemSelected(item)
    }
    // Function to validate and download the image
    private fun validateAndDownloadImage(imageUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val finalUrl = if (imageUrl.contains("images?q=")) {
                    extractImageUrl(imageUrl)
                } else {
                    imageUrl
                }

                if (finalUrl.startsWith("data:image/")) {
                    // Handle data URLs directly
                    handleDataUrl(finalUrl)
                } else {
                    val resolvedUrl = if (finalUrl.contains("encrypted-tbn0")) {
                        resolveTbnUrl(finalUrl)
                    } else {
                        finalUrl
                    }

                    if (resolvedUrl.startsWith("http") || resolvedUrl.startsWith("https")) {
                        val url = URL(resolvedUrl)
                        val urlConnection = url.openConnection() as HttpURLConnection
                        urlConnection.connect()

                        val contentType = urlConnection.contentType
                        val extension = determineExtension(url.path, contentType)

                        if (isValidImageType(contentType) && isValidExtension(extension)) {
                            withContext(Dispatchers.Main) {
                                startDownload(resolvedUrl, null, extension)
                                Toast.makeText(requireContext(), "Image download started", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Invalid image content type", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Error: Unknown protocol in resolved URL", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun determineExtension(path: String?, contentType: String?): String {
        var extension = path?.substringAfterLast('.', "") ?: ""

        if (extension.isEmpty()) {
            extension = when {
                contentType?.contains("jpeg") == true -> "jpg"
                contentType?.contains("png") == true -> "png"
                contentType?.contains("gif") == true -> "gif"
                contentType?.contains("bmp") == true -> "bmp"
                contentType?.contains("webp") == true -> "webp"
                contentType?.contains("svg") == true -> "svg"
                contentType?.contains("tiff") == true -> "tiff"
                contentType?.contains("ico") == true -> "ico"
                contentType?.contains("heic") == true -> "heic"
                contentType?.contains("avif") == true -> "avif"
                else -> "img"
            }
        }

        return extension
    }

    private fun isValidImageType(contentType: String?): Boolean {
        return contentType?.startsWith("image/") == true
    }

    private fun isValidExtension(extension: String): Boolean {
        val supportedExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff", "ico", "heic", "avif")
        return supportedExtensions.contains(extension)
    }

    private suspend fun handleDataUrl(dataUrl: String) {
        try {
            val mimeType = dataUrl.substringAfter("data:").substringBefore(";base64,")
            val base64Data = dataUrl.substring(dataUrl.indexOf(",") + 1)
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)

            var fileName = "downloaded_image"

            val fileNameIndex = dataUrl.indexOf("filename=")
            if (fileNameIndex != -1) {
                var endIndex = dataUrl.indexOf("&", fileNameIndex)
                if (endIndex == -1) {
                    endIndex = dataUrl.length
                }
                fileName = dataUrl.substring(fileNameIndex + 9, endIndex)
            } else {
                val uri = Uri.parse(dataUrl)
                val path = uri.path
                if (path != null) {
                    val segments = path.split("/")
                    if (segments.isNotEmpty()) {
                        fileName = segments.last()
                    }
                } else {
                    val timestamp = System.currentTimeMillis()
                    fileName = "image_$timestamp"
                }
            }

            val extension = when {
                mimeType.contains("jpeg") -> "jpg"
                mimeType.contains("png") -> "png"
                mimeType.contains("gif") -> "gif"
                mimeType.contains("bmp") -> "bmp"
                mimeType.contains("webp") -> "webp"
                mimeType.contains("svg") -> "svg"
                mimeType.contains("tiff") -> "tiff"
                mimeType.contains("ico") -> "ico"
                mimeType.contains("heic") -> "heic"
                mimeType.contains("avif") -> "avif"
                else -> "img"
            }

            fileName += ".$extension"

            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

            FileOutputStream(file).use { fos ->
                fos.write(decodedBytes)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Image downloaded successfully", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun extractImageUrl(imageUrl: String): String {
        val urldataParam = "images?q="
        val startIndex = imageUrl.indexOf(urldataParam) + urldataParam.length
        val endIndex = imageUrl.indexOf("&", startIndex).takeIf { it != -1 } ?: imageUrl.length
        return URLDecoder.decode(imageUrl.substring(startIndex, endIndex), "UTF-8")
    }

    private fun resolveTbnUrl(tbnUrl: String): String {
        return try {
            val connection = URL(tbnUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connect()
            val redirectedUrl = connection.url.toString()
            connection.disconnect()
            redirectedUrl
        } catch (e: Exception) {
            tbnUrl
        }
    }

    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        TODO("Not yet implemented")
    }

    fun loadAd() {
        val adRequest = AdRequest.Builder().build()


        InterstitialAd.load(
            requireContext(),
            "ca-app-pub-4270893888625106/2835817173",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
            })
    }

    override fun onItemClick(historyItem: HistoryItem) {
        // Handle item click here
        val query = historyItem.url
        openUrlInBrowser(query)
        showLoadingToast()
    }
    private fun showLoadingToast() {
        Toast.makeText(requireContext(), "Provided link is loading. You can go back.", Toast.LENGTH_LONG).show()
    }
    private fun openUrlInBrowser(query: String) {
        val browserFragment = BrowseFragment(urlNew = query)
        changeTab("Brave", browserFragment)
    }

    fun onBackPressed(): Boolean {
        val webView = binding.webView
        return if (isLoadingPage) {
            true // Back press handled within the fragment
        } else if (webView.canGoBack()) {
            webView.goBack()
            TabQuickButtonAdapter.updateTabs()
            LinkTubeActivity.tabs2Btn.text = tabsList.size.toString()
            true // Back press handled within the fragment
        } else {
            false // Back press not handled within the fragment
        }
    }
    fun isLoading(): Boolean {
        return binding.webView.progress != 100
    }

}


