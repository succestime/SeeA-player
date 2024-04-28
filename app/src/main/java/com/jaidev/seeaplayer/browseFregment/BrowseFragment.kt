

package com.jaidev.seeaplayer.browseFregment


import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.util.Base64
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.DownloadListener
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.internal.ViewUtils
import com.google.android.material.snackbar.Snackbar
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.HistoryAdapter
import com.jaidev.seeaplayer.browserActivity.HistoryBrowser
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity.Companion.myPager
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity.Companion.tabsList
import com.jaidev.seeaplayer.browserActivity.changeTab
import com.jaidev.seeaplayer.browserActivity.checkForInternet
import com.jaidev.seeaplayer.dataClass.FileType
import com.jaidev.seeaplayer.dataClass.HistoryItem
import com.jaidev.seeaplayer.dataClass.HistoryManager
import com.jaidev.seeaplayer.databinding.FragmentBrowseBinding
import java.io.ByteArrayOutputStream


class BrowseFragment(private var urlNew : String) : Fragment(), DownloadListener {
    lateinit var binding: FragmentBrowseBinding

    var webIcon: Bitmap? = null
    private lateinit var historyAdapter: HistoryAdapter
    private var isBtnTextUrlFocused = false // Flag to track if btnTextUrl has been focused
    private var mInterstitialAd: InterstitialAd? = null
lateinit var mAdView : AdView
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

        fun onDownloadProgress(downloadId: Long, bytesDownloaded: Long, totalBytes: Long)

