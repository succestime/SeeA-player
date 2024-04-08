

package com.jaidev.seeaplayer.browseFregment

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.jaidev.seeaplayer.DownloadWithPauseResumeNew
import com.jaidev.seeaplayer.LinkTubeActivity
import com.jaidev.seeaplayer.LinkTubeActivity.Companion.myPager
import com.jaidev.seeaplayer.LinkTubeActivity.Companion.tabsList
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.changeTab
import com.jaidev.seeaplayer.databinding.FragmentBrowseBinding
import java.io.ByteArrayOutputStream



class BrowseFragment(private var urlNew : String) : Fragment() {
    lateinit var binding: FragmentBrowseBinding
    var webIcon: Bitmap? = null
    private var url: String? = null
    companion object {

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
                else -> loadUrl("https://search.brave.com/search?q=$urlNew")
            }

        }




        return view

    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility", "ObsoleteSdkInt")
    override fun onResume() {
        super.onResume()
        tabsList[myPager.currentItem].name =
            binding.webView.url.toString()
        LinkTubeActivity.tabsBtn.text = tabsList.size.toString()

        // for downloading file using external download manager
        binding.webView.setDownloadListener { url, _, _, _, _ ->
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)))
        }

        val linkRef = requireActivity() as LinkTubeActivity

        var frag: BrowseFragment? = null
        try {
            frag = tabsList[linkRef.binding.myPager.currentItem].fragment as BrowseFragment
        }catch (_:Exception){}


        binding.webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            linkRef.saveData()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(requireActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED) {
                    Log.v("TAG", "Permission is granted")
                    downloadDialog(url, userAgent, contentDisposition, mimetype)
                    linkRef.saveData()
                } else {
                    Log.v("TAG", "Permission is revoked")
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                }
            } else {
                Log.v("TAG", "Permission is granted")
                downloadDialog(url, userAgent, contentDisposition, mimetype)
            }
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

        linkRef.binding.refreshBrowserBtn.visibility = View.VISIBLE
        linkRef.binding.refreshBrowserBtn.setOnClickListener {
            binding.webView.reload()
        }

        linkRef.binding.bottomRefreshBrowser.visibility = View.VISIBLE
        linkRef.binding.bottomRefreshBrowser.setOnClickListener {
            binding.webView.reload()
        }


        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false



            webViewClient = object : WebViewClient() {

                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    if (LinkTubeActivity.isDesktopSite)
                        view?.evaluateJavascript(
                            "document.querySelector('meta[name=\"viewport\"]').setAttribute('content'," +
                                    " 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));",
                            null)

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
                    linkRef.saveData()
//                    if (url!!.contains(
//                            "you",
//                            ignoreCase = false
//                        )
//                    )
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    linkRef.binding.progressBar.visibility = View.GONE
                    binding.webView.zoomOut()


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
///////////////////////////////
////////////////////////////// //
            // in video teacher was saying this can make issue so also watch this this
            binding.webView.reload()
        }
    }




    override fun onPause() {
        super.onPause()
        (requireActivity() as LinkTubeActivity).saveBookmarks()
        // for clearing all  webView data
        binding.webView.apply {
            clearMatches()
            clearHistory()
            clearFormData()
            clearSslPreferences()
            clearCache(true)

            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
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


    private fun downloadDialog(url: String?, userAgent: String?, contentDisposition: String?, mimetype: String?) {
        val linkRef = requireActivity() as LinkTubeActivity

        val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.custon_dialog_downloader, null)
        val buttonYes = dialogView.findViewById<Button>(R.id.Button_Yes)
        val buttonNo = dialogView.findViewById<Button>(R.id.Button_No)
        val editTextFileName = dialogView.findViewById<EditText>(R.id.EditTextFileName)
        val editTextFileUrl = dialogView.findViewById<EditText>(R.id.EditTextFile_Url)

        builder.setView(dialogView)
        val alertDialog = builder.create()
        alertDialog.setCancelable(false)

        editTextFileName.setText(filename)
        editTextFileUrl.setText(url)

        val fi = editTextFileName.text.toString()

        buttonYes.setOnClickListener {
            val intent = Intent(it.context, DownloadWithPauseResumeNew::class.java)
            intent.putExtra("urlss", url)
            intent.putExtra("filenames", fi)
            startActivity(intent)
            alertDialog.dismiss()
        }

        buttonNo.setOnClickListener {
            alertDialog.dismiss()
        }

        builder.setTitle(R.string.download_title)
        builder.setMessage(getString(R.string.download_file)+ ' '  + filename)

        linkRef.saveData()

        builder.setPositiveButton(getString(R.string.ok)) { dialog, which ->
            val request = DownloadManager.Request(Uri.parse(url))
            val cookie = CookieManager.getInstance().getCookie(url)

            request.addRequestHeader("Cookie", cookie)
            request.addRequestHeader("User-Agent", userAgent)

            // Set MIME type for the download request based on the detected mimetype
            if (mimetype != null) {
                request.setMimeType(mimetype)
            }

            request.allowScanningByMediaScanner()

            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)

            downloadManager.enqueue(request)
        }

        builder.setNegativeButton("No") { dialog, which ->
            binding.webView.goBack()
        }

        alertDialog.show()
    }

    private fun getSystemService(serviceName: String): Any? {
        return when (serviceName) {
            Context.DOWNLOAD_SERVICE -> requireContext().getSystemService(Context.DOWNLOAD_SERVICE)
            else -> null
        }

    }
}
