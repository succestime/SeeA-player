
package com.jaidev.seeaplayer.browserActivity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.TabAdapter
import com.jaidev.seeaplayer.browseFregment.BrowseFragment
import com.jaidev.seeaplayer.browseFregment.HomeFragment
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity.Companion.myPager
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity.Companion.tabsBtn
import com.jaidev.seeaplayer.dataClass.Bookmark
import com.jaidev.seeaplayer.dataClass.HistoryManager
import com.jaidev.seeaplayer.dataClass.Tab
import com.jaidev.seeaplayer.dataClass.exitApplication
import com.jaidev.seeaplayer.databinding.ActivityLinkTubeBinding
import com.jaidev.seeaplayer.databinding.BookmarkDialogBinding
import com.jaidev.seeaplayer.databinding.TabViewBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LinkTubeActivity : AppCompatActivity() {

    private var printJob : PrintJob? = null
    // Assuming 'this' is a valid Context from an Activity or Fragment
    lateinit var mAdView: AdView
    private var mInterstitialAd : InterstitialAd? = null
    private var rewardedInterstitialAd : RewardedInterstitialAd? = null
    private var tempText: CharSequence? = null
    @SuppressLint("StaticFieldLeak")
    lateinit var binding: ActivityLinkTubeBinding
    companion object {
        var tabsList: ArrayList<Tab> = ArrayList()
        private var isFullscreen: Boolean = true
        var isDesktopSite: Boolean = false
        var bookmarkList: ArrayList<Bookmark> = ArrayList()
        var bookmarkIndex : Int = -1
        lateinit var myPager : ViewPager2
        lateinit var tabsBtn : MaterialTextView
        const val REQUEST_CODE_SPEECH_INPUT = 2000
        const val MAX_HISTORY_SIZE = 150
        private const val TABS_LIST_KEY = "tabs_list"

    }

    @SuppressLint("ObsoleteSdkInt")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setTheme(More.themesList[More.themeIndex])
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.statusBarColor = Color.parseColor("#373636")

        binding = ActivityLinkTubeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()



        tabsList.add(Tab("Home",HomeFragment(), LinkTubeActivity()))
        binding.myPager.adapter = TabsAdapter(supportFragmentManager, lifecycle)
        binding.myPager.isUserInputEnabled = false
        myPager = binding.myPager
        tabsBtn = binding.tabBtn

        changeFullscreen(enable = true)

    }

    fun initializeBinding(){
        binding.googleMicBtn.setOnClickListener {
            speak()
        }

        binding.moreBtn.setOnClickListener {
            val popupMenu = PopupMenu(this@LinkTubeActivity, binding.moreBtn)
            popupMenu.menuInflater.inflate(R.menu.browser_menu, popupMenu.menu)


// Set icons to be visible
            try {
                val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldPopup.isAccessible = true
                val popup = fieldPopup.get(popupMenu)
                popup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(popup, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popupMenu.setOnMenuItemClickListener { item ->

                when (item.itemId) {
                    R.id.newTab -> {
                        changeTab("Home", HomeFragment())
                    }

                    R.id.history -> {
                        startActivity(Intent(this, HistoryBrowser::class.java))
                    }

                    R.id.download -> {
                        startActivity(Intent(this, FileActivity::class.java))
                    }

                    R.id.save -> {
                        save()
                    }

                    R.id.bookmark -> {
                        bookMark()
                    }

                    R.id.desktop -> {
                        desktopMade()
                    }
                    R.id.fullScreen -> {
                        fullScreen()
                    }

                    R.id.share -> {
                        share()
                    }

                    R.id.exit -> {
                        exit()
                    }
                }
                true
            }

            popupMenu.show()

        }
        binding.bottomMoreBrowser.setOnClickListener {
            val popupMenu = PopupMenu(this@LinkTubeActivity, binding.bottomMoreBrowser)
            popupMenu.menuInflater.inflate(R.menu.browser_menu, popupMenu.menu)

// Set icons to be visible
            try {
                val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldPopup.isAccessible = true
                val popup = fieldPopup.get(popupMenu)
                popup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(popup, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.newTab -> {
                        changeTab("Home", HomeFragment())
                    }

                    R.id.history -> {
                        startActivity(Intent(this, HistoryBrowser::class.java))
                    }

                    R.id.download -> {
                        startActivity(Intent(this, FileActivity::class.java))
                    }
                    R.id.save -> {
                        save()
                    }
                    R.id.bookmark -> {
                        bookMark()
                    }
                    R.id.desktop -> {
                        desktopMade()
                    }
                    R.id.fullScreen -> {
                        fullScreen()
                    }
                    R.id.share -> {
                        share()

                    }
//
//                    R.id.setting -> {
//
//                    }
                    R.id.exit -> {
                        exit()
                    }
                }
                true
            }

            popupMenu.show()


        }

        binding.bookMarkBtn.setOnClickListener {
            bookMark()
        }


        val isTablet = resources.configuration.smallestScreenWidthDp >= 600

        // Show or hide the buttons based on the device type
        if (isTablet) {
            binding.backBrowserBtn.visibility = View.VISIBLE
            binding.forwardBrowserBtn.visibility = View.VISIBLE
            binding.topDownloadBrowser.visibility = View.VISIBLE
        } else {
            binding.backBrowserBtn.visibility = View.GONE
            binding.forwardBrowserBtn.visibility = View.GONE
            binding.topDownloadBrowser.visibility = View.GONE
        }
    }


    private fun desktopMade(){
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (_: Exception) {
        }
        frag?.binding?.webView?.apply {
            val isTablet = resources.configuration.smallestScreenWidthDp >= 600

            if (isTablet) {
                // Tablet-specific desktop mode
                if (isDesktopSite) {
                    // Currently in desktop mode on tablet, switch to mobile/desktop mode
                    settings.userAgentString = null

                    isDesktopSite = false // Toggle desktop mode state
                    Toast.makeText(this@LinkTubeActivity, "Desktop Mode Disabled", Toast.LENGTH_LONG).show()
                } else {
                    // Currently in mobile/desktop mode on tablet, switch to tablet-specific desktop mode
                    settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4501.0 Safari/537.36 Edge/91.0.866.0"
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1200px')", null)
                    isDesktopSite = true // Toggle desktop mode state
                    Toast.makeText(this@LinkTubeActivity, "Desktop Mode Enabled", Toast.LENGTH_LONG).show()
                }
            } else {
                // Mobile/desktop mode for non-tablet devices
                if (isDesktopSite) {
                    // Currently in desktop mode on mobile, switch to mobile/desktop mode
                    settings.userAgentString = null
                    isDesktopSite = false // Toggle desktop mode state
                    Toast.makeText(this@LinkTubeActivity, "Desktop Mode Disabled", Toast.LENGTH_LONG).show()
                } else {
                    // Currently in mobile mode on mobile, switch to desktop mode
                    settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4501.0 Safari/537.36 Edge/91.0.866.0"
                    settings.useWideViewPort = true
                    isDesktopSite = true // Toggle desktop mode state
                    Toast.makeText(this@LinkTubeActivity, "Desktop Mode Enabled", Toast.LENGTH_LONG).show()
                }

            }
            frag.binding.webView.reload()
            frag.binding.swipeRefreshBrowser.isRefreshing = false
        }

    }

    private fun bookMark(){
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (_: Exception) {
        }

        frag?.let {
            if (bookmarkIndex == -1) {
                val viewB = layoutInflater.inflate(
                    R.layout.bookmark_dialog,
                    binding.root,
                    false
                )
                val bBinding = BookmarkDialogBinding.bind(viewB)

                val dialogB = MaterialAlertDialogBuilder(this@LinkTubeActivity)
                    .setTitle("Add Bookmark")
                    .setMessage("Url: ${it.binding.webView.url}")
                    .setPositiveButton("Add") { dialog, _ ->
                        try {
                            val array = ByteArrayOutputStream()
                            it.webIcon?.compress(
                                Bitmap.CompressFormat.PNG,
                                100,
                                array
                            )
                            bookmarkList.add(
                                Bookmark(
                                    name = bBinding.bookmarkTitle.text.toString(),
                                    url = it.binding.webView.url!!,
                                    image = array.toByteArray()
                                )
                            )
                            Toast.makeText(
                                this@LinkTubeActivity,
                                "Bookmark added successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@LinkTubeActivity,
                                "Bookmark failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .setView(viewB)
                    .create()
                dialogB.show()
                bBinding.bookmarkTitle.setText(it.binding.webView.title)
            } else {
                val dialogB = MaterialAlertDialogBuilder(this@LinkTubeActivity)
                    .setTitle("Remove Bookmark")
                    .setMessage("Url: ${it.binding.webView.url}")
                    .setPositiveButton("Remove") { dialog, _ ->
                        bookmarkList.removeAt(bookmarkIndex)
                        Toast.makeText(
                            this@LinkTubeActivity,
                            "Bookmark removed successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .create()
                dialogB.show()
            }
        }
    }

 private fun fullScreen(){
     isFullscreen = if (isFullscreen) {
         changeFullscreen(enable = false)
         false
     }
     else {
         changeFullscreen(enable = true)
         true
     }

 }

    private fun share() {
        var frag: BrowseFragment? = null
        try {
            frag = LinkTubeActivity.tabsList[myPager.currentItem].fragment as BrowseFragment
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val url = frag?.binding?.webView?.url

        if (url != null && url.isNotEmpty()) {
            // If URL is present, share the URL
            ShareCompat.IntentBuilder(this@LinkTubeActivity)
                .setChooserTitle("Share URL")
                .setType("text/plain")
                .setText(url)
                .startChooser()
        } else {
            // If URL is not present, notify the user
            Toast.makeText(this@LinkTubeActivity, "URL not found", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun exit(){
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Exit")
            .setMessage("Do you want to exit the app?")
            .setPositiveButton("Yes") { _, _ ->
                exitApplication()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        val customDialog = builder.create()
        customDialog.show()


    }

    private fun save(){
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (_: Exception) {
        }
        if (frag != null)
            saveAsPdf(web = frag.binding.webView)
        else Snackbar.make(binding.root, "First Open A WebPage\uD83D\uDE03", 3000)
            .show()
    }

    private fun checkHistorySize() {
        if (HistoryManager.getHistorySize(this) >= MAX_HISTORY_SIZE) {
            val builder = MaterialAlertDialogBuilder(this)
            builder.setTitle("Delete History Items")
                .setMessage("The history has reached its maximum capacity. Delete some items?")
                .setPositiveButton("Delete") { _, _ ->
                    startActivity(Intent(this, HistoryBrowser::class.java))
                    finish()
                }
            val customDialog = builder.create()
            customDialog.show()
        }
    }

    fun speak() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hi, speak something")

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    fun loadAd(){
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }
        })

    }

    fun rewardedIAd(){
        val adRequest = AdRequest.Builder().build()
        RewardedInterstitialAd.load(this,"ca-app-pub-3504589383575544/3262210040",
            adRequest, object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(p0: RewardedInterstitialAd) {
                    rewardedInterstitialAd=p0
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    rewardedInterstitialAd=null
                }
            })

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SPEECH_INPUT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val speechInput = result?.get(0)
                    val currentFragment = tabsList[myPager.currentItem].fragment
                    val activity = tabsList[myPager.currentItem].activity
                    if (currentFragment is BrowseFragment) {
                        val webView = currentFragment.binding.webView
                        handleSpeechInput(webView, speechInput)
                    } else if (activity is LinkTubeActivity) {
                        activity.navigateToBrowserFragment(speechInput ?: "")
                    } else {
                        // Handle the case when the current fragment is not a BrowseFragment or LinkTubeActivity
                    }
                }
            }
        }
    }

    fun navigateToBrowserFragment(query: String) {
        val url = "https://www.google.com/search?q=${Uri.encode(query)}"
        val browserFragment = BrowseFragment(urlNew = url)
        changeTab("Brave", browserFragment)
    }


    private fun handleSpeechInput(webView: WebView, speechInput: String?) {
        if (!speechInput.isNullOrEmpty()) {
            // Check if the input is a number or a string
            val searchQuery =
                if (speechInput.matches(Regex("-?\\d+(\\.\\d+)?"))) { // Check if input is a number
                    "https://www.google.com/search?q=$speechInput"
                } else {
                    "https://www.google.com/search?q=${Uri.encode(speechInput)}"
                }
            webView.loadUrl(searchQuery)
        }
    }



    @Deprecated("Deprecated in Java")
    @SuppressLint("NotifyDataSetChanged")
    override fun onBackPressed() {
        var handled = false
        val currentFragment = tabsList.getOrNull(binding.myPager.currentItem)?.fragment
        if (currentFragment is BrowseFragment) {
            // Check if the current fragment is a BrowseFragment and can handle the back press
            handled = currentFragment.onBackPressed()
        }

        if (!handled) {
            // If back press is not handled by the current fragment, perform default behavior
            if (currentFragment is BrowseFragment && currentFragment.isLoading()) {
                // If the current fragment is a BrowseFragment and a page is loading, do nothing
            } else {
                when {
                    binding.myPager.currentItem != 0 -> {
                        tabsList.removeAt(binding.myPager.currentItem)
                        binding.myPager.adapter?.notifyDataSetChanged()
                        binding.myPager.currentItem = tabsList.size - 1
                    }
                    else -> super.onBackPressed()
                }
            }
        }
    }


    private inner class TabsAdapter(fa: FragmentManager, lc: Lifecycle) :
        FragmentStateAdapter(fa, lc) {
        override fun getItemCount(): Int = tabsList.size

        override fun createFragment(position: Int): Fragment = tabsList[position].fragment
    }


    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            window.navigationBarColor = ContextCompat.getColor(this, R.color.link_tube_tab_color)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()


        } else {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.link_tube_tab_color)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()

        }
    }
    private fun initializeView() {

        binding.homeBrowserBtn.setOnClickListener {
            changeTab("Home", HomeFragment())
        }
        binding.downloadBrowser.setOnClickListener {
            var frag: BrowseFragment? = null
            try {
                frag = LinkTubeActivity.tabsList[myPager.currentItem].fragment as BrowseFragment
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Get the current URL from the WebView
            val currentUrl = frag?.binding?.webView?.url
            if (currentUrl != null) {
                downloadCurrentWebPage(currentUrl)
            } else {
                Toast.makeText(this@LinkTubeActivity, "No webpage to download", Toast.LENGTH_SHORT).show()
            }// If URL is not present, notify the user
        }

        binding.topDownloadBrowser.setOnClickListener {
            var frag: BrowseFragment? = null
            try {
                frag = LinkTubeActivity.tabsList[myPager.currentItem].fragment as BrowseFragment
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Get the current URL from the WebView
            val currentUrl = frag?.binding?.webView?.url
            if (currentUrl != null) {
                downloadCurrentWebPage(currentUrl)
            } else {
                Toast.makeText(this@LinkTubeActivity, "No webpage to download", Toast.LENGTH_SHORT).show()
            }// If URL is not present, notify the user
        }
        binding.bottomMediaBrowser.setOnClickListener {
            if (checkForInternet(this)) {
                // Internet is available, proceed to show ad and navigate to MainActivity
               rewardedIAd()
                rewardedInterstitialAd?.show(this, object : OnUserEarnedRewardListener {
                    override fun onUserEarnedReward(p0: RewardItem) {
                    }

                })
            } else {
                // No internet, directly navigate to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return@setOnClickListener
            }

            // Define an ad listener to handle navigation after ad is dismissed
            rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Ad dismissed, proceed to navigate to MainActivity
                    startActivity(Intent(this@LinkTubeActivity, MainActivity::class.java))
                    finish()
                }
            }
        }


        binding.tabBtn.setOnClickListener {
            val viewTabs =
                layoutInflater.inflate(R.layout.tab_view, binding.root, false)
            val bindingTabs = TabViewBinding.bind(viewTabs)

            val dialogTabs = MaterialAlertDialogBuilder(this , R.style.roundCornerDialog).setView(viewTabs)
                .setTitle("Select Tab")
                .setPositiveButton("Home"){self, _ ->
                    changeTab("Home", HomeFragment())
                    self.dismiss()
                }
                .setNeutralButton("Google"){self, _ ->
                    changeTab("Google", BrowseFragment(urlNew = "www.google.com"))
                    self.dismiss()
                }
                .create()

            bindingTabs.tabsRV.setHasFixedSize(true)
            bindingTabs.tabsRV.layoutManager = LinearLayoutManager(this)
            bindingTabs.tabsRV.adapter = TabAdapter(this, dialogTabs)

            dialogTabs.show()

            val pBtn =   dialogTabs.getButton(AlertDialog.BUTTON_POSITIVE)
            val nBtn =   dialogTabs.getButton(AlertDialog.BUTTON_NEUTRAL)

            pBtn.isAllCaps = false
            nBtn.isAllCaps = false

            pBtn.setCompoundDrawablesWithIntrinsicBounds( ResourcesCompat.getDrawable(resources,
                R.drawable.home_browse, theme)
                , null, null, null)
            nBtn.setCompoundDrawablesWithIntrinsicBounds( ResourcesCompat.getDrawable(resources,
                R.drawable.plus_icon, theme)
                , null, null, null)



        }
    }

    private fun downloadCurrentWebPage(url: String) {
        var frag: BrowseFragment? = null
        try {
            frag = LinkTubeActivity.tabsList[myPager.currentItem].fragment as BrowseFragment
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Extracting the search query from the URL
        val query = extractSearchQuery(url)
        val decodedQuery = URLDecoder.decode(query, "UTF-8")
        val fileName = "${decodedQuery?.replace("+", " ")} - SeeA LinkTube.mhtml"

        val destinationDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(destinationDir, fileName)

        // Save the webpage to the file
        frag?.binding?.webView?.saveWebArchive(file.absolutePath, false) { savedFile ->
            if (savedFile != null) {
                Toast.makeText(this, "Page saved as $fileName", Toast.LENGTH_SHORT).show()
                Log.d("Downloaded URL", url)
            } else {
                Toast.makeText(this, "Failed to save page", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun extractSearchQuery(url: String): String? {
        val queryIndex = url.indexOf("search?q=")
        return if (queryIndex != -1) {
            val startIndex = queryIndex + "search?q=".length
            val endIndex = url.indexOf('&', startIndex)
            if (endIndex != -1) {
                url.substring(startIndex, endIndex)
            } else {
                url.substring(startIndex)
            }
        } else {
            null
        }
    }



    override fun onResume() {
        super.onResume()
        checkHistorySize()
        MobileAds.initialize(this){}
        mAdView = findViewById(R.id.adView)
        rewardedIAd()
        initializeView()
        initializeBinding()
        getAllBookmarks()
        // banner ads
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
        setSwipeRefreshBackgroundColor()

        printJob?.let {
            when{
                it.isCompleted -> Snackbar.make(binding.root, "Successful -> ${it.info.label}", 4000).show()
                it.isFailed -> Snackbar.make(binding.root, "Failed -> ${it.info.label}", 4000).show()
            }
        }
    }


    private fun saveAsPdf(web: WebView){
        val pm = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "${URL(web.url).host}_${SimpleDateFormat("HH:mm d_MMM_yy", Locale.ENGLISH)
            .format(Calendar.getInstance().time)}"
        val printAdapter = web.createPrintDocumentAdapter(jobName)
        val printAttributes = PrintAttributes.Builder()
        printJob = pm.print(jobName , printAdapter, printAttributes.build())

    }

    private fun changeFullscreen(enable: Boolean){
        if(enable){
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, binding.root).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }else{
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun isBookmarked(url: String): Int{
        bookmarkList.forEachIndexed { index, bookmark ->
            if(bookmark.url == url) return index
        }
        return -1
    }

    fun saveBookmarks(){
        //for storing bookmarks data using shared preferences
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE).edit()

        val data = GsonBuilder().create().toJson(bookmarkList)
        editor.putString("bookmarkList", data)

        editor.apply()
    }

    private fun getAllBookmarks(){
        //for getting bookmarks data using shared preferences from storage
        bookmarkList = ArrayList()
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE)
        val data = editor.getString("bookmarkList", null)

        if(data != null){

            val list: ArrayList<Bookmark> = GsonBuilder().create().fromJson(data, object: TypeToken<ArrayList<Bookmark>>(){}.type)
            bookmarkList.addAll(list)
        }
    }
}

@SuppressLint("NotifyDataSetChanged")
fun changeTab(url: String, fragment: Fragment , isBackground : Boolean = false) {
    LinkTubeActivity.tabsList.add(Tab(name = url,fragment = fragment , activity = Activity()))
    myPager.adapter?.notifyDataSetChanged()
    tabsBtn.text = LinkTubeActivity.tabsList.size.toString()
    if(!isBackground) myPager.currentItem = LinkTubeActivity.tabsList.size - 1

}



@SuppressLint("ObsoleteSdkInt")
fun checkForInternet(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    } else {
        @Suppress("DEPRECATION") val networkInfo =
            connectivityManager.activeNetworkInfo ?: return false
        @Suppress("DEPRECATION")
        return networkInfo.isConnected
    }


}