        fun onDownloadCompleted(downloadId: Long)
    }


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_browse, container, false)
        binding = FragmentBrowseBinding.bind(view)
        registerForContextMenu(binding.webView)
        binding.webView.apply {

            when {
             URLUtil.isValidUrl(urlNew) -> loadUrl(urlNew)
              urlNew.contains(".com", ignoreCase = true) -> loadUrl(urlNew)
                else -> loadUrl("https://www.google.com/search?q=$urlNew")
            }

        }
        val rootView = view.rootView

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom
            val isKeyboardVisible = keypadHeight > screenHeight * 0.15
            binding.historyRecycler.visibility = if (isKeyboardVisible) View.VISIBLE else View.GONE
        }


        return view

    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility", "ObsoleteSdkInt",
        "RestrictedApi"
    )
    override fun onResume() {
        super.onResume()
        registerDownloadReceiver()
        tabsList[myPager.currentItem].name =
            binding.webView.url.toString()
        LinkTubeActivity.tabsBtn.text = tabsList.size.toString()

        binding.webView.setDownloadListener { url, _, _, mimeType, _ ->
            // Determine the file extension based on MIME type
            val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)

            // Extract the filename from the URL
            val fileName = URLUtil.guessFileName(url, null, fileExtension)

            // Check the file type and initiate appropriate download action
            if (!mimeType.isNullOrBlank() && !fileExtension.isNullOrBlank()) {
                when {
                    mimeType.startsWith("application/pdf") -> {
                        startDownload(url, "document.pdf", fileExtension)
                    }
                    mimeType.startsWith("video/") -> {
                        // Assuming the filename is 'video.mp4' for demonstration
                        startDownload(url, "video.mp4", fileExtension)
                    }
                    mimeType.startsWith("image/") -> {
                        // Assuming the filename is 'image.jpg' for demonstration
                        startDownload(url, "image.jpg", fileExtension)
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
            linkRef.binding.btnTextUrl.requestFocus()
         loadHistoryItems()
        }

        historyAdapter = HistoryAdapter(
            HistoryBrowser.historyItems,
            object : HistoryAdapter.ItemClickListener {
                override fun onItemClick(historyItem: HistoryItem) {
                    // Handle item click here, if needed
                }
            },
            isBrowseFragment = true
        )

        binding.historyRecycler.setHasFixedSize(true)
        binding.historyRecycler.setItemViewCacheSize(5)
        binding.historyRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecycler.adapter = historyAdapter

        // Set editor action listener for btnTextUrl (IME_ACTION_DONE or IME_ACTION_GO)
        linkRef.binding.btnTextUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                val query = linkRef.binding.btnTextUrl.text.toString()
                if (checkForInternet(requireContext())) {
                    // Hide keyboard and RecyclerView
                    ViewUtils.hideKeyboard(linkRef.binding.btnTextUrl)
                    binding.historyRecycler.visibility = View.GONE
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



        binding.swipeRefreshBrowser.setOnRefreshListener {
            binding.webView.reload()

             binding.swipeRefreshBrowser.visibility = View.GONE
        }



        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
         settings.displayZoomControls = false
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4501.0 Mobile Safari/537.36"
       settings.useWideViewPort = true
        settings.loadWithOverviewMode = true




            webViewClient = object : WebViewClient() {

                @Deprecated("Deprecated in Java")
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    // Handle WebView error
                    Log.e(
                        "WebViewError",
                        "Error loading $failingUrl: $description (error code: $errorCode)"
                    )
                }


                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    settings.setSupportZoom(true)
                    settings.javaScriptEnabled = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false


                }

                override fun doUpdateVisitedHistory(
                    view: WebView?,
                    url: String?,
                    isReload: Boolean
                ) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    linkRef.binding.btnTextUrl.text = SpannableStringBuilder(url)
                    tabsList[LinkTubeActivity.myPager.currentItem].name =
                        url.toString()
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    linkRef.binding.progressBar.progress = 0
                    linkRef.binding.progressBar.visibility = View.VISIBLE

                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    linkRef.binding.progressBar.visibility = View.GONE
                    binding.webView.zoomOut()
                    // Save the visited page to history


                    val websiteTitle = HistoryManager.extractWebsiteTitle(url ?: "")
                    val favicon = view?.favicon
                    val timestamp = System.currentTimeMillis()
                    val historyItem = HistoryItem(url ?: "", websiteTitle, timestamp, favicon)
                    // Add history item to the HistoryManager
                  HistoryManager.addHistoryItem(historyItem, requireContext())

                }


            }
            webChromeClient = object : WebChromeClient() {
                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    super.onReceivedIcon(view, icon)
                    try {
                        linkRef.binding.webIcon.setImageBitmap(icon)
                        webIcon = icon
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
                    linkRef.binding.progressBar.progress = newProgress
                }

            }

            binding.webView.setOnTouchListener { _, motionEvent ->
                linkRef.binding.root.onTouchEvent(motionEvent)
                return@setOnTouchListener false
            }

        }

        loadHistoryItems()

    }
    private fun performSearch(query: String) {
        if (checkForInternet(requireContext())) {
            changeTab(query, BrowseFragment(query))
        } else {
            Toast.makeText(requireContext(), "No Internet Connection \uD83C\uDF10", Toast.LENGTH_SHORT).show()
        }
    }
    @SuppressLint("Range")
    private fun startDownload(url: String, fileName: String, fileExtension: String?) {
        val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)

        val request = DownloadManager.Request(downloadUri)
            .setTitle(fileName) // Set the desired file name explicitly
            .setDescription("Downloading")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadId = downloadManager.enqueue(request)

        // Notify FileActivity about download start
        val fileType = getFileType(fileExtension)
        val fileIconResId = getIconResId(fileExtension)

        // Update UI to show download progress
        (requireActivity() as? DownloadListener)?.onDownloadStarted(fileName, "", fileType, fileIconResId)

        // Create a notification to display download progress
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val notificationBuilder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setContentTitle(fileName) // Use the provided file name in the notification
            .setContentText("Downloading $fileName")
            .setSmallIcon(R.drawable.download_icon)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        val notification = notificationBuilder.build()
        notificationManager.notify(downloadId.toInt(), notification)


        }




    private fun getFileType(fileExtension: String?): FileType {
        return when (fileExtension?.toLowerCase()) {
            "pdf" -> FileType.PDF
            "jpg" -> FileType.IMAGE
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
        override fun onReceive(context: Context?, intent: Intent?) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent?.action) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                // Handle download completion

            }
        }
    }
    override fun onPause() {
        super.onPause()
        unregisterDownloadReceiver()

        (requireActivity() as LinkTubeActivity).saveBookmarks()
        // for clearing all  webView data
//        binding.webView.apply {
//            clearMatches()
//            clearHistory()
//            clearFormData()
//            clearSslPreferences()
//            clearCache(true)
//
//            CookieManager.getInstance().removeAllCookies(null)
//            WebStorage.getInstance().deleteAllData()
//        }
    }
    // Assuming this is in your BrowseFragment or similar place

    @SuppressLint("NotifyDataSetChanged")
    private fun loadHistoryItems() {
        val historyList = HistoryManager.getHistoryList(requireContext())
        val limitedHistoryList = historyList.take(15)
        HistoryBrowser.historyItems.clear()
        HistoryBrowser.historyItems.addAll(limitedHistoryList)
        historyAdapter.notifyDataSetChanged()
        updateEmptyStateVisibility()
    }

    private fun updateEmptyStateVisibility() {
        if (HistoryBrowser.historyItems.isEmpty()) {
            binding.historyRecycler.visibility = View.GONE
        } else {
            binding.historyRecycler.visibility = View.VISIBLE
        }
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
                menu.add("View Image")
                menu.add("Save Image")
                menu.add("Share")
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


            "View Image" ->{
                if(imgUrl != null) {
                    if (imgUrl.contains("base64")) {
                        val pureBytes = imgUrl.substring(imgUrl.indexOf(",") + 1)
                        val decodedBytes = Base64.decode(pureBytes, Base64.DEFAULT)
                        val finalImg =
                            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        val imgView = ShapeableImageView(requireContext())
                        imgView.setImageBitmap(finalImg)

                        val imgDialog = MaterialAlertDialogBuilder(requireContext()).setView(imgView).create()
                        imgDialog.show()

                        imgView.layoutParams.width = Resources.getSystem().displayMetrics.widthPixels
                        imgView.layoutParams.height = (Resources.getSystem().displayMetrics.heightPixels * .75).toInt()
                        imgView.requestLayout()

                        imgDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                    }
                    else changeTab(imgUrl, BrowseFragment(imgUrl))
                }
            }

            "Save Image" ->{
                if(imgUrl != null) {
                    if (imgUrl.contains("base64")) {
                        val pureBytes = imgUrl.substring(imgUrl.indexOf(",") + 1)
                        val decodedBytes = Base64.decode(pureBytes, Base64.DEFAULT)
                        val finalImg =
                            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        MediaStore.Images.Media.insertImage(
                            requireActivity().contentResolver,
                            finalImg, "Image", null
                        )
                        Snackbar.make(binding.root, "Image Saved Successfully", 3000).show()
                    }
                    else startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(imgUrl)))
                }
            }

            "Share" -> {
                val tempUrl = url ?: imgUrl
                if(tempUrl != null){
                    if(tempUrl.contains("base64")){

                        val pureBytes = tempUrl.substring(tempUrl.indexOf(",") + 1)
                        val decodedBytes = Base64.decode(pureBytes, Base64.DEFAULT)
                        val finalImg = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        val path = MediaStore.Images.Media.insertImage(requireActivity().contentResolver,
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
}
